package me.shadaj.gcj

import sbt._
import sbt.Keys._

object Settings {
  lazy val competitionHost = settingKey[String]("Website on which the competition is being held (does not include /codejam)")
  lazy val contestId = settingKey[String]("ID of the tournament you are participating in")
  lazy val problemLaunchers = settingKey[Map[String, String]]("Map of problem id to problem launcher")
  lazy val commonSources = settingKey[Seq[File]]("List of sources to always include when submitting a solution")
  lazy val problemSources = settingKey[Map[String, Seq[File]]]("Map of problem id to specific sources for that problem")
}
