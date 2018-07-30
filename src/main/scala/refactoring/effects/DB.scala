package refactoring.effects

import cats.MonadError

import scala.util.Try

class Row {
  def apply[A](k: String): A = ???
}

object DB {

  def queryUnique(q: Sql): Try[Row] = ???

  def queryUniqueM[F[_]](q: Sql)(implicit me: MonadError[F, Exception]): F[Row] = ???

}
