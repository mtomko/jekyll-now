package refactoring.effects

import cats.MonadError
import cats.implicits._

object EffectsMonadM {

def findBookById[F[_]](id: Int)(implicit me: MonadError[F, Exception]): F[Book] =
  for {
    row <- DB.queryUniqueM[F](sql"""select * from catalog where id = $id""")
    format <- parseFormat[F](row[String]("format"))
    (id, title, author) = (row[Int]("id"), row[String]("title"), row[String]("author"))
    book <- format match {
      case Print =>
        me.pure(PrintBook(id, title, author))
      case Digital =>
        me.map(parseDownloadType[F](row[String]("downloadType")))(EBook(id, title, author, _))
    }
  } yield book

def parseFormat[F[_]](s: String)(implicit me: MonadError[F, Exception]): F[Format] =
  tryParse[F, Format](s, Format.fromString)

def parseDownloadType[F[_]](s: String)(implicit me: MonadError[F, Exception]): F[DownloadType] =
  tryParse[F, DownloadType](s, DownloadType.fromString)

def tryParse[F[_], A](s: String, parse: String => Option[A])(
    implicit me: MonadError[F, Exception]): F[A] =
  parse(s)
    .map(me.pure)
    .getOrElse(me.raiseError(new ParseError(s)))

}
