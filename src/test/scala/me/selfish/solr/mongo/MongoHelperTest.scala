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
