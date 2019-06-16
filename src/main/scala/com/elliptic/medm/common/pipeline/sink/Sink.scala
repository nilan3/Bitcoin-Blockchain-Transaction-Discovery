package com.elliptic.medm.common.pipeline.sink

import com.elliptic.medm.utils.AppConfig
import org.apache.spark.sql.{DataFrame, SparkSession}

trait Sink[T] {
  protected val configuration: AppConfig
  protected val spark: SparkSession

  protected def createCustomWriter(pipelines: Array[DataFrame]): Array[T]

}
