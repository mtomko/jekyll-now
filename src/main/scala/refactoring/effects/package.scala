package refactoring

package object effects {

  implicit class SqlHelper(private val sc: StringContext) extends AnyVal {
    def sql(args: Any*): Sql = Sql(args.mkString)
  }

}
