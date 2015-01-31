package me.shadaj.gcj

import sbt._
import sbt.Keys._

object GCJPlugin extends Plugin {
  override def buildSettings = Seq(libraryDependencies += "me.shadaj" %% "gcj-parser" % "0.1-SNAPSHOT")
  override lazy val projectSettings = Seq(
    Tasks.login := TasksImpl.loginImpl.value,
    Tasks.initializeContest := TasksImpl.initializeContestImpl.value,
    Tasks.userStatus := TasksImpl.userStatusImpl.value,
    Tasks.downloadRunAndSubmit := TasksImpl.downloadRunAndSubmitImpl.evaluated,
    Settings.competitionHost := "code.google.com",
    Settings.problemLaunchers := Map.empty,
    Settings.commonSources := Seq.empty,
    Settings.problemSources := Map.empty,
    cleanFiles <++= baseDirectory { base => Seq(base / "inputs", base / "outputs", base / "zips")}
  )
}
