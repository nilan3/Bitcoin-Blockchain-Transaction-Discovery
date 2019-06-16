package com.elliptic.medm.common.pipeline.source

import com.elliptic.medm.utils.AppConfig
import org.apache.spark.sql.types.{StringType, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, from_json}

trait KafkaStreamingSource {

  protected def createCustomReader(spark: SparkSession, configuration: AppConfig, schema: StructType): DataFrame = {
    val inputStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", configuration.getString("kafka.bootstrapServers"))
      .option("subscribe", configuration.getString("kafka.topicInput"))
      .option("maxOffsetsPerTrigger", configuration.getString("kafka.maxOffsetsPerTrigger"))
      .option("startingOffsets", configuration.getString("kafka.startingOffsets"))
      .load()

    inputStream.select(
      col("key").cast(StringType),
      from_json(col("value").cast(StringType), schema).alias("json")
    ).selectExpr("key", "json.*")
  }
}