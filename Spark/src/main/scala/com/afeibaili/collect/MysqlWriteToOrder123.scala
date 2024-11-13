package com.afeibaili.collect

import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{SaveMode, SparkSession}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

object MysqlWriteToOrder123 {
  def main(args: Array[String]): Unit = {
    /*1、抽取ds_db01库中order_master的增量数据进入Hive的ods库中表order_master。
          根据ods.order_master表中modified_time作为增量字段，只将新增的数据抽入，
          字段名称、类型不变，同时添加静态分区，分区字段为etl_date，类型为String，
          且值为当前比赛日的前一天日期（分区字段格式为yyyyMMdd）。
          使用hive cli执行show partitions ods.order_master命令，
          将执行结果截图粘贴至客户端桌面【Release\\模块B提交结果.docx】中对应的任务序号下；*/


    // 搭建好前置环境条件 MySQL准备三张以上表，并包含三个字段，id、name、modified_time，
    // 在里面提前写好数据,并将一部分日期之间的数据添加到hive

    // 创建提前准备的三张表
    val orders = Array("order1_table", "order2_table", "order3_table")
    //创建SparkSql
    val session = SparkSession.builder()
      .appName("WriteBeforeOrder123")
      .master("yarn")
      .config("hive.exec.dynamic.partition", "true")
      .config("hive.exec.dynamic.partition.mode", "nostrict")
      .enableHiveSupport()
      .getOrCreate()

    //创建jdbc信息
    val properties = new Properties()
    properties.put("user", "root")
    properties.put("password", "*********")
    properties.put("driver", "com.mysql.cj.jdbc.Driver")

    val nowDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    //循环每一张表
    orders.foreach(table => {
      //获取hive最新更新时间
      val maxModifiedTime = session
        .sql(s"select max(modified_time) as max_modified_time from spark.$table")
        .collect()(0)
        .getAs[String]("max_modified_time")

      //建立mysql连接
      session.read.jdbc("jdbc:mysql://123.56.82.129/spark", s"$table", properties)
        //过滤出新增的数据
        .filter(s"modified_time > $maxModifiedTime")
        //创建一个新列，用于分区
        .withColumn("etl_date", lit(nowDateTime))
        //写出到hive
        .write
        .format("hive")
        //模式为追加
        .mode(SaveMode.Append)
        //设置分区
        .partitionBy("etl_date")
        //保存
        .saveAsTable(s"spark.$table")
      println(s"=============================$table===============================")
    })
  }
}