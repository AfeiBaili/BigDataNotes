package com.afeibaili

import com.afeibaili.pojo.Order
import com.afeibaili.util.Util
import org.apache.spark.sql.{SaveMode, SparkSession}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object Main {
  //创建Seq集合
  private val orders = ArrayBuffer[Order]()
  //创建一百个对象
  for (i <- 1 to 100) {
    val order = Order(i, Util.getName,
      LocalDateTime
        .now()
        .plusDays(5)
        .minusDays(new Random().nextInt(10))
        .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    )
    orders += order
  }

  def main(args: Array[String]): Unit = {
    //创建spark
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("OrderApp")
      .enableHiveSupport()
      .getOrCreate()

    //隐式转换
    import spark.implicits._

    //被转换的集合
    orders.toDF().createOrReplaceTempView("order")
    //连接并写入
    val properties = new Properties()
    properties.put("user", "root")
    properties.put("password", "afeibaili233")
    spark.sql("select * from order").write
      .mode(SaveMode.Append)
      .jdbc("jdbc:mysql://afeisserver/spark", "order_table", properties)
  }
}