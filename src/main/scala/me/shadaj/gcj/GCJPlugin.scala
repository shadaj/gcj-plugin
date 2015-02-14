package me.shadaj.gcj

import sbt._
import sbt.Keys._

object GCJPlugin extends AutoPlugin {
  override def buildSettings = Seq(libraryDependencies += "me.shadaj" %% "gcj-parser" % "0.1-SNAPSHOT")
  val autoImport = Keys

  override lazy val projectSettings = Seq(
    Keys.login := Tasks.loginImpl.value,
    Keys.initializeContest := Tasks.initializeContestImpl.value,
    Keys.userStatus := Tasks.userStatusImpl.value,
    Keys.download := Tasks.downloadImpl.evaluated,
    Keys.run := Tasks.runImpl.evaluated,
    Keys.submit := Tasks.submitImpl.evaluated,
    Keys.downloadRun := Tasks.downloadRunImpl.evaluated,
    Keys.runAndSubmit := Tasks.runAndSubmitImpl.evaluated,
    Keys.downloadRunAndSubmit := Tasks.downloadRunAndSubmitImpl.evaluated,
    Keys.competitionHost := "code.google.com",
    Keys.problemLaunchers := Map.empty,
    Keys.commonSources := Seq.empty,
    Keys.problemSources := Map.empty,
    cleanFiles <++= baseDirectory { base => Seq(base / "inputs", base / "outputs", base / "zips")}
  )
}
