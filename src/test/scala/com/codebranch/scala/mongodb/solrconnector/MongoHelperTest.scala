package com.codebranch.scala.mongodb.solrconnector

import collection.mutable.Stack
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers



class MongoHelperTest extends FlatSpec with ShouldMatchers {
  import com.codebranch.scala.mongodb.solrconnector.util.MongoHelper._

  "MongoHelper" should "split dbName.Collection to (dbName, Collection)" in {
    val namespace = "dbName.Collection"
    val result = getDbCollectionName( namespace )

    val names = ( "dbName", "Collection" )

    result should equal (names)
  }

}
