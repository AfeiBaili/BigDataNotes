package com.afeibaili.util

import scala.io.Source
import scala.util.Random

object Util {

  def getName: String = {
    val text = Source.fromFile("D:\\Java\\BigDataNotes\\Spark\\src\\main\\resources\\names.txt")
    val random = new Random()

    try {
      val array = text.getLines().toArray
      array(random.nextInt(array.length))
    } finally {
      text.close()
    }
  }

  def main(args: Array[String]): Unit = {
    println(getName)
  }
}
