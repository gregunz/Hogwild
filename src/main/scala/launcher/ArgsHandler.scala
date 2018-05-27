package launcher

import utils.Utils

import scala.util.Try

object ArgsHandler {
  type Args = Array[String]
  type Options = Map[String, String]

  def argsToMap(args: Args): Options =
    args.flatMap { arg =>
      Try(Utils.split(arg, '=')).toOption
    }.toMap
}
