sbtPlugin := true

name := "gcj-plugin"

organization := "me.shadaj"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.7"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.3"

fork in run := true