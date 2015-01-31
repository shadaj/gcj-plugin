package me.shadaj.gcj

import sbt._
import sbt.Keys._
import sbt.complete.DefaultParsers._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object Tasks {
  lazy val login = taskKey[Unit]("Log in to the GCJ server")
  lazy val initializeContest = taskKey[Unit]("Initialize the contest")
  lazy val userStatus = taskKey[Unit]("Get the user's status")
  lazy val downloadRunAndSubmit = inputKey[Unit]("Run and submit the solution to a problem")
}

object TasksImpl {
  private var maybeGcjLogin: Option[GCJLogin] = None

  def blockify[T](f: Future[T]) = {
    Await.result(f, Duration.Inf)
  }

  lazy val loginImpl = Def.task {
    if (maybeGcjLogin.isEmpty) {
      maybeGcjLogin = Some(new GCJLogin(Settings.competitionHost.value,
        baseDirectory.value,
        Settings.contestId.value,
        streams.value.log))
    }

    maybeGcjLogin.map(l => blockify(l.login)).get
  }

  lazy val initializeContestImpl = Def.task {
    if (maybeGcjLogin.isEmpty) {
      maybeGcjLogin = Some(new GCJLogin(Settings.competitionHost.value,
        baseDirectory.value,
        Settings.contestId.value,
        streams.value.log))
    }

    maybeGcjLogin.map(l => blockify(l.initializeContest)).get
  }

  lazy val userStatusImpl = Def.task {
    if (maybeGcjLogin.isEmpty) {
      maybeGcjLogin = Some(new GCJLogin(Settings.competitionHost.value,
        baseDirectory.value,
        Settings.contestId.value,
        streams.value.log))
    }

    maybeGcjLogin.map(l => blockify(l.outputUserStatus)).get
  }

  lazy val downloadRunAndSubmitImpl = Def.inputTaskDyn {
    if (maybeGcjLogin.isEmpty) {
      maybeGcjLogin = Some(new GCJLogin(Settings.competitionHost.value,
        baseDirectory.value,
        Settings.contestId.value,
        streams.value.log))
    }

    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val problemLetter = args.head
    val problemSet = args(1)

    val problemLaunchers: Map[String, String] = Settings.problemLaunchers.value
    val launcherClass: String = problemLaunchers(problemLetter)

    (compile in Compile).toTask.value
    maybeGcjLogin.map { l =>
      blockify(l.downloadInput(problemLetter.head, problemSet).map { r =>
        (baseDirectory.value / "outputs").mkdir()
        val inputFile = baseDirectory.value / "inputs" / s"$problemLetter-$problemSet.in"
        val outputFile = baseDirectory.value / "outputs" / s"$problemLetter-$problemSet.out"
        (runMain in Compile).toTask(s" $launcherClass ${inputFile.absolutePath} ${outputFile.absolutePath}").map { u =>
          if (readLine("Would you like to submit the solution [y/n]? ") == "y") {
            val toZip = (Settings.commonSources.value ++ Settings.problemSources.value.getOrElse(problemLetter, Seq.empty)).map { f =>
              (f, baseDirectory.value.toURI.relativize(f.toURI).getPath)
            }

            (baseDirectory.value / "zips").mkdir()
            val zipFile = baseDirectory.value / "zips" / s"$problemLetter.zip"
            IO.zip(toZip, zipFile)
            streams.value.log.success(s"Created zip file of sources at $zipFile")

            blockify(l.submitSolution(problemLetter.head, problemSet, outputFile, zipFile))
            ()
          } else {
            streams.value.log.error("Aborting submit")
          }
        }
      })
    }.get
  }
}
