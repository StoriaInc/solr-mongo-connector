package com.codebranch.scala.mongodb.solrconnector


import akka.actor.{ActorLogging, Actor}



/**
 * User: alexey
 * Date: 9/21/13
 * Time: 9:52 PM
 */
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
