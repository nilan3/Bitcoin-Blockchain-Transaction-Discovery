package com.elliptic.medm.common.pipeline.sink

import org.apache.spark.sql.streaming.DataStreamWriter
import org.apache.spark.sql.{DataFrame, Row}

/**
  * Kafka streaming sink connector trait
  */
trait KafkaStreamingSink extends Sink[DataStreamWriter[Row]] {
  override def createCustomWriter(pipelines: Array[DataFrame]): Array[DataStreamWriter[Row]] = {
    val streams = pipelines.zipWithIndex.map { case (pipeline, index) =>
      pipeline.writeStream
        .format("kafka")
        .outputMode(configuration.getString("spark.outputMode"))
        .option("kafka.bootstrap.servers", configuration.getString("kafka.bootstrapServers"))
//        .option("kafka.security.protocol", configuration.getString("kafka.securityProtocol"))
        .option("checkpointLocation", configuration.getString("spark.checkpointLocation") + "/" + index.toString)
    }
    streams
  }
}
