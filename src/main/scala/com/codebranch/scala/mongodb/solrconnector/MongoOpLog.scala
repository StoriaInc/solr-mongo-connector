package com.codebranch.scala.mongodb.solrconnector

import scala.collection.JavaConversions._
import org.bson.types.BSONTimestamp
import com.mongodb._
import com.mongodb.Bytes.{QUERYOPTION_TAILABLE, QUERYOPTION_AWAITDATA, QUERYOPTION_NOTIMEOUT}
import scala.Some


class MongoOpLog(mongoClient: MongoClient,
                 startTimestamp: Option[BSONTimestamp] = None,
//                 namespace: Option[List[String]] = None,
                 replicaSet: Boolean = true) extends Iterator[MongoOpLogEntry] {
  /**
   * Option[BSONTimestamp]
   */
  lazy val lastOplogTs = getLastTimestamp

  /**
   * DBCollection
   */
  protected val oplog = mongoClient.getDB("local")
      .getCollection(if(replicaSet) "oplog.rs" else "oplog.$main" )

  val tsp = verifyOpLog()

  /**
   * DBCursor
   */
  val cursor = getOplogCursor()

  /**
   * Checks if there is another object available
   * @return
   */
  def hasNext = cursor.hasNext

  def next() = MongoOpLogEntry(cursor.next().asInstanceOf[BasicDBObject])

  def verifyOpLog(): BSONTimestamp = {
    assume(lastOplogTs.isDefined,
           "No oplog found. mongod must be a --master or belong to a Replica Set.")
    startTimestamp match {
      case Some(ts) => {
        oplog.findOne(new BasicDBObject("ts",ts)) match {
          case v: DBObject => ts
          case null =>
            throw new Exception("No oplog entry for requested start timestamp.")
        }
      }
      case None => {
        lastOplogTs.get
      }
    }
  }


  def getLastTimestamp : Option[BSONTimestamp] = {
    val cursor = oplog.find().sort(new BasicDBObject( "$natural", -1 )).limit(1)
    if (cursor.hasNext) {
      Some(cursor.next().get("ts").asInstanceOf[BSONTimestamp])
    } else {
      None
    }
  }

  protected def getOplogCursor() = {

    val query = new BasicDBObject("ts", new BasicDBObject("$gt", tsp))

    oplog.find(query)
      .addOption(QUERYOPTION_NOTIMEOUT)
      .addOption(QUERYOPTION_TAILABLE)
//      .addOption(QUERYOPTION_AWAITDATA)
  }

}


object MongoOpLogEntry {
  def apply(entry: BasicDBObject) = {
    entry.get("op") match {
      case InsertOp.typeCode =>
        MongoInsertOperation(
          entry.get("ts").asInstanceOf[BSONTimestamp],
          Option(entry.getLong("h")),
          entry.getString("ns"),
          entry.get("o").asInstanceOf[BasicDBObject])
      case UpdateOp.typeCode =>
        MongoUpdateOperation(
          entry.get("ts").asInstanceOf[BSONTimestamp],
          Option(entry.getLong("h")),
          entry.getString("ns"),
          entry.get("o").asInstanceOf[BasicDBObject],
          entry.get("o2").asInstanceOf[BasicDBObject])
      case DeleteOp.typeCode =>
        MongoDeleteOperation(
          entry.get("ts").asInstanceOf[BSONTimestamp],
          Option(entry.getLong("h")),
          entry.getString("ns"),
          entry.get("o").asInstanceOf[BasicDBObject])
      case CommandOp.typeCode => MongoCommandOperation(
        entry.get("ts").asInstanceOf[BSONTimestamp],
        Option(entry.getLong("h")),
        entry.getString("ns"),
        entry.get("o").asInstanceOf[BasicDBObject])
      case value: String => MongoNopOperation(
        entry.get("ts").asInstanceOf[BSONTimestamp],
        Option(entry.getLong("h")),
        entry.getString("ns"),
        entry.get("o").asInstanceOf[BasicDBObject])
    }
  }
}

sealed trait OpLogType { def typeCode: String }
// insert operation
case object InsertOp extends OpLogType { val typeCode = "i" }
// update operation
case object UpdateOp extends OpLogType { val typeCode = "u" }
// delete operation
case object DeleteOp extends OpLogType { val typeCode = "d" }
// comman: drop collection or database etc
case object CommandOp extends OpLogType { val typeCode = "c" }
// nop: (re)configuration of replica etc
case object NopOp extends OpLogType { val typeCode = "n" }

sealed trait MongoOpLogEntry {
  val timestamp: BSONTimestamp

  val opID: Option[Long]

  val opType: OpLogType

  val namespace: String

  val document: BasicDBObject

  override def toString = s"$opType in $namespace.\nDocument: $document"
}

case class MongoInsertOperation(timestamp: BSONTimestamp, opID: Option[Long], namespace: String, document: BasicDBObject) extends MongoOpLogEntry {
  val opType = InsertOp
}

case class MongoUpdateOperation(timestamp: BSONTimestamp, opID: Option[Long], namespace: String, document: BasicDBObject, documentID: BasicDBObject) extends MongoOpLogEntry {
  val opType = UpdateOp
  override def toString = super.toString + s"\nDocumentId: $documentID"
}

case class MongoDeleteOperation(timestamp: BSONTimestamp, opID: Option[Long], namespace: String, document: BasicDBObject) extends MongoOpLogEntry {
  val opType = DeleteOp
}

case class MongoCommandOperation(timestamp: BSONTimestamp, opID: Option[Long], namespace: String, document: BasicDBObject) extends MongoOpLogEntry {
  val opType = CommandOp
}
case class MongoNopOperation(timestamp: BSONTimestamp, opID: Option[Long], namespace: String, document: BasicDBObject) extends MongoOpLogEntry {
  val opType = NopOp
}