package refactoring.effects

import scala.util.{Failure, Success, Try}

object EffectsTail {

def findBookById(id: Int): Try[Book] =
  DB.queryUnique(sql"""select * from catalog where id = $id""").flatMap { row =>
    val id = row[Int]("id")
    val title = row[String]("title")
    val author = row[String]("author")
    val formatStr = row[String]("format")
    val downloadTypeStr = row[Option[String]]("download_type")
    extractBook(id, title, author, formatStr, downloadTypeStr)
  }

def extractBook(
    id: Int,
    title: String,
    author: String,
    formatStr: String,
    downloadTypeStrOpt: Option[String]): Try[Book] =
  Format.fromString(formatStr) match {
    case None => Failure(new ParseError(formatStr))
    case Some(Print) =>
      Success(PrintBook(id, title, author))
    case Some(Digital) =>
      extractEBook(id, title, author, downloadTypeStrOpt)
  }

def extractEBook(
    id: Int,
    title: String,
    author: String,
    downloadTypeStrOpt: Option[String]): Try[EBook] =
  downloadTypeStrOpt match {
    case None => Failure(new AssertionError())
    case Some(downloadTypeStr) =>
      DownloadType.fromString(downloadTypeStr) match {
        case None =>
          Failure(new ParseError(downloadTypeStr))
        case Some(dt) =>
          Success(EBook(id, title, author, dt))
      }
  }

}
