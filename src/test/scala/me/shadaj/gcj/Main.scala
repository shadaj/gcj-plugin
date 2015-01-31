package me.shadaj.gcj

import java.io.File

import org.fusesource.jansi.{AnsiConsole, Ansi}
import sbt.{Level, Logger}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main extends App {
  def printlnLogger = new Logger {
    def log(level: Level.Value, message: => String): Unit = println(message)

    def success(message: => String): Unit = println(message)

    def trace(t: => Throwable): Unit = println(t)
  }

  val login = new GCJLogin("code.google.com", new File("."), "32016", printlnLogger)

  def blockify[T](f: Future[T]) = {
    Await.result(f, Duration.Inf)
  }

  blockify(login.login)
  blockify(login.initializeContest)
  blockify(login.outputUserStatus)
  blockify(login.downloadInput('A', "small"))
  System.exit(0)
}
