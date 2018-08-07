package refactoring.effects

import cats.MonadError
import cats.implicits._

object EffectsMonadM {

def findBookById[F[_]](id: Int)(implicit me: MonadError[F, Throwable]): F[Book] =
  for {
    row <- DB.queryUnique[F](sql"""select * from catalog where id = $id""")
    format <- parseFormat[F](row[String]("format"))
    (id, title, author) = (row[Int]("id"), row[String]("title"), row[String]("author"))
    book <- format match {
      case Print =>
        me.pure(PrintBook(id, title, author))
      case Digital =>
        parseDownloadType[F](row[Option[String]]("downloadType"), id)
          .map(EBook(id, title, author, _))
    }
  } yield book

def parseFormat[F[_]](s: String)(implicit me: MonadError[F, Throwable]): F[Format] =
  tryParse[F, Format](s, Format.fromString)

def parseDownloadType[F[_]](o: Option[String], id: Int)(
    implicit me: MonadError[F, Throwable]): F[DownloadType] =
  o.map(tryParse[F, DownloadType](_, DownloadType.fromString))
    .getOrElse(
      me.raiseError(new AssertionError(s"download type not provided for digital book $id")))

def tryParse[F[_], A](s: String, parse: String => Option[A])(
    implicit me: MonadError[F, Throwable]): F[A] =
  parse(s)
    .map(me.pure)
    .getOrElse(me.raiseError(new ParseError(s)))

}
