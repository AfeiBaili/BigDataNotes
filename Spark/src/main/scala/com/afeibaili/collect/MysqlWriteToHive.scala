package com.afeibaili

import com.afeibaili.util.GetProperties
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{SaveMode, SparkSession}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MysqlWriteToHive {
  def main(args: Array[String]): Unit = {
    val url = "jdbc:mysql://123.56.82.129/spark"
    val properties = GetProperties()
    val session = SparkSession
      .builder()
      .config("hive.exec.dynamic.partition", "true")
      .config("hive.exec.dynamic.partition.mode", "nostrict")
      .appName("mysqlWriteToHive")
      .enableHiveSupport()
      .getOrCreate()

    val maxModifiedTime = session.sql("select max(modified_time) maxModifiedTime from spark.order_table").collect()(0).getAs[String]("maxModifiedTime")

    println(maxModifiedTime)

    val date = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    session.read.jdbc(url, "order_table", properties)
      .filter(s"modified_time > '$maxModifiedTime'")
      .show()
    session.read.jdbc(url, "order_table", properties)
      .filter(s"modified_time > '$maxModifiedTime'")
      .withColumn("etl_date", lit(date))
      .write
      .format("hive")
      .partitionBy("etl_date")
      .mode(SaveMode.Append)
      .saveAsTable("spark.order_table")

    session.stop()
  }
}
