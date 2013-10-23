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
package me.selfish.solr.mongo.util


import scala.collection.JavaConversions._
import com.mongodb._
import com.typesafe.scalalogging.slf4j.Logging


object MongoHelper extends Logging {
  val IdKey = "_id"
  val DropDatabaseCommand = "dropDatabase"
  val DropCollectionCommand = "drop"


  lazy val mongoClient = {
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