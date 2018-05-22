package launcher

object ArgsHandler {
  type Args = Array[String]
  type Options = Map[String, String]

  val defaultNames = IndexedSeq("mode", "port", "interval", "worker-ip:worker-port")

  def argsToMap(args: Args): Options =
    args.zipAll(defaultNames, "", "").flatMap { case(arg, defaultName) =>
        arg.split("=").toList match {
          case name :: value :: Nil if name != "" && value != "" =>
            List(name.toLowerCase -> value.toLowerCase)
          case value :: Nil if defaultName != "" && value != "" =>
            List(defaultName.toLowerCase -> value.toLowerCase)
          case _ => Nil
        }
    }.toMap
}
