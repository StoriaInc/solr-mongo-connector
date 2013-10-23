package com.codebranch.scala.mongodb.solrconnector.util

import org.bson.types.BSONTimestamp
import scala.language.implicitConversions


object TimestampHelper {

  implicit def BSONToLong(ts: BSONTimestamp): Long = {
    (ts.getTime.toLong << 32) + ts.getInc
  }


  implicit def toBSONTimestamp(ts: Long): BSONTimestamp = {
    val seconds = ts >> 32
    val increment = ts & 0xffffffff

    new BSONTimestamp(seconds.toInt, increment.toInt)
  }
}

