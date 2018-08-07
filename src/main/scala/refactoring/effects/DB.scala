package refactoring.effects

import cats.MonadError

import scala.util.Try

case class Sql(s: String) extends AnyVal

class Row {
  def apply[A](k: String): A = ???
}

object DB {

  def unsafeQueryUnique(q: Sql): Try[Row] = ???

  def queryUnique[F[_]](q: Sql)(implicit me: MonadError[F, Throwable]): F[Row] = ???

}
