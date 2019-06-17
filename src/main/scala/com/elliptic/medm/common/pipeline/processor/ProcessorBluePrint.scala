package com.elliptic.medm.common.pipeline.processor

import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Base class for main processor for any pipeline
  * transforms the input from source and prepares data for sink
  */
abstract class ProcessorBluePrint {

  def createSchema(): StructType

  def defineProcessing(data: DataFrame, spark: SparkSession): Array[DataFrame]

}
