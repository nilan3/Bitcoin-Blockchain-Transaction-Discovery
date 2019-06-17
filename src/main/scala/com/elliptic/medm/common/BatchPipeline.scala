package com.elliptic.medm.common

import com.elliptic.medm.common.pipeline.processor.ProcessorBluePrint
import com.elliptic.medm.common.pipeline.source.Source
import com.elliptic.medm.utils.AppConfig
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, DataFrameWriter, Row}

import scala.annotation.tailrec

/**
  *Extended class for batch pipelines only.
  */
abstract class BatchPipeline(config: AppConfig, processor: ProcessorBluePrint)
  extends AbstractPipelines[DataFrameWriter[Row]](config) with Source{
  private val schema: StructType = processor.createSchema()
  private lazy val reader: DataFrame = createCustomReader(spark, config, schema)
  override protected lazy val processedPipelines: Array[DataFrame] = Array(reader)

  override def createWriter(pipelines: Array[DataFrame]): Array[DataFrameWriter[Row]] = createCustomWriter(pipelines)

  def consoleWriter(pipelines: Array[DataFrame]): Unit = {
    pipelines.foreach(df => df.show(20, false))
  }

  @tailrec
  final override def start(): Unit = {
    if (configuration.getBoolean("spark.consoleWriter")) consoleWriter(processor.defineProcessing(reader, spark))
    else createWriter(processor.defineProcessing(reader, spark)).foreach(_.save)
    Thread.sleep(configuration.getInt("collection.intervalMs"))
    start()
  }

}
