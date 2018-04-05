package me.shadaj.gcj

import sbt.{File, Logger}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GCJLogin(competitionHost: String, baseDirectory: File, contestId: String, logger: Logger) {
  val loginManager = new LoginStatusManager(competitionHost, baseDirectory, contestId, logger)

  def logGCJErrors[T](future: Future[T]) = {
    future.recover {
      case e: GCJException => sys.error(e.getMessage)
    }
  }

  def login = logGCJErrors(loginManager.loggedInCodeJam)
  def initializeContest = logGCJErrors(loginManager.contestInitialized)
  def outputUserStatus = logGCJErrors(loginManager.contestInitialized.flatMap(_.outputUserStatus))
  def downloadInput(problemLetter: Char, set: String) = logGCJErrors(loginManager.contestInitialized.flatMap(_.downloadInput(problemLetter, set)))
  def submitSolution(problemLetter: Char, set: String, outputFile: File, zipFile: File) = logGCJErrors(loginManager.contestInitialized.flatMap(_.submitSolution(problemLetter, set, outputFile, zipFile)))
}
