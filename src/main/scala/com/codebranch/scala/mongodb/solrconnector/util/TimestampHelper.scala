/**
 * Copyright 2013 SelfishInc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

