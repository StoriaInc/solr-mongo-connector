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
package me.selfish.solr.mongo

import akka.actor._
import org.apache.solr.common.SolrInputDocument
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import org.bson.types.BSONTimestamp
import scala.io.Source
import scala.reflect.io.File
import akka.actor.SupervisorStrategy.Resume
import com.mongodb.BasicDBObject
import me.selfish.solr.mongo.util._
import scala.Some
import akka.actor.OneForOneStrategy


/**
 * User: alexey
 * Date: 9/21/13
 * Time: 12:24 PM
 */
class SolrImporter extends Actor with ActorLogging {
  import SolrImporter._
  import TimestampHelper._


  override def preStart() {
    super.preStart()
    val ts = readTimestamp
      val w = context.actorOf(Props(new MongoOplogReader()), "OplogReader")
      w ! StartProcessing(ts)

  }


  def receive: Receive = {
    case UpdateTimestamp(ts) => {
      log.debug(s"updating timestamp")
      saveTimestamp(ts)
    }
  }


  //TODO: do not save ts in file, get it from solr
  def saveTimestamp(ts: BSONTimestamp) {
    try {
      File(Config.configFileUrl).writeAll(BSONToLong(ts).toString)
    } catch {
      case e: Exception => {
        log.error(s"could not save timestamps to ${Config.configFileUrl}")
      }
    }
  }


  def readTimestamp: Option[BSONTimestamp] = {
    try {
      val lines = Source.fromFile(Config.configFileUrl).getLines().toList
      Some(toBSONTimestamp(lines(0).toLong))
    } catch {
      case e: Exception => {
        log.error(s"could not read timestamps from ${Config.configFileUrl}")
        None
      }
    }
  }



  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 50, withinTimeRange = 1 minute)
    {
      case e: Exception ⇒ {
        log.error(e, "Worker throw an exception")
        Resume
      }
    }
}


class MongoOplogReader extends Actor with ActorLogging with TimeLogging {

  import MongoConversions._
  import SolrImporter._
  import SolrHelper._
  import MongoHelper._
  import TimestampHelper._
  import context.dispatcher

  val solrWorker =  context.actorOf(
    Props(new SolrWorker()).withMailbox("solr-worker-mailbox"), "solrWorker")
  val namespaces = Config.namespace.toSet


  override def preStart(){
    super.preStart()
  }


  def receive: Receive = {
    case StartProcessing(ts) => ts match {
      case ts @ Some(_) => {
        context.system.scheduler.scheduleOnce(Duration.Zero, self, Process)
        log.info("Start reading oplog")
        loggingTime("creating oplog"){
          val oplog = new MongoOpLog(MongoHelper.mongoClient, ts)
          context become running(oplog)
        }
      }
      case None => {
        //TODO: this will fail on empty oplog
        val lastOpsTimestamp = new MongoOpLog(MongoHelper.mongoClient).getLastTimestamp.get
        context become dumpingDatabase(namespaces.toList, lastOpsTimestamp)
      }
    }
  }


  def running(oplog: MongoOpLog): Receive = {
    case Process => {

//      when we have large oplog, this could take long time, and solr importer will be overloaded
      val ops = oplog.take(Config.maxEntriesForProcess).toSeq

      log.debug(s"got ${ops.length} operations from oplog")

      val batches = ops.filter {
        case o: MongoNopOperation => false
        case o: MongoCommandOperation => false
        case o if namespaces(o.namespace) => true
        case o => false
      }.sliding(Config.bulkMaxUpsert, Config.bulkMaxUpsert)
          .map(_.partition {
        case o: MongoDeleteOperation => false
        case _ => true
      }).map{ case (add, del) => {
        SolrBatch(add.map(getSolrInputDocument).flatten, del.map(_.document.get("_id").toString))
      }}

      batches foreach(b => solrWorker ! b)

      ops.lastOption map(_.timestamp) foreach (ts => {
        context.parent ! UpdateTimestamp(ts)
      })

      context.system.scheduler.scheduleOnce(Config.workerSleep milliseconds, self, Process)
    }
    //just ignore this
    case BatchImported =>
  }


