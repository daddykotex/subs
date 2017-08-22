package io.github.daddykotex.subs

import java.util.concurrent.Executors

import scala.util.Properties.envOrNone
import fs2.{Stream, Task}
import org.http4s.util.StreamApp
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object Server extends StreamApp {

  val port: Int = envOrNone("HTTP_PORT") map (_.toInt) getOrElse 8080
  val ip: String = "0.0.0.0"
  val pool: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  override def stream(args: List[String]): Stream[Task, Nothing] =
    BlazeBuilder
      .bindHttp(port, ip)
      .mountService(Web.app)
      .withExecutionContext(pool)
      .serve
}
