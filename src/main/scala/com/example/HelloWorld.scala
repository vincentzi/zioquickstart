package com.example

import zio.http.model.Method
import zio.http._
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, Console}

object HelloWorld extends ZIOAppDefault {

  val app: HttpApp[Any, Nothing] = Http.collect[Request] {
    case Method.GET -> !! / "text" => Response.text("Hello World with ZIO!")
  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    for {
      _ <- Console.printLine("Starting the server")
      _ <- Server.serve(app).provide(Server.default)
    } yield ()
}
