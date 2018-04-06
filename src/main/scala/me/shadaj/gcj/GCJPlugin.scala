package me.shadaj.gcj

import sbt._
import sbt.Keys._

object GCJPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  val autoImport = Keys

  override lazy val projectSettings = Seq(
    Keys.gcjLogin := Tasks.loginImpl.value,
    Keys.gcjInitializeContest := Tasks.initializeContestImpl.value,
    Keys.gcjStatus := Tasks.userStatusImpl.value,
    Keys.gcjDownload := Tasks.downloadImpl.evaluated,
    Keys.gcjRun := Tasks.runImpl.evaluated,
    Keys.gcjSubmit := Tasks.submitImpl.evaluated,
    Keys.gcjDownloadRun := Tasks.downloadRunImpl.evaluated,
    Keys.gcjRunAndSubmit := Tasks.runAndSubmitImpl.evaluated,
    Keys.gcjDownloadRunAndSubmit := Tasks.downloadRunAndSubmitImpl.evaluated,
    Keys.gcjZip := Tasks.zipImpl.evaluated,
    Keys.competitionHost := "code.google.com",
    Keys.problemLaunchers := Map.empty,
    Keys.commonSources := Seq.empty,
    Keys.problemSources := Map.empty,
    cleanFiles ++= baseDirectory { base => Seq(base / "inputs", base / "outputs", base / "zips") }.value
  )
}
