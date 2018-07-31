package refactoring.effects

import scala.util.{Failure, Success, Try}

object EffectsMonad {

def findBookById(id: Int): Try[Book] =
  for {
    row <- DB.queryUnique(sql"""select * from catalog where id = $id""")
    format <- parseFormat(row[String]("format"))
    (id, title, author) = (row[Int]("id"), row[String]("title"), row[String]("author"))
    book <- format match {
      case Print =>
        Success(PrintBook(id, title, author))
      case Digital =>
        parseDownloadType(row[Option[String]]("download_type"), id)
          .map(EBook(id, title, author, _))
    }
  } yield book

def parseFormat(s: String): Try[Format] = tryParse(s, Format.fromString)

def parseDownloadType(o: Option[String], id: Int): Try[DownloadType] =
  o.map(tryParse(_, DownloadType.fromString))
    .getOrElse(Failure(new AssertionError(s"download type not provided for digital book $id")))

def tryParse[A](s: String, parse: String => Option[A]): Try[A] =
  parse(s)
    .map(Success(_))
    .getOrElse(Failure(new ParseError(s)))

}
