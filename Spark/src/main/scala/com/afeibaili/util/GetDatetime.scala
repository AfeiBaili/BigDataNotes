package com.afeibaili.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Random

object GetDatetime {
  val random = new Random()

  def apply(plusDays: Int = 50, minusDays: Int = 30): String = {
    LocalDateTime.now()
      .plusDays(random.nextInt(plusDays))
      .minusDays(random.nextInt(minusDays))
      .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
  }
}
