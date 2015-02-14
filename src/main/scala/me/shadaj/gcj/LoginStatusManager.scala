package me.shadaj.gcj

import java.io.PrintWriter
import java.net.{CookieHandler, CookieManager}
import javafx.application.Platform
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import javax.swing.JFrame

import com.ning.http.client.cookie.Cookie
import com.ning.http.multipart.{FilePart, StringPart}
import dispatch._
import org.fusesource.jansi.Ansi._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import sbt._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions

private class CookieUI(url: String, onURLChange: (String, CookieUI) => Unit) extends JFrame("Google Sign-In") { self =>
  val fxPanel = new JFXPanel
  add(fxPanel)
  setVisible(true)
  setSize(500, 500)
  private val cookieManager = new CookieManager()
  def cookieStore = cookieManager.getCookieStore
  CookieHandler.setDefault(cookieManager)

  Platform.runLater(new Runnable {
    def run(): Unit = {
      val webView = new WebView
      fxPanel.setScene(new Scene(webView, 500, 500))
      webView.getEngine.load(url)
      webView.getEngine.locationProperty().addListener(new ChangeListener[String] {
        def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
          onURLChange(newValue, self)
        }
      })
    }
  })
}

class LoginStatusManager(competitionHost: String, baseDirectory: File, contestId: String, logger: Logger) {
  class GCJException(msg: String) extends Exception(msg)

  val codeJamPythonVersion = "v1.2-beta1"

  val gcjHost = host(competitionHost).secure
  val gcjBase = gcjHost / "codejam"
  val doRequestPath = gcjBase / "contest" / "dashboard" / "do"

  val requestReferer = s"http://$competitionHost/codejam/contest/dashboard?c=$contestId"

  trait LoginStatus {
    def asLoggedInGoogle: Future[LoggedInGoogle]
    def asLoggedInCodeJam: Future[LoggedInCodeJam]
    def asContestInitialized: Future[ContestInitialized]

    def >(that: LoginStatus): Boolean
  }

  case object NotLoggedIn extends LoginStatus {
    private def serverCompatible: Future[Boolean] = {
      val requestPath = gcjBase / "cmdline"
      val request = requestPath.GET <<? Map("cmd" -> "CheckVersion", "version" -> codeJamPythonVersion)

      Http(request).map { result =>
        Json.parse(result.getResponseBody).asOpt[VersionCheck].map { check =>
          if (!check.valid) {
            throw new GCJException("The GCJ server you are connecting to is not compatible with this plugin")
          } else {
            logger.success("The GCJ server is compatible with this plugin")
          }

          check.valid
        }.getOrElse {
          throw new GCJException(s"Unable to parse response from server: ${result.getResponseBody}")
          false
        }
      }
    }

    private def getLoginURL: Future[String] = {
      serverCompatible.flatMap { _ =>
        logger.info("Getting contest status")
        val request = doRequestPath.GET <<?
          Map("cmd" -> "GetInitialValues",
            "contest" -> contestId,
            "zx" -> DateTime.now().getMillis.toString) <:<
          Map("Referer" -> requestReferer)

        Http(request).map { response =>
          val ret = Json.parse(response.getResponseBody).asOpt[ContestStatus]
          ret.map { cs =>
            cs.login_html.split("href=\"")(1).split("\"").head
          }.getOrElse(throw new GCJException("Unable to parse contest status"))
        }
      }
    }

    private def getGoogleCookie: Future[Cookie] = {
      getLoginURL.flatMap { url =>
        logger.info("Please sign in to your Google account in the opened window")
        logger.warn("Do NOT quit the Java process that shows up after the window is opened, doing so will quit SBT")
        val promise = Promise[Cookie]()
        new CookieUI(url, (url, app) => {
          if (url == s"https://$competitionHost/codejam/contest/dashboard?c=$contestId") {
            val maybeCookie = app.cookieStore.getCookies.find(_.getName == "SACSID").map { c =>
              new Cookie(c.getName,
                         c.getValue,
                         c.getValue,
                         c.getDomain,
                         c.getPath,
                         System.currentTimeMillis() + c.getMaxAge,// Since HttpCookie doesn't tell us when the cookie was created we have to guess
                         c.getMaxAge.toInt,
                         c.getSecure,
                         c.isHttpOnly)
            }

            if (maybeCookie.isDefined) {
              logger.success("Extracted cookie after logging in to Google account")
              promise.success(maybeCookie.get)
            } else {
              promise.failure(new GCJException("Unable to extract cookie after logging in"))
            }

            app.setVisible(false)
            app.dispose()
          }
        })

        promise.future
      }
    }

    def asLoggedInGoogle: Future[LoggedInGoogle] = getGoogleCookie.map { cookie =>
        LoggedInGoogle(cookie)
      }

    def asContestInitialized: Future[ContestInitialized] = asLoggedInGoogle.flatMap(_.asContestInitialized)

    def asLoggedInCodeJam: Future[LoggedInCodeJam] = asLoggedInGoogle.flatMap(_.asLoggedInCodeJam)

    def >(that: LoginStatus) = false
  }

