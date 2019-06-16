package com.elliptic.medm.common.pipeline.sink

import org.apache.spark.sql.{DataFrame, DataFrameWriter, Row}

trait KafkaBatchSink extends Sink[DataFrameWriter[Row]] {

  override def createCustomWriter(pipelines: Array[DataFrame]): Array[DataFrameWriter[Row]] = {
    val dataWriters = pipelines.map(
      _.write
        .format("kafka")
        .option("kafka.bootstrap.servers", configuration.getString("kafka.bootstrapServers"))
    )
    dataWriters
  }
}