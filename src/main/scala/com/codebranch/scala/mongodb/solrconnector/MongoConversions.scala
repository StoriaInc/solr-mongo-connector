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
package com.codebranch.scala.mongodb.solrconnector

import scala.collection.JavaConversions._
import com.mongodb.{BasicDBList, BasicDBObject}


object MongoConversions {

  def convert(dbObject: BasicDBObject): Map[String, Any] = dbObject.map {
      case(k, v) => convert(v, k)
    }.reduce(_++_)


  def convert(value: Any, path: String = ""): Map[String, Any] = {
    val map: Map[String, Any] = value match {
      case null => {
        Map(path -> null)
      }
      case v:BasicDBObject => {
        val list = v.map { case (k, dbo) =>
          convert(dbo, getKeyName(k, path))
        }.toList
        if (list.isEmpty) {
          Map(path -> null)
        } else {
          list.filter((a:Map[String,Any]) => a != null).reduce(_++_)
        }
      }
      case v:BasicDBList => {
        var sumMap = Map[String, Any]()
        v.toList.map{ value =>
          if (value.isInstanceOf[AnyRef]) {
            convert(value, path).foreach( tuple => {
                sumMap = sumMap.updated( tuple._1, sumMap.getOrElse(tuple._1,Nil).asInstanceOf[List[Any]] :+ tuple._2 )
              }
            )
          } else {
            sumMap.updated( path, sumMap.getOrElse(path, Nil).asInstanceOf[List[Any]] :+ value )
          }
        }
        sumMap
      }
      case v => {
        Map(path -> v)
      }
    }
    map.filter((p:((String, Any))) => p._2 != null)
  }


  def getKeyName(key: String, path: String) = {
    if (path.length > 0) s"$path.$key"
    else key
  }
}
