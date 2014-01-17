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
import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._


object ApplicationBuild extends Build {
  private def frumaticRepository(r : String) : Resolver =
    "Sonatype Nexus Repository Manager" at "http://nexus.frumatic.com/content/repositories/" + r

  val frumaticRepositorySnapshots = frumaticRepository("snapshots")
  val frumaticRepositoryReleases = frumaticRepository("releases")

  val appName       = "scala-mongo-connector"
  val isSnapshot    = true
  val version       = "1.0" + (if (isSnapshot) "-SNAPSHOT" else "")

  val scalaStyleSettings = org.scalastyle.sbt.ScalastylePlugin.Settings

  val buildSettings = Defaults.defaultSettings ++ assemblySettings ++ scalaStyleSettings ++ Seq (
    organization := "SelfishInc",
    Keys.version := version,
    scalaVersion := Versions.ScalaVersion,
    scalacOptions in ThisBuild ++= Seq(
      "-feature",
      "-language:postfixOps",
      "-deprecation"
    ),
    retrieveManaged := true,
    test in assembly := {},
    //trying to fix GC limit overhead on hiload
    javaOptions in run ++= Seq(
      "-d64", "-Xmx2G", "-XX:-UseConcMarkSweepGC"
    ),
    testOptions in Test := Nil,
    libraryDependencies ++= appDependencies,
    resolvers ++= Seq(
      "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
    ),
    exportJars := true,
    publishTo := {
      if (isSnapshot)
        Some(frumaticRepositorySnapshots)
      else
        Some(frumaticRepositoryReleases)
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )

  import Versions._

  val appDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor" % AkkaVersion ,
    "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,

    "org.mongodb" % "mongo-java-driver" % "2.11.3",
    "org.scalatest" %% "scalatest" % "1.9.2",
    "org.apache.solr" % "solr-solrj" % "4.5.0",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
    "commons-logging" % "commons-logging" % "1.1.1"
  )

  val main = Project(
    appName,
    file("."),
    settings = buildSettings
  )
}

object Versions {
  val ScalaVersion = "2.10.3"
  val AkkaVersion = "2.2.2"
}