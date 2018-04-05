sbtPlugin := true

name := "gcj-plugin"

organization := "me.shadaj"

scalaVersion := "2.12.5"

libraryDependencies += "org.dispatchhttp" %% "dispatch-core" % "0.14.0"

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.7"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.9"

fork in run := true
