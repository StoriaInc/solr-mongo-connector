solr-mongo-connector
=====================

##System overview##

###System requirements:###
1. It requires a mongodb replica set setup.
2. JVM

To start the connector simply run
java -Dconfig.file=connector.conf -Dlogback.configurationFile=connector-logger.xml  -jar  scala-mongo-connector.jar

-Dconfig.file=connector.conf - defines path to main configuration file
-Dlogback.configurationFile=connector-logger.xml - defines path to logger configuration file
scala-mongo-connector.jar - path to mongo connector jar

###Main configuration file:###
connector {
   // file to store last timestamps
   timestamps.file = "last_timestamps"

   // mongo connection string to replica set
   mongo.uri = ["127.0.0.1:27017"]

   // Solr uri
   solr.uri = "http://localhost:8983/solr"

   // list of collections to process
   namespace = [ "db.Collection" ]

   // max number of records to upsert
   bulk.max_upsert = 100

   // number of entries to be processed in one actor receive loop
   max-entries-for-process = 1000

   // ms delay of worker when all records taken from OpLog
   worker.sleep = 1000
}

timestamps.file - path to file to store timestamp of last processed record from oplog. If file does exists, connector takes timestamp from this file and uses it as entr$

mongo.ur - list of mongodb addresses.

solr.uri - Apache Solr URI. Only one URI for all namespaces. Current version of mongo connector do not support solr multi core functionality.

namespace - list of collections to process. Correct format is “databaseName.collectionName”. Example: [ “db.Collection”, “db.Example” ]. This param can not be empty. Yo$


##Usage With Solr##

###Nested documents and solr scheme###

Solr has a flat scheme with no nested documents or fields. Connector flattens mongodb nested documents into flat map. It uses path to the element as new name and leaves$
Example:
{
user: {
 _id: “1”,
name: “Name”,
contacts: {
email: “user@example.com”,
“phone”: “+00000000”
},
tags: [ “tag1”, “tag2”, “tag3”],
interests: [
        { _id: “i1”, title: “MongoDB”},
        { _id: “i2”, title: “Scala” },
        { _id: “i3”, title: “MoScal” }
]
}
 }
Connector transforms this document into:
{
user._id: “1”,
user.name: “Name”,
user.contacts.email: “user@example.com”,
user.contacts.phone: “+00000000”,
user.tags: [ “tag1”, “tag2”, “tag3”],
interests._id: [ “i1”, “i2”, “i3” ],
interests.title: [ “MongoDB”, “Scala”, “MoScal” ]
}

###Required fields###
_id - stores MongoDB collection id
_ts - stores timestamp
ns - stores namespace

<field name="_id" type="string" indexed="true" stored="true" required="true" />
<field name="_ts" type="long" indexed="true" stored="true"/>
<field name="ns" type="text_general" indexed="true" stored="true"/>

The schema also sets the _id field to be the unique key:
<uniqueKey>_id</uniqueKey>

