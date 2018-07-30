package refactoring.effects

case class Sql(s: String) extends AnyVal

sealed trait Format
case object Print extends Format
case object Digital extends Format
object Format {
  def fromString(s: String): Option[Format] = ???
}

sealed trait DownloadType
case object Epub extends DownloadType
case object Pdf extends DownloadType
object DownloadType {
  def fromString(s: String): Option[DownloadType] = ???
}

sealed trait Book extends Product with Serializable {
  def id: Int
  def title: String
  def author: String
  def format: Format
}

case class PrintBook(
    id: Int,
    title: String,
    author: String,
) extends Book {
  override val format: Format = Print
}

case class EBook(
    id: Int,
    title: String,
    author: String,
    downloadType: DownloadType
) extends Book {
  override val format: Format = Digital
}

case class ParseError(str: String) extends Exception(str)
