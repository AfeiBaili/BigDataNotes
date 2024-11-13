package com.afeibaili.write

import com.afeibaili.NameGenerator
import com.afeibaili.pojo.Order
import com.afeibaili.util.{GetDatetime, GetProperties}
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{SaveMode, SparkSession}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object WriteBeforeOrder123Source {
  /**
   * 创建数据到MySql，并将部分数据转移到Hive
   */
  def main(args: Array[String]): Unit = {
    //创建SparkSql对象
    val session = SparkSession.builder()
      //设置应用名称
      .appName("writeBeforeOrder1Order2Order3")
      //配置hive分区信息
      .config("hive.exec.dynamic.partition", "true")
      .config("hive.exec.dynamic.partition.mode", "nostrict")
      //开启hive支持
      .enableHiveSupport()
      .getOrCreate()

    //创建jdbc信息
    val properties = GetProperties()

    //指定创建的三张表
    val tuples = Array[(String, Boolean)](("order1_table", true), ("order2_table", false), ("order3_table", true))
    //循环
    tuples.foreach(tuples => {
      //创建可变数组
      val orders = ArrayBuffer[Order]()
      //创建随机数 用来随机日期，匹配比赛环境
      val random = new Random()

      //循环创建五百天数据
      for (i <- 1 to 500) {
        //生成modified_time时间，数据区间在11/11最多加50天最少在减去30天
        val datetime = GetDatetime()
        //添加到先前创建的可变数组，（NameGenerator.getName上一个动态发的项目用于生成一些名字）
        orders += Order(i, if (tuples._2) NameGenerator.getName else NameGenerator.getChineseName, datetime)
      }
      //隐式转换，将可变数组转换成DataFrame，用于传入SparkSql
      import session.implicits._
      //创建虚拟表
      orders.toDF().createOrReplaceTempView(tuples._1)
      //使用SparkSql查询传入的二元组
      session.sql(s"select * from ${tuples._1}")
        //写出
        .write
        //模式是覆写
        .mode(SaveMode.Overwrite)
        //连接MySql
        .jdbc("jdbc:mysql://123.56.82.129/spark", s"${tuples._1}", properties)
      //如果一张表打印好了会输出以下分割线
      println(s"===================================生成数据${tuples._1}表成功==========================================")
    })

    //写入一部分数据到hive,生成数据将根据modified_time小于11/10号的数据写入到hive
    //循环遍历每一张表
    tuples.foreach(tuples => {
      //从mysql读取数据
      session
        .read
        .jdbc("jdbc:mysql://123.56.82.129/spark", s"${tuples._1}", properties)
        //过滤出小于20241110之前的数据
        .filter("modified_time < '20241110'")
        //添加新字段值为20241110
        .withColumn("etl_date", lit("20241110"))
        //写出
        .write
        //写出到hive
        .format("hive")
        //设置分区字段
        .partitionBy("etl_date")
        //模式为覆写模式
        .mode(SaveMode.Overwrite)
        //保存
        .saveAsTable(s"spark.${tuples._1}")
    })
  }
}