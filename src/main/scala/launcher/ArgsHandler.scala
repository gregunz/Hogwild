package launcher

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
    "in-second" -> "0",
    "early-stopping" -> "10",
    "min-loss" -> "0"
  )

  def argsToMap(args: Args): Options =
    defaults ++ args.flatMap { arg =>
      arg.split("=").toList match {
        case name :: value :: Nil if name != "" && value != "" =>
          List(name.toLowerCase -> value.toLowerCase)
        case _ => Nil
      }
    }.toMap
}
