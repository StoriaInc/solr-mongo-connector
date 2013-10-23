package com.codebranch.scala.mongodb.solrconnector.util

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.Logging


object Config extends Logging {
  import scala.collection.JavaConversions._

  protected val conf = ConfigFactory.load()

  val maxEntriesForProcess = conf.getInt("connector.max-entries-for-process")
  val configFileUrl = conf.getString("connector.timestamps.file")
  val mongoUri = conf.getStringList("connector.mongo.uri")
  val solrUri = conf.getString("connector.solr.uri")
  val namespace = conf.getStringList("connector.namespace").toList
  val bulkMaxUpsert = conf.getInt("connector.bulk.max_upsert")
  val workerSleep = conf.getLong("connector.worker.sleep")
}