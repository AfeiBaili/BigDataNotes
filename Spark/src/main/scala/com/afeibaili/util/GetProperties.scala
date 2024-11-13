package com.afeibaili.util

import java.util.Properties

object GetProperties {
  val url: String = "jdbc:mysql://123.56.82.129/spark"

  def apply(): Properties = {
    val properties = new Properties()
    properties.put("user", "root")
    properties.put("password", "afeibaili233")
    properties.put("driver", "com.mysql.cj.jdbc.Driver")
    properties
  }
}
