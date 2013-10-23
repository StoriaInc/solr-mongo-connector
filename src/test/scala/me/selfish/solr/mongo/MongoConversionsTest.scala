package me.selfish.solr.mongo

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import com.mongodb.{BasicDBList, BasicDBObject}
import org.bson.types.ObjectId
import MongoConversions._


class MongoConversionsTest extends FlatSpec with ShouldMatchers {

  "MongoConversions" should "transform BasicDBObject with simple data types to Map with simple data types" in {
    val objectId = new ObjectId()
    val short:Short = 32
    val int:Int = 256
    val long:Long = 1024
    val float:Float = 32
    val double:Double = 128.32
    val char:Char = 'A'
    val string:String = "lorem ipsum"
    val boolean:Boolean = false

    val correctMap = Map(
      "_id" -> objectId,
      "short" -> short,
      "int" -> int,
      "long" -> long,
      "float" -> float,
      "double" -> double,
      "char" -> char,
      "string" -> string,
      "boolean" -> boolean
    )

    val dbObject = new BasicDBObject( "_id", objectId )
    dbObject.append("short",short)
    dbObject.append("int",int)
    dbObject.append("long",long)
    dbObject.append("float",float)
    dbObject.append("double",double)
    dbObject.append("char",char)
    dbObject.append("string",string)
    dbObject.append("boolean",boolean)
    dbObject.append("null",null)

    val result = convert( dbObject )
    result should equal (correctMap)
  }


  it should "transform { key: BasicDBObject} to Map(key.(%subkeys) -> %value)" in {
    val objectId = new ObjectId()
    val short:Short = 32
    val int:Int = 256
    val long:Long = 1024
    val float:Float = 32
    val double:Double = 128.32
    val char:Char = 'A'
    val string:String = "lorem ipsum"
    val boolean:Boolean = false

    val correctMap = Map(
      "key._id" -> objectId,
      "key.short" -> short,
      "key.int" -> int,
      "key.long" -> long,
      "key.float" -> float,
      "key.double" -> double,
      "key.char" -> char,
      "key.string" -> string,
      "key.boolean" -> boolean
    )

    val dbObject = new BasicDBObject( "_id", objectId )
    dbObject.append("short",short)
    dbObject.append("int",int)
    dbObject.append("long",long)
    dbObject.append("float",float)
    dbObject.append("double",double)
    dbObject.append("char",char)
    dbObject.append("string",string)
    dbObject.append("boolean",boolean)
    dbObject.append("null",null)
    val nestedDBObject = new BasicDBObject( "key", dbObject )

    val result = convert( nestedDBObject )
    result should equal (correctMap)
  }


  it should "transform nested objects to flat map" in {
    val level_3 = new BasicDBObject( "level_3.1", "value_3.1" ).append( "level_3.2", "value_3.2" )
    val level_2 = new BasicDBObject( "level_2", level_3 )
    val level_2_a = new BasicDBObject( "level_2a", level_3 )
    val level1 = new BasicDBObject( "level_1", level_2 ).append( "level_a", level_2_a )

    val correctMap = Map(
      "level_1.level_2.level_3.1" -> "value_3.1",
      "level_1.level_2.level_3.2" -> "value_3.2",
      "level_a.level_2a.level_3.1" -> "value_3.1",
      "level_a.level_2a.level_3.2" -> "value_3.2"
    )

    val result = convert( level1 )
    result should equal (correctMap)
  }


  it should "transform Sequences and List of simple data types to Map( %key -> %seq|%list)" in {
    val seqInt = Seq[Int](1,2,3,4,5,10,20,30,40,50)
    val seqStr = Seq[String]("lorem","ipsum","dolor","sit","amet")
    val listInt = List[Int](1,2,3,4,5,10,20,30,40,50)
    val listString = List[String]("lorem","ipsum","dolor","sit","amet")

    val dbObject = new BasicDBObject()
        .append("seqInt", seqInt)
        .append("seqStr", seqStr)
        .append("listInt", listInt)
        .append("listString", listString)

    val correctMap = Map(
      "seqInt" -> listInt,
      "seqStr" -> listString,
      "listInt" -> listInt,
      "listString" -> listString
    )

    val result = convert(dbObject)
    result should equal (correctMap)
  }

  it should "transform list of objects to map( object.param -> List(object1.param, object2.param ... objectN.param) )" in {
    val listOfObjects = List(
      new BasicDBObject().append("cParam1", "cParam1-1").append("cParam2", 1),
      new BasicDBObject().append("cParam1", "cParam1-2").append("cParam2", 2),
      new BasicDBObject().append("cParam1", "cParam1-3").append("cParam2", 3)
    )
    val DBListOfObjects = new BasicDBList()
    //listOfObjects.foreach( value => DBListOfObjects.add(value) )
    listOfObjects.map( value => DBListOfObjects.add(value) )

    println(DBListOfObjects.toString)

    val dbObject = new BasicDBObject()
        .append( "list", DBListOfObjects )

    val correctMap = Map(
      "list.cParam1" -> List("cParam1-1","cParam1-2","cParam1-3"),
      "list.cParam2" -> List(1,2,3)
    )

    val result = convert(dbObject)
    result should equal (correctMap)
  }

  it should "pass on BasicDBObject and Key" in {
    val key = "collaborators"
    val accountId = 25
    val confirm = true

    val dbObject = new BasicDBObject()
        .append("accountId", accountId)
        .append("confirm", confirm)

    val correctMap = Map(
      s"$key.accountId" -> accountId,
      s"$key.confirm" -> confirm
    )

    val result = convert(dbObject, key)
    result should equal (correctMap)
  }

}
