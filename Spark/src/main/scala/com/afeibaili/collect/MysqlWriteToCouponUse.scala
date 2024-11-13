package com.afeibaili.collect

import com.afeibaili.util.GetProperties
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{SaveMode, SparkSession}

object MysqlWriteToCouponUse {
  def main(args: Array[String]): Unit = {
    //获取SparkSql
    val session = SparkSession.builder()
      .appName("MysqlWriteToCouponUse")
      .master("yarn")
      .config("hive.exec.dynamic.partition.mode", "nostrict")
      .config("hive.exec.dynamic.partition", "true")
      .enableHiveSupport()
      .getOrCreate()

    //使用greatest获取出最大的值
    val maxTime = session
      .sql("select greatest(max(get_time),max(used_time),max(pay_time)) as max_time from spark.coupon_use")
      .collect()(0).getAs[String]("max_time")
    //打印测试
    println(s"==========================$maxTime=====================================")

    session.read.jdbc(GetProperties.url, "coupon_use", GetProperties())
      .filter(s"get_time between '$maxTime' and '20241118' and used_time between '$maxTime' and '20241118' and pay_time between '$maxTime' and '20241118'")
      .show()

    //连接mysql，这里进行了封装
    session.read.jdbc(GetProperties.url, "coupon_use", GetProperties())
      //过滤出指定区间的值
      .filter(s"get_time between '$maxTime' and '20241118' and used_time between '$maxTime' and '20241118' and pay_time between '$maxTime' and '20241118'")
      .withColumn("etl_date", lit("20241118"))
      //写出并指定分区
      .write
      .format("hive")
      .mode(SaveMode.Append)
      .partitionBy("etl_date")
      .saveAsTable("spark.coupon_use")

  }
}
