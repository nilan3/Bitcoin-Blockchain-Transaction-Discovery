package com.elliptic.medm.common

import com.elliptic.medm.utils.AppConfig
import org.apache.spark.sql.SparkSession


/**
  * Spark session helper trait
  */
trait SparkSessionHelper {

  protected def createSpark(name: String, master: String): SparkSession = SparkSession
    .builder
    .appName(name)
    .master(master)
    .getOrCreate()

  protected def createSpark(): SparkSession = SparkSession
    .builder
    .getOrCreate()

  protected def createSpark(configuration: AppConfig): SparkSession = {
    val spark = SparkSession
      .builder
      .appName(configuration.getString("spark.appName"))
      .master(configuration.getString("spark.master"))
      .config("spark.sql.session.timeZone", "UTC")
      .config("spark.sql.shuffle.partitions", configuration.getInt("spark.shufflePartitions"))

    spark.getOrCreate()
  }

}

