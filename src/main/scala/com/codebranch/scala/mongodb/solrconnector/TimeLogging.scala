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
package com.codebranch.scala.mongodb.solrconnector


import akka.actor.{ActorLogging, Actor}

trait TimeLogging { this : Actor with ActorLogging =>
  def loggingTime[T](action: String)(f: => T):T = {
    log.info(s"$action started")
    val start = System.currentTimeMillis()
    val res = f
    val end = System.currentTimeMillis()
    log.info(s"$action finished. Time = ${end - start} milliseconds")
	  res
  }
}
