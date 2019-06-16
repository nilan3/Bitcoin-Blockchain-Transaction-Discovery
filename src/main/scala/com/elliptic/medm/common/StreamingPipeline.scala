package com.elliptic.medm.common

import com.elliptic.medm.common.pipeline.processor.ProcessorBluePrint
import com.elliptic.medm.common.pipeline.source.Source
import com.elliptic.medm.utils.AppConfig
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.streaming.DataStreamWriter
import org.apache.spark.sql.types.StructType

abstract class StreamingPipeline(config: AppConfig, processor: ProcessorBluePrint)
  extends AbstractPipelines[DataStreamWriter[Row]](config) with Source {
  private val schema: StructType = processor.createSchema()
  private lazy val reader: DataFrame = createCustomReader(spark, config, schema)
  override protected lazy val processedPipelines: Array[DataFrame] = processor.defineProcessing(reader, spark)

  override def createWriter(pipelines: Array[DataFrame]): Array[DataStreamWriter[Row]] = createCustomWriter(pipelines)

  def consoleWriter(pipelines: Array[DataFrame]): Array[DataStreamWriter[Row]] = {
    pipelines.zipWithIndex.map { case (pipeline, index) =>
      pipeline.writeStream
        .format("console")
        .outputMode(configuration.getString("spark.outputMode"))
        .option("truncate", value = false)
        .option("numRows", 20)
    }
  }

  override def start(): Unit = {
    if (configuration.getBoolean("spark.consoleWriter")) consoleWriter(processedPipelines).foreach(_.start())
    else writer.foreach(_.start())
    spark.streams.awaitAnyTermination()
  }

}
