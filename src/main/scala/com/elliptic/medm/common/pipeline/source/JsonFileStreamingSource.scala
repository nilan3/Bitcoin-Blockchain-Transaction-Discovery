package com.elliptic.medm.common.pipeline.source

import com.elliptic.medm.utils.AppConfig
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Json File source connector trait
  */
trait JsonFileStreamingSource {

  protected def createCustomReader(spark: SparkSession, configuration: AppConfig, schema: StructType): DataFrame = {
    val inputStream = spark.readStream
      .format("json")
      .option("maxFilesPerTrigger", configuration.getString("file.maxFilesPerTrigger"))

    inputStream.schema(schema).load(configuration.getString("file.inputPath"))
  }
}