  case class LoggedInGoogle(cookie: Cookie) extends LoginStatus {
    private def getMiddlewareTokens: Future[TokenResponse] = {
      logger.info("Getting middleware tokens from GCJ server")
      val requestPath = gcjBase / "middleware"
      val request = requestPath.GET <<? Map(
        "cmd" -> "GetMiddlewareTokens",
        "actions" -> "GetInitialValues,GetInputFile,GetUserStatus,SubmitAnswer"
      ) addCookie cookie

      Http(request).map { result =>
        Json.parse(result.getResponseBody).asOpt[TokenResponse].map { tokens =>
          tokens
        }.getOrElse {
          throw new GCJException("Unable to parse middleware tokens returned by Code Jam server")
        }
      }
    }

    def asLoggedInGoogle: Future[LoggedInGoogle] = Future.successful(this)

    def asLoggedInCodeJam: Future[LoggedInCodeJam] = getMiddlewareTokens.map { tokens =>
      LoggedInCodeJam(cookie, tokens)
    }

    def asContestInitialized: Future[ContestInitialized] = asLoggedInCodeJam.flatMap(_.asContestInitialized)

    def >(that: LoginStatus) = that match {
      case NotLoggedIn => true
      case _ => false
    }
  }

  case class LoggedInCodeJam(cookie: Cookie, tokens: TokenResponse) extends LoginStatus {
    def getContestStatus: Future[ContestStatus] = {
      logger.info("Getting contest status")
      val request = doRequestPath.GET <<?
        Map("cmd" -> "GetInitialValues",
          "contest" -> contestId,
          "zx" -> DateTime.now().getMillis.toString,
          "csrfmiddlewaretoken" -> tokens.tokens.GetInitialValues) <:<
        Map("Referer" -> requestReferer) addCookie cookie

      Http(request).map { response =>
        val ret = Json.parse(response.getResponseBody).asOpt[ContestStatus]

        ret.foreach { p =>
          logger.success("Downloaded contest status")
        }

        ret.getOrElse(throw new GCJException("Unable to parse contest status"))
      }
    }

    def getContestProblems: Future[ProblemList] = {
      logger.info("Getting contest problems")
      val requestPath = gcjBase / "contest" / "dashboard" / "do"
      val request = requestPath.GET <<?
        Map("cmd" -> "GetProblems", "contest" -> contestId) <:<
        Map("Referer" -> requestReferer) addCookie cookie

      Http(request).map { response =>
        val ret = Json.parse(response.getResponseBody).asOpt[ProblemList]

        ret.foreach { p =>
          logger.success("Downloaded contest problems")
        }

        ret.getOrElse(throw new GCJException("Unable to parse problem list"))
      }
    }

    def asLoggedInGoogle: Future[LoggedInGoogle] = Future.successful(LoggedInGoogle(cookie))

    def asLoggedInCodeJam: Future[LoggedInCodeJam] = Future.successful(this)

    def asContestInitialized: Future[ContestInitialized] = getContestStatus.zip(getContestProblems).map { case (contestStatus, problems) =>
      ContestInitialized(cookie, tokens, contestStatus, problems)
    }

    def >(that: LoginStatus) = that match {
      case NotLoggedIn => true
      case LoggedInGoogle(_) => true
      case _ => false
    }
  }

