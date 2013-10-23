package com.codebranch.scala.mongodb.solrconnector.util


import scala.collection.JavaConversions._
import com.mongodb._
import com.codebranch.scala.mongodb.solrconnector.MongoConversions
import MongoConversions.convert
import scala.collection.mutable.ListBuffer
import org.apache.solr.common.SolrInputDocument
import com.typesafe.scalalogging.slf4j.Logging



object MongoHelper extends Logging {
	val IdKey = "_id"
	val DropDatabaseCommand = "dropDatabase"
	val DropCollectionCommand = "drop"


  val mongoClient = {
    //docs says that primary is default, but...
    val options = MongoClientOptions.builder().readPreference(ReadPreference.primary()).build()

    val servers = Config.mongoUri.map(addr => {
      val (host, port) = getHostPort(addr)
      new ServerAddress(host, port)
    })
    new MongoClient(servers.toList, options)
  }


  def getHostPort(addr: String): (String, Int) = {
    val hp = addr.split(':')
    hp.length match {
      case 2 => (hp(0), hp(1).toInt)
      case _ => throw new Exception(
        s"Incorrect server address: $addr. Correct format is host:port")
    }
  }


  def getDbCollectionName(Namespace: String): (String, String) = {
    val names = Namespace.split('.')
    names.length match {
      case 2 => (names(0), names(1))
      case _ => throw new Exception(
        s"Incorrect namespace: $Namespace. Correct format is dbName.collectionName")
    }
  }
}