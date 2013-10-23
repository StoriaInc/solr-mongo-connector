package com.codebranch.scala.mongodb.solrconnector

import com.typesafe.scalalogging.slf4j.Logging
import akka.actor.{Props, ActorSystem}
import com.codebranch.scala.mongodb.solrconnector.util.Config



object ScalaMongoConnector extends Logging {

  def main(args: Array[String]) {

    if (Config.namespace.isEmpty) {
      logger.warn("Misconfiguration: Namespace should be list of strings. Check your configuration file")
      System.exit(2)
    }

	  val system = ActorSystem("Scala-mongo-connector")
	  system.actorOf(Props[SolrImporter], "SolrImporter")
  }
}