  def dumpingDatabase(namespaces: List[String], lastTimestamp: BSONTimestamp): Receive = {
    namespaces match {
      case Nil => {
        // we dumped all collections, send updates about timestamp and start reading oplog
        context.parent ! UpdateTimestamp(lastTimestamp)
        self ! StartProcessing(Some(lastTimestamp))
        receive
      }
      case ns :: t => {
        //we have smth to dump
        val (db, collection) = getDbCollectionName(ns)
        val cursor = mongoClient.getDB(db).getCollection(collection).find()

        //send batch to SolrWorker
        def sendBatch() {
          val addList = ListBuffer[SolrInputDocument]()
          while (cursor.hasNext && addList.length < Config.bulkMaxUpsert) {
            try {
              val doc = cursor.next.asInstanceOf[BasicDBObject]
              addList.append(getSolrInputDocument(convert(doc), ns, lastTimestamp))
              if(addList.length == Config.bulkMaxUpsert || !cursor.hasNext){
                solrWorker ! SolrBatch(addList.clone(), Nil)
              }
            } catch {
              case e: Exception => log.error(e, s"Error while dumping $ns")
            }
          }
        }

        //initiate start dumping
        self ! BatchImported


        {
          case BatchImported => {
            if(cursor.hasNext) sendBatch()
            else context become dumpingDatabase(t, lastTimestamp)
          }
        }
      }
    }
  }
}


class SolrWorker extends Actor with ActorLogging with Stash with TimeLogging {
  import context.dispatcher
  import SolrHelper._
  import SolrImporter._

  def receive: Receive = {
    case SolrBatch(add, del) => {
      context become exportingToSolr(() => exportDocs(add, del))
    }
  }


  def exportingToSolr(action: () => Boolean): Receive = {
    context.system.scheduler.scheduleOnce(0 seconds, self, Process)

    {
      case Process => {
        log.debug(s"trying import docs to solr")
        if(action()) {
          log.debug(s"docs imported to solr ")
          context.parent ! BatchImported
          unstashAll()
          context become receive
        } else {
          log.warning(s"fail to import docs to solr, try once more")
          context.system.scheduler.scheduleOnce(Config.workerSleep milliseconds, self, Process)
        }
      }
      case a: SolrAction => stash()
    }
  }


  def dropCollection(query: String): Boolean = {
    try {
      dropCollectionFromSolr(query) == 0
    } catch {
      case e: Exception => {
        log.error(e, s"Error during dropping collection from solr, query:\n$query")
        false
      }
    }
  }


  //true if processed, false on any error
  def exportDocs(
      addList: Iterable[SolrInputDocument],
      delList: Iterable[String]
      ): Boolean =
    try {
      log.debug(s"export docs: addList size ${addList.size}, delList size ${delList.size}")

      val addResponseStatus = addDocsToSolr(addList, commit = delList.isEmpty)
      val deleteResponseStatus = deleteDocsFromSolr(delList.toList, commit = true)

      val success = addResponseStatus == 0 && deleteResponseStatus == 0

      if(!success)
        log.warning(s"solr response status: add $addResponseStatus, delete: $deleteResponseStatus")
      success

    } catch {
      case e: Exception => {
        log.error(e, s"Error during exporting docs to solr")
        false
      }
    }
}



object SolrImporter {
  sealed trait SolrAction
  case class SolrBatch(add: Iterable[SolrInputDocument], del: Iterable[String])
      extends SolrAction


  case object Process
  case class StartProcessing(lastTimestamp: Option[BSONTimestamp])
  case class UpdateTimestamp(timestamp: BSONTimestamp)
  case object BatchImported
}
