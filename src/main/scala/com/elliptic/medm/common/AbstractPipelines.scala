package com.elliptic.medm.common

import com.elliptic.medm.utils.AppConfig
import com.elliptic.medm.common.pipeline.sink.Sink
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Base class for a batch or streaming pipeline
  */
abstract class AbstractPipelines[T](config: AppConfig)
  extends Sink[T] with SparkSessionHelper {

  protected val configuration: AppConfig = config
  protected val spark: SparkSession = createSpark(configuration)
  spark.sparkContext.setLogLevel("WARN")
  protected val processedPipelines: Array[DataFrame]
  protected lazy val writer: Array[T] = createWriter(processedPipelines)

  def createWriter(pipelines: Array[DataFrame]): Array[T]

  def start(): Unit

}