  case class ContestInitialized(cookie: Cookie, tokens: TokenResponse, contestStatus: ContestStatus, problems: ProblemList) extends LoginStatus {
    def getUserStatus: Future[UserStatusNoProblemContext] = {
      logger.info("Getting user status")
      val request = doRequestPath.GET <<?
        Map("cmd" -> "GetUserStatus",
          "contest" -> contestId,
          "zx" -> DateTime.now().getMillis.toString,
          "csrfmiddlewaretoken" -> tokens.tokens.GetUserStatus) <:<
        Map("Referer" -> requestReferer) addCookie cookie

      Http(request).map { response =>
        Json.parse(response.getResponseBody).asOpt[UserStatusNoProblemContext].getOrElse(throw new GCJException("No user status is available for this contest"))
      }
    }

    def outputUserStatus: Future[Unit] = {
      getUserStatus.map { noContext =>
        val status = noContext.withContext(problems)
        logger.info(s"Points: ${status.points}")
        logger.info(s"Rank: ${status.rank}")
        problems.foreach { case Problem(ioSets, _, key, name) =>
          logger.info(s"Problem: $name")
          ioSets.foreach { case io@ProblemSet(_, _, _, points, ioName) =>
            logger.info(s"\tSet: $ioName")
            val ProblemStatus(attempts, timeRemaining, solvedTime, submitted) = status.problemStatus(io)
            if (submitted) {
              if (solvedTime >= 0) {
                logger.info(ansi.render(s"\t\t@|green CORRECT! +$points|@").toString)
              } else {
                logger.info(ansi.render("\t\t@|red Incorrect Solution|@").toString)
              }
            } else {
              logger.info(ansi.render("\t\t@|yellow Solution not submitted|@").toString)
            }
            logger.info(s"\t\tAttempts: $attempts")

            if (timeRemaining != -1) {
              val timeRemainingMins = timeRemaining/60.0
              logger.info(s"\t\tTime Remaining: $timeRemainingMins%.2f min")
            }

            if (solvedTime != -1) {
              val solvedTimeMins = solvedTime/60.0
              logger.info(f"\t\tSolved Time: $solvedTimeMins%.2f min")
            }

            logger.info(s"\t\tSubmitted: $submitted")
          }
        }
      }
    }

    def downloadInput(problemLetter: Char, problem: Problem, set: ProblemSet): Future[Unit] = {
      logger.info(s"Downloading input for ${problem.name} ${set.name}")
      val filename = s"$problemLetter-${set.name}.in"
      val requestPath = doRequestPath / filename
      val request = requestPath.GET <<?
        Map("cmd" -> "GetInputFile",
          "contest" -> contestId,
          "problem" -> problem.id.toString,
          "input_id" -> set.number.toString,
          "filename" -> filename,
          "input_file_type" -> "0",
          "csrfmiddlewaretoken" -> tokens.tokens.GetInputFile,
          "agent" -> s"cmdline-$codeJamPythonVersion") <:<
        Map("Referer" -> requestReferer) addCookie cookie

      Http(request).map { response =>
        val inputDirectory = baseDirectory / "inputs"
        val fileToWrite = baseDirectory / "inputs" / filename
        if (inputDirectory.exists() || inputDirectory.mkdirs()) {
          val writer = new PrintWriter(fileToWrite)
          writer.print(response.getResponseBody)
          writer.close()
          logger.success(s"Downloaded input to ${fileToWrite.getAbsolutePath}")
        } else {
          throw new GCJException("Unable to create directories to download file to")
        }
      }
    }

    def downloadInput(problemLetter: Char, problemSet: String): Future[Unit] = {
      val problem = problems(problemLetter - 'A')
      problem.io_sets.find(_.name == problemSet).map { set =>
        downloadInput(problemLetter, problem, set)
      }.getOrElse {
        Future.failed(new GCJException(s"Unable to find set with name $problemSet"))
      }
    }

    // name -> value
    private implicit def tupleToStringPart(tuple: (String, String)): StringPart =
      new StringPart(tuple._1, tuple._2)
    // (name, filename) -> file
    private implicit def tupleToFilePart(tuple: ((String, String), File)): FilePart = new FilePart(tuple._1._1, tuple._1._2, tuple._2)

    def submitSolution(outputFile: File, sourcesZip: File, problem: Problem, problemSet: ProblemSet): Future[ProblemSubmit] = {
      logger.info(s"Submitting solution for ${problem.name} ${problemSet.name}")
      val request = doRequestPath.POST.
        addBodyPart("csrfmiddlewaretoken" -> tokens.tokens.SubmitAnswer).
        addBodyPart(("answer", outputFile.getName) -> outputFile).
        addBodyPart(("source-file0", sourcesZip.getName) -> sourcesZip).
        addBodyPart("cmd" -> "SubmitAnswer").
        addBodyPart("contest" -> contestId).
        addBodyPart("problem" -> problem.id.toString).
        addBodyPart("input_id" -> problemSet.number.toString).
        addBodyPart("num_source_files" -> "1").
        addBodyPart("agent" -> s"cmdline-$codeJamPythonVersion") <:<
        Map("Content-Encoding" -> "text/plain", "Referer" -> requestReferer) addCookie cookie

      Http(request).map { r =>
        Json.parse(r.getResponseBody).asOpt[ProblemSubmitNoSetContext].map { noContext =>
          val ret = noContext.inContext(contestStatus.statusId, problemSet)
          logger.info(ret.ansiString)
          ret
        }.getOrElse(throw new GCJException("Unable to parse submit response"))
      }
    }

    def submitSolution(problemLetter: Char, problemSet: String, outputFile: File, sourcesZip: File): Future[ProblemSubmit] = {
      val problem = problems(problemLetter - 'A')
      problem.io_sets.find(_.name == problemSet).map { set =>
        submitSolution(outputFile, sourcesZip, problem, set)
      }.getOrElse {
        Future.failed(new GCJException(s"Unable to find set with name $problemSet"))
      }
    }

    def asLoggedInGoogle: Future[LoggedInGoogle] = Future.successful(LoggedInGoogle(cookie))

    def asLoggedInCodeJam: Future[LoggedInCodeJam] = Future.successful(LoggedInCodeJam(cookie, tokens))

    def asContestInitialized: Future[ContestInitialized] = Future.successful(this)

    def >(that: LoginStatus) = true
  }

  private var currentStatus: LoginStatus = NotLoggedIn

  def loggedInGoogle = {
    val ret = currentStatus.asLoggedInGoogle

    ret.foreach { r =>
      if (r > currentStatus) currentStatus = r
    }

    ret
  }

  def loggedInCodeJam = {
    val ret = currentStatus.asLoggedInCodeJam

    ret.foreach { r =>
      if (r > currentStatus) currentStatus = r
    }

    ret
  }

  def contestInitialized = {
    val ret = currentStatus.asContestInitialized

    ret.foreach { r =>
      if (r > currentStatus) currentStatus = r
    }

    ret
  }
}
