package com.elliptic.medm.application.transactioncollection

import com.elliptic.medm.common.BatchPipeline
import com.elliptic.medm.common.pipeline.processor.ProcessorBluePrint
import com.elliptic.medm.common.pipeline.sink.KafkaBatchSink
import com.elliptic.medm.utils.{AppConfig, Utilities}
import org.apache.spark.sql.functions.{col, udf, lit}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import scalaj.http.{Http, HttpOptions}

import scala.annotation.tailrec

/**
  * Connector to fetch list of address from config and transform into a dataframe
  * This will allow the list to be distributed across workers and the api can be queried in
  * concurrently by workers
  */
trait ShapeshiftApiSource {
  protected def createCustomReader(spark: SparkSession, configuration: AppConfig, schema: StructType): DataFrame = {
    val addresses: List[String] = configuration.getList("collection.addresses")
    spark.createDataFrame(
      spark.sparkContext.parallelize(addresses.map(x => Row(x))),
      schema
    )
  }
}

/**
  * Custom Pipeline class to read from shapeshift api and write to kafka
  */
class ShapeshiftToKafkaPipeline(configuration: AppConfig, processor: ProcessorBluePrint)
  extends BatchPipeline(configuration, processor)
    with ShapeshiftApiSource
    with KafkaBatchSink {
}

/**
  * Main processor for taking list of addresses and fetching transactions from shapeshift at
  * configured intervals
  */
class TransactionCollectionApplication(configuration: AppConfig)
  extends ProcessorBluePrint with java.io.Serializable {

  /**
    * Schema used to convert list of address into dataframe
    */
  override def createSchema(): StructType = StructType(List(
    StructField("address", StringType, nullable = false)
  ))

  /**
    * Recursive method to query transaction from api
    * Used in spark udf
    */
  def queryApi(url: String, count: Int = 0): String = {
    if (count == configuration.getInt("collection.retries")) {
      s"""{"error": "unable to obtain response from $url"}"""
    } else {
      try {
        val resp = Http(url)
          .header("Content-Type", "application/json")
          .header("Charset", "UTF-8")
          .option(HttpOptions.readTimeout(configuration.getInt("collection.timeoutMs"))).asString
        if (resp.code == 200) {
          resp.body.replaceAll("\\s", "")
        } else {
          queryApi(url, count + 1)
        }
      } catch {
        case _: Throwable => queryApi(url, count + 1)
      }
    }
  }

  /**
    * Custom Pipeline class to read from shapeshift api and write to kafka
    */
  override def defineProcessing(data: DataFrame, spark: SparkSession): Array[DataFrame] = {
    val getTransaction = udf((address: String) => {
      val url = configuration.getString("collection.api") + address
      queryApi(url)
    })
    val res = data
      .withColumn("value", getTransaction(col("address")))
      .withColumn("topic", lit(configuration.getString("kafka.topicOutput")))
      .select(col("address").as("key"), col("value"), col("topic"))
    Array(res)
  }
}

object TransactionCollectionApplication {

  def createProcessor(configuration: AppConfig): ProcessorBluePrint = new TransactionCollectionApplication(configuration)

  def main(args: Array[String]): Unit = {
    val configuration = Utilities.loadConfig(args)
    val pipeline: Unit = new ShapeshiftToKafkaPipeline(configuration, createProcessor(configuration)).start()

  }

}
