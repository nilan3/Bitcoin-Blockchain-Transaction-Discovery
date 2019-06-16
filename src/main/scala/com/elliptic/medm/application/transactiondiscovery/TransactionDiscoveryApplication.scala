package com.elliptic.medm.application.transactiondiscovery

import com.elliptic.medm.common.pipeline.{StreamingFileToKafkaPipeline, StreamingKafkaToKafkaPipeline}
import com.elliptic.medm.utils.AppConfig
import com.elliptic.medm.common.pipeline.processor.ProcessorBluePrint
import com.elliptic.medm.utils.Utilities
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout, OutputMode}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

case class InputRow(status: String, address: String, withdraw: String, incomingCoin: Double, incomingType: String, outgoingCoin: String, outgoingType: String, transaction: String, transactionURL: String)
case class AddressState(latestTransaction: List[String])

class TransactionDiscoveryApplication(configuration: AppConfig)
  extends ProcessorBluePrint with java.io.Serializable {

  /**
    * Schema for reading input messages from kafka.
    * Ensures no invalid messages are read.
    */
  override def createSchema(): StructType = StructType(List(
    StructField("status", StringType, nullable = false),
    StructField("address", StringType, nullable = false),
    StructField("error", StringType, nullable = true),
    StructField("withdraw", StringType, nullable = true),
    StructField("incomingCoin", DoubleType, nullable = true),
    StructField("incomingType", StringType, nullable = true),
    StructField("outgoingCoin", StringType, nullable = true),
    StructField("outgoingType", StringType, nullable = true),
    StructField("transaction", StringType, nullable = true),
    StructField("transactionURL", StringType, nullable = true)
  ))

  /**
    * Main Processor for handling filtering and stateful operations to extract new transaction for each address
    */
  override def defineProcessing(data: DataFrame, spark: SparkSession): Array[DataFrame] = {
    val topic = configuration.getString("kafka.topicOutput")
    val filtered = data
      .where(col("status") === "complete")
      .select("status", "address", "withdraw", "incomingCoin", "incomingType", "outgoingCoin",
        "outgoingType", "transaction", "transactionURL")
    val sink = prepareKafkaSink(statefulStream(filtered), topic)

    Array(sink)
  }

  /**
    * Stateful operator
    * Group and lookup last state for each address before comparing and updating if new transaction is seen
    * Message is only output when new traction id is observed for a particular address.
    */
  def statefulStream(data: DataFrame): DataFrame = {
    import data.sparkSession.implicits._
    data
      .as[InputRow]
      .groupByKey(_.address)
      .flatMapGroupsWithState[AddressState, InputRow](
      outputMode = OutputMode.Append(),
      timeoutConf = GroupStateTimeout.ProcessingTimeTimeout())(updateState)
      .select("status", "address", "withdraw", "incomingCoin", "incomingType", "outgoingCoin",
        "outgoingType", "transaction", "transactionURL")
  }

  /**
    * In cases where the application had stopped for a period of time and is starting to catchup on a large range of
    * kafka offsets, duplicates will be seen if no new transactions are made for each address.
    * By grouping by transaction id and taking the first element, duplicates are removed.
    */
  def removeDuplicates(incomingTransactions: List[InputRow], lastState: AddressState): List[InputRow] = {
    incomingTransactions
      .groupBy(_.transaction).map(_._2.head)
      .filter(x => !lastState.latestTransaction.contains(x.transaction))
      .toList
  }

  /**
    * Method to updating state for each address. The latest observed group of transaction id are recorded and used to
    * compare against nextt batch of api call responses. If the transaction id mataches the state, the messages are
    * ignored. Otherwise, address state is updated with latest transaction id and new message is output to sink.
    */
  def updateState(address: String, transactionsIter: Iterator[InputRow], oldState: GroupState[AddressState]): Iterator[InputRow] = {
    val transactions = transactionsIter.toList
    if (oldState.hasTimedOut) oldState.remove
    val sink = if (transactions.nonEmpty) {
      val existingState = if (oldState.exists & !oldState.hasTimedOut) {
        oldState.get
      } else {
        AddressState(List())
      }
      val filtered = removeDuplicates(transactions, existingState)
      if (filtered.nonEmpty) {
        val newState = AddressState(filtered.map(_.transaction))
        oldState.update(newState)
        oldState.setTimeoutDuration(configuration.getString("state.timeout"))
      }
      filtered
    } else {
      List()
    }
    sink.toIterator
  }

  /**
    * convert dataframe fields to JSON for output into kafka topic
    */
  def prepareKafkaSink(output: DataFrame, topic: String): DataFrame = {
    output
      .withColumn("topic", lit(topic))
      .select(to_json(struct("status", "address", "withdraw", "incomingCoin", "incomingType", "outgoingCoin",
        "outgoingType", "transaction", "transactionURL")).as("value"), col("topic"))
  }

}

object TransactionDiscoveryApplication {

  def createProcessor(configuration: AppConfig): ProcessorBluePrint = new TransactionDiscoveryApplication(configuration)

  def main(args: Array[String]): Unit = {
    val configuration = Utilities.loadConfig(args)
//    val pipeline: Unit = new StreamingFileToKafkaPipeline(configuration, createProcessor(configuration)).start()
    val pipeline: Unit = new StreamingKafkaToKafkaPipeline(configuration, createProcessor(configuration)).start()

  }

}
