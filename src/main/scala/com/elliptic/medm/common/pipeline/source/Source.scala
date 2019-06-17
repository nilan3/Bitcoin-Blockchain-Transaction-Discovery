package com.elliptic.medm.common.pipeline.source

import com.elliptic.medm.utils.AppConfig
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Base trait for source connector
  */
trait Source {
  protected val configuration: AppConfig

  protected def createCustomReader(spark: SparkSession, configuration: AppConfig, schema: StructType): DataFrame
}
