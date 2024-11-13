package com.afeibaili.collect.write

import com.afeibaili.NameGenerator
import com.afeibaili.pojo.CouponUse
import com.afeibaili.util.{GetDatetime, GetProperties}
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{SaveMode, SparkSession}

import scala.collection.mutable.ArrayBuffer

object WriteBeforeCouponUse {
  def main(args: Array[String]): Unit = {
    val session = SparkSession.builder()
      .appName("writeCouponUse")
      .master("yarn")
      .config("hive.exec.dynamic.partition", "true")
      .config("hive.exec.dynamic.partition.mode", "nostrict")
      .enableHiveSupport()
      .getOrCreate()

    val couponUses = ArrayBuffer[CouponUse]()

    for (i <- 1 to 500) {
      couponUses += CouponUse(i, NameGenerator.getChineseName, GetDatetime(), GetDatetime(), GetDatetime())
    }

    import session.implicits._
    couponUses.toDF().createOrReplaceTempView("coupon_use_temp")

    session.sql("select * from coupon_use_temp")
      .write
      .mode(SaveMode.Overwrite)
      .jdbc(GetProperties.url, "coupon_use", GetProperties())

    println("===========================================写入Mysql成功=======================================")

    session.read
      .jdbc(GetProperties.url, "coupon_use", GetProperties())
      .filter("get_time < '20241110' and used_time < '20241110' and pay_time < '20241110'")
      .show
    session.read
      .jdbc(GetProperties.url, "coupon_use", GetProperties())
      .filter("get_time < '20241110' and used_time < '20241110' and pay_time < '20241110'")
      .withColumn("etl_date", lit("20241110"))
      .write
      .format("hive")
      .partitionBy("etl_date")
      .mode(SaveMode.Overwrite)
      .saveAsTable("spark.coupon_use")
  }
}
