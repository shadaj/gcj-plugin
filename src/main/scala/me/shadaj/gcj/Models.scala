package me.shadaj.gcj

import org.fusesource.jansi.Ansi
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json

sealed trait ContestStatusId {
  def canSubmit: Boolean
}

object ContestStatusId {
  val statusIdForInt = Vector(Planned, Active, Quiet, Finished, Practice)
}

case object Planned extends ContestStatusId {
  val canSubmit = false
}

case object Active extends ContestStatusId {
  val canSubmit = true
}

case object Quiet extends ContestStatusId {
  val canSubmit = false
}

case object Finished extends ContestStatusId {
  val canSubmit = false
}

case object Practice extends ContestStatusId {
  val canSubmit = true
}

case class Secrets(username: String, password: String)

object Secrets {
  implicit val format = Json.format[Secrets]
}

case class VersionCheck(valid: Boolean)

object VersionCheck {
  implicit val format = Json.format[VersionCheck]
}

case class MiddlewareTokens(SubmitAnswer: String,
                            GetInitialValues: String,
                            GetUserStatus: String,
                            GetInputFile: String)

object MiddlewareTokens {
  implicit val format = Json.format[MiddlewareTokens]
}

case class TokenResponse(tokens: MiddlewareTokens, private val expire: String) {
  def expireDate = {
    DateTime.parse(expire, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
  }
}

object TokenResponse {
  implicit val format = Json.format[TokenResponse]
}

case class ProblemSet(suffix: String, difficulty: Int, number: Int, points: Int, name: String)

object ProblemSet {
  implicit val format = Json.format[ProblemSet]
}

case class Problem(io_sets: Array[ProblemSet], id: Long, key: String, name: String)

object Problem {
  implicit val format = Json.format[Problem]
}

case class ContestStatus(login_html: String,
                         version: Int,
                         csrf_middleware_token: String,
                         start_int: Long,
                         admin_html_snippet: String,
                         seconds_left: Long,
                         input_panel_html: String,
                         seconds_until_start: Long,
                         qualified: Boolean,
                         clar_last_seen: Int,
                         logout_html: String,
                         private val cs: Int,
                         logged_in: Boolean,
                         email: Option[String],
                         name: String) {
  val statusId = ContestStatusId.statusIdForInt(cs)
}

object ContestStatus {
  implicit val format = Json.format[ContestStatus]
}

case class ProblemStatus(attempts: Int, timeRemaining: Int, solvedTime: Int, submitted: Boolean)

class UserStatus(attempts: Map[ProblemSet, Int],
                 val rank: Int,
                 timeRemaining: Map[ProblemSet, Int],
                 solvedTime: Map[ProblemSet, Int],
                 submitted: Map[ProblemSet, Int],
                 val points: Int) {
  def problemStatus(io: ProblemSet) = ProblemStatus(attempts(io), timeRemaining(io), solvedTime(io), submitted(io) != 0)
}

case class UserStatusNoProblemContext(private val a: Array[Int],
                                      private val rank: Int,
                                      private val p: Array[Int],
                                      private val s: Array[Int],
                                      private val submitted: Array[Int],
                                      private val pts: Int) {
  def withContext(problems: ProblemList) = {
    new UserStatus(
      problems.flatMap(_.io_sets).zip(a).toMap,
      rank,
      problems.flatMap(_.io_sets).zip(p).toMap,
      problems.flatMap(_.io_sets).zip(s).toMap,
      problems.flatMap(_.io_sets).zip(submitted).toMap,
      pts)
  }
}

object UserStatusNoProblemContext {
  implicit val format = Json.format[UserStatusNoProblemContext]
}

sealed trait ProblemSubmit {
  def ansiString: String
}

case object Rejected extends ProblemSubmit {
  val ansiString = Ansi.ansi().render("@|red Rejected|@").toString
}

case object Correct extends ProblemSubmit {
  val ansiString = Ansi.ansi().render("@|green Correct!|@").toString
}

case object Incorrect extends ProblemSubmit {
  val ansiString = Ansi.ansi().render("@|red Incorrect! Try Again!|@").toString
}

case object Submitted extends ProblemSubmit {
  val ansiString = Ansi.ansi().render("@|yellow Submitted; will be graded at end of contest|@").toString
}

case class ProblemSubmitNoSetContext(private val msg: String,
                                     private val ok: Boolean,
                                     private val hasAnswer: Boolean,
                                     private val inputId: Int,
                                     private val problemId: Int) {
  def inContext(contestStatusId: ContestStatusId, set: ProblemSet): ProblemSubmit = {
    if (!hasAnswer) {
      Rejected
    } else if (set.difficulty == 0 || contestStatusId == Practice) {
      if (ok) {
        Correct
      } else {
        Incorrect
      }
    } else {
      Submitted
    }
  }
}

object ProblemSubmitNoSetContext {
  implicit val format = Json.format[ProblemSubmitNoSetContext]
}