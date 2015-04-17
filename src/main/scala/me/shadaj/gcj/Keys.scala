package me.shadaj.gcj

import sbt._

object Keys {
  lazy val competitionHost = settingKey[String]("Website on which the competition is being held (does not include /codejam)")
  lazy val contestId = settingKey[String]("ID of the tournament you are participating in")
  lazy val problemLaunchers = settingKey[Map[String, String]]("Map of problem id to problem launcher")
  lazy val commonSources = settingKey[Seq[File]]("List of sources to always include when submitting a solution")
  lazy val problemSources = settingKey[Map[String, Seq[File]]]("Map of problem id to specific sources for that problem")

  lazy val login = TaskKey[Unit]("gcjLogin", "Log in to the GCJ server")
  lazy val initializeContest = TaskKey[Unit]("gcjInit", "Initialize the contest")
  lazy val userStatus = TaskKey[Unit]("gcjStatus", "Get the user's status")

  lazy val download = InputKey[Unit]("gcjD", "Download the input for a problem")
  lazy val run = InputKey[Unit]("gcjR", "Run the solution for a problem")
  lazy val submit = InputKey[Unit]("gcjS", "Submit the solution for a problem")
  lazy val downloadRun = InputKey[Unit]("gcjDr", "Download and run the solution for a problem")
  lazy val runAndSubmit = InputKey[Unit]("gcjRs", "Run and submit the solution for a problem")
  lazy val downloadRunAndSubmit = InputKey[Unit]("gcjDrs", "Download, run and submit the solution to a problem")
  lazy val zip = InputKey[Unit]("gcjZip", "Zip sources for a problem")
}
