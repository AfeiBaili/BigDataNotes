package com.afeibaili.write

import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{SaveMode, SparkSession}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

object WriteBeforeOrder {

  def main(args: Array[String]): Unit = {
    val jdbc = new Properties()
    jdbc.put("user", "root")
    jdbc.put("password", "afeibaili233")

    val date = LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    //去mysql拿到数据
    spark.read.jdbc("jdbc:mysql://123.56.82.129/spark", "order_table", jdbc)
      //过滤出11-5号之前的数据
      .filter(s"modified_time < '$date'")
      //添加列
      .withColumn("etl_date", lit(date))
      .write
      //写入到hive
      .format("hive")
      .mode(SaveMode.Overwrite)
      //设置分区
      .partitionBy("etl_date")
      .saveAsTable("spark.order_table")

    spark.stop()
  }

  def spark: SparkSession = SparkSession
    .builder()
    .config("hive.exec.dynamic.partition", "true")
    .config("hive.exec.dynamic.partition.mode", "nonstrict")
    .enableHiveSupport()
    .appName("WriteBeforeOrder")
    .master("yarn")
    .getOrCreate()
}
