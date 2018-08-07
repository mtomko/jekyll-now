package refactoring.effects

import scala.util.{Failure, Success, Try}

object EffectsOrig {

def findBookById(id: Int): Try[Book] = {
  // queryUnique returns a `Try[Row]`
  DB.unsafeQueryUnique(sql"""select * from catalog where id = $id""").flatMap { row =>
    // pick out the properties every book possesses
    val id = row[Int]("id")
    val title = row[String]("title")
    val author = row[String]("author")
    val formatStr = row[String]("format")

    // now start to determine the types - get the format first
    Format.fromString(formatStr) match {
      case None =>
        Failure(new ParseError(formatStr))
      case Some(Print) =>
        // for print books, we can construct the book and return immediately
        Success(PrintBook(id, title, author))
      case Some(Digital) =>
        // for digital books we need to handle the download type
        row[Option[String]]("download_type") match {
          case None =>
            Failure(new AssertionError(s"download type not provided for digital book $id"))
          case Some(downloadStr) =>
            DownloadType.fromString(downloadStr) match {
              case None     => Failure(new ParseError(downloadStr))
              case Some(dt) => Success(EBook(id, title, author, dt))
            }
        }
    }
  }
}

}
