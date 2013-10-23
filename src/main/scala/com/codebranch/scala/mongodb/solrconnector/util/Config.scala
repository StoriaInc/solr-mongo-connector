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