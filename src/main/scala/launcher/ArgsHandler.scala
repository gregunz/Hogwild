package launcher

import utils.Utils

import scala.util.Try

object ArgsHandler {
  type Args = Array[String]
  type Options = Map[String, String]

  val defaults: Map[String, String] = Map(
    "mode" -> "async",
    "data-path" -> "data/",
    "lambda" -> "0.1",
    "step-size" -> "0.1",
    "port" -> "50500",
    "interval" -> "500",
    "in-second" -> "0"
  )

  def argsToMap(args: Args): Options =
    defaults ++ args
      .flatMap { arg =>
        Try(Utils.split(arg, '=')).toOption
      }
      .toMap
}
