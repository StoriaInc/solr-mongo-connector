package me.selfish.solr.mongo

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers



class MongoHelperTest extends FlatSpec with ShouldMatchers {
  import util.MongoHelper._

  "MongoHelper" should "split dbName.Collection to (dbName, Collection)" in {
    val namespace = "dbName.Collection"
    val result = getDbCollectionName( namespace )

    val names = ( "dbName", "Collection" )

    result should equal (names)
  }

}
