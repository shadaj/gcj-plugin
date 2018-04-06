package me.shadaj.gcj

import sbt._

object Keys {
  lazy val competitionHost = settingKey[String]("Website on which the competition is being held (does not include /codejam)")
  lazy val contestId = settingKey[String]("ID of the tournament you are participating in")
  lazy val problemLaunchers = settingKey[Map[String, String]]("Map of problem id to problem launcher")
  lazy val commonSources = settingKey[Seq[File]]("List of sources to always include when submitting a solution")
  lazy val problemSources = settingKey[Map[String, Seq[File]]]("Map of problem id to specific sources for that problem")

  lazy val gcjLogin = TaskKey[Unit]("gcjLogin", "Log in to the GCJ server")
  lazy val gcjInitializeContest = TaskKey[Unit]("gcjInit", "Initialize the contest")
  lazy val gcjStatus = TaskKey[Unit]("gcjStatus", "Get the user's status")

  lazy val gcjDownload = InputKey[Unit]("gcjD", "Download the input for a problem")
  lazy val gcjRun = InputKey[Unit]("gcjR", "Run the solution for a problem")
  lazy val gcjSubmit = InputKey[Unit]("gcjS", "Submit the solution for a problem")
  lazy val gcjDownloadRun = InputKey[Unit]("gcjDr", "Download and run the solution for a problem")
  lazy val gcjRunAndSubmit = InputKey[Unit]("gcjRs", "Run and submit the solution for a problem")
  lazy val gcjDownloadRunAndSubmit = InputKey[Unit]("gcjDrs", "Download, run and submit the solution to a problem")
  lazy val gcjZip = InputKey[Unit]("gcjZip", "Zip sources for a problem")
}
