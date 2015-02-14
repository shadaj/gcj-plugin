package me.shadaj.gcj

import sbt._
import sbt.Keys._
import sbt.complete.DefaultParsers._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import Keys._

object Tasks {
  private var maybeGcjLogin: Option[GCJLogin] = None

  def blockify[T](f: Future[T]) = {
    Await.result(f, Duration.Inf)
  }

  lazy val buildGcjLogin = Def.task {
    if (maybeGcjLogin.isEmpty) {
      maybeGcjLogin = Some(new GCJLogin(competitionHost.value,
        baseDirectory.value,
        contestId.value,
        streams.value.log))
    }
  }

  lazy val loginImpl = Def.task {
    buildGcjLogin.value
    maybeGcjLogin.map(l => blockify(l.login)).get
  }

  lazy val initializeContestImpl = Def.task {
    buildGcjLogin.value
    maybeGcjLogin.map(l => blockify(l.initializeContest)).get
  }

  lazy val userStatusImpl = Def.task {
    buildGcjLogin.value
    maybeGcjLogin.map(l => blockify(l.outputUserStatus)).get
  }

  lazy val downloadImpl = Def.inputTask {
    buildGcjLogin.value

    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val problemLetter = args.head
    val problemSet = args(1)

    maybeGcjLogin.map { l =>
      blockify(l.downloadInput(problemLetter.head, problemSet))
    }.get
  }

  lazy val runImpl = Def.inputTaskDyn {
    buildGcjLogin.value

    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val problemLetter = args.head
    val problemSet = args(1)

    val problemLaunchers: Map[String, String] = Keys.problemLaunchers.value
    val launcherClass: String = problemLaunchers(problemLetter)

    (compile in Compile).toTask.value
    maybeGcjLogin.map { l =>
      (baseDirectory.value / "outputs").mkdir()
      val inputFile = baseDirectory.value / "inputs" / s"$problemLetter-$problemSet.in"
      val outputFile = baseDirectory.value / "outputs" / s"$problemLetter-$problemSet.out"
      (runMain in Compile).toTask(s" $launcherClass ${inputFile.absolutePath} ${outputFile.absolutePath}")
    }.get
  }

  lazy val submitImpl = Def.inputTask {
    buildGcjLogin.value

    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val problemLetter = args.head
    val problemSet = args(1)

    maybeGcjLogin.map { l =>
      val outputFile = baseDirectory.value / "outputs" / s"$problemLetter-$problemSet.out"

      val toZip = (commonSources.value ++ problemSources.value.getOrElse(problemLetter, Seq.empty)).map { f =>
        (f, baseDirectory.value.toURI.relativize(f.toURI).getPath)
      }

      (baseDirectory.value / "zips").mkdir()
      val zipFile = baseDirectory.value / "zips" / s"$problemLetter.zip"
      IO.zip(toZip, zipFile)
      streams.value.log.success(s"Created zip file of sources at $zipFile")

      blockify(l.submitSolution(problemLetter.head, problemSet, outputFile, zipFile))
      ()
    }.get
  }

  lazy val downloadRunImpl = Def.inputTaskDyn {
    buildGcjLogin.value

    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val problemLetter = args.head
    val problemSet = args(1)

    val problemLaunchers: Map[String, String] = Keys.problemLaunchers.value
    val launcherClass: String = problemLaunchers(problemLetter)

    (compile in Compile).toTask.value
    maybeGcjLogin.map { l =>
      blockify(l.downloadInput(problemLetter.head, problemSet).map { r =>
        (baseDirectory.value / "outputs").mkdir()
        val inputFile = baseDirectory.value / "inputs" / s"$problemLetter-$problemSet.in"
        val outputFile = baseDirectory.value / "outputs" / s"$problemLetter-$problemSet.out"
        (runMain in Compile).toTask(s" $launcherClass ${inputFile.absolutePath} ${outputFile.absolutePath}")
      })
    }.get
  }

  lazy val runAndSubmitImpl = Def.inputTaskDyn {
    buildGcjLogin.value

    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val problemLetter = args.head
    val problemSet = args(1)

    val problemLaunchers: Map[String, String] = Keys.problemLaunchers.value
    val launcherClass: String = problemLaunchers(problemLetter)

    (compile in Compile).toTask.value
    maybeGcjLogin.map { l =>
      (baseDirectory.value / "outputs").mkdir()
      val inputFile = baseDirectory.value / "inputs" / s"$problemLetter-$problemSet.in"
      val outputFile = baseDirectory.value / "outputs" / s"$problemLetter-$problemSet.out"
      (runMain in Compile).toTask(s" $launcherClass ${inputFile.absolutePath} ${outputFile.absolutePath}").map { u =>
        if (readLine("Would you like to submit the solution [y/n]? ") == "y") {
          val toZip = (commonSources.value ++ problemSources.value.getOrElse(problemLetter, Seq.empty)).map { f =>
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
    }.get
  }

  lazy val downloadRunAndSubmitImpl = Def.inputTaskDyn {
    buildGcjLogin.value

    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val problemLetter = args.head
    val problemSet = args(1)

    val problemLaunchers: Map[String, String] = Keys.problemLaunchers.value
    val launcherClass: String = problemLaunchers(problemLetter)

    (compile in Compile).toTask.value
    maybeGcjLogin.map { l =>
      blockify(l.downloadInput(problemLetter.head, problemSet).map { r =>
        (baseDirectory.value / "outputs").mkdir()
        val inputFile = baseDirectory.value / "inputs" / s"$problemLetter-$problemSet.in"
        val outputFile = baseDirectory.value / "outputs" / s"$problemLetter-$problemSet.out"
        (runMain in Compile).toTask(s" $launcherClass ${inputFile.absolutePath} ${outputFile.absolutePath}").map { u =>
          if (readLine("Would you like to submit the solution [y/n]? ") == "y") {
            val toZip = (commonSources.value ++ problemSources.value.getOrElse(problemLetter, Seq.empty)).map { f =>
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
