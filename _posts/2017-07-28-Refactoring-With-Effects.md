---
layout: post
title: Refactoring with effects
---

I was recently cleaning up some Scala code I'd written a few months
ago when I realized I had been structuring code in a very confusing
way for a very long time. At work, we've been trying to combat the
inevitable spaghetti of code that gets written by different authors at
different times, as requirements inevitably change. We all know that
code should be made up of short, easily digestible functions but we
don't always get guidance on how to achieve that. In the presence of
error handling and nested data structures, the problem gets even
harder.

### How to break up code into smaller pieces
The goal of this blog post is to describe a concrete strategy for
structuring code so that the overall flow of control is clear to the
reader, even months later; and so the smaller pieces are both
digestible and testable. I'll start by giving an example function,
operating on some nested data types. Then I'll explore some ways to
break it into smaller pieces. The key insight is that we can use
computational effects in the form of
[monads](https://typelevel.org/cats/typeclasses/monad.html) (more
specifically,
[MonadError](https://typelevel.org/cats/api/cats/MonadError.html)) to
wrap smaller pieces and ultimately, put them back together.

Let's not worry about MonadError yet, but instead look at some example
code. Suppose we need to read an object from a relational database.
Unfortunately, rows in the table may represent objects of a variety of
types so we have to read the row and build up the object graph. This
is the boundary between the weakly typed wilderness and the strongly
typed world within our program.

Say our database table represents a library catalog, which might have
print books and ebooks. We'd like to grab an ebook and represent it
as such.

Here's a simple table

| id | title                  | author         | format | download_type |
|----|------------------------|----------------|--------|---------------|
| 45 | Programming In Haskell | Hutton, Graham | print  | null          |
| 46 | Programming In Haskell | Hutton, Graham | ebook  | epub          |
| 49 | Programming In Haskell | Hutton, Graham | ebook  | pdf           |

We can define a simple domain model:

```scala
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

sealed trait Book {
  def id: Int
  def title: String
  def author: String
  def format: Format
}

case class PrintBook(
    id: Int,
    title: String,
    author: String,
    downloadType: DownloadType
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

case class ParseError[T](str: String)
    extends Exception(s"$str is not a ${classOf[T].getSimpleName}")
```

We want to be able to define something akin to

```scala
def findBookById(id: Int): Option[Book] = ???
```

Well, actually, we'd like to do this without blocking threads, so
maybe that should look a little more like:

```scala
def findBookById(id: Int): Task[Book] = ???
```

One trivial definition might be:

```scala
def findBookById(id: Int): Try[Book] = {
  // assume queryUnique returns a `Try[Row]`
  DB.queryUnique(sql"""select * from catalog where id = $id""").flatMap { row =>
    // pick out the properties every book possesses
    val id        = row[Int]("id")
    val title     = row[String]("title")
    val author    = row[String]("author")
    val formatStr = row[String]("format")
     // now start to determine the types - get the format first
    Format.fromString(formatStr) match {
      case None =>
        Failure(new ParseError[Format](formatStr))
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
              case None     => Failure(new ParseError[DownloadType](downloadStr))
              case Some(dt) => Success(EBook(id, title, author, dt))
            }
        }
    }
  }
}
```

Depending on your perspective, that is arguably a long function. If
you think it is not so long, pretend that the table has a number of
other fields that must also be conditionally parsed to construct a
`Book`. Previously, I might have refactored this by a strategy I'm
going to call "tail-refactoring", for lack of a better description.
Basically, each function does a little work, or some error checking,
and then calls the next appropriate function in the chain.

You can imagine what kind of code will result. Functions are smaller,
but it's hard to describe what each function does, and functions
occasionally have to carry along additional parameters that they will
ignore except to pass deeper into the call chain. Let's take a look at
an example refactoring:

```scala
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
    case None => Failure(new ParseError[Format](formatStr))
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
          Failure(new ParseError[DownloadType](downloadTypeStr))
        case Some(dt) =>
          Success(EBook(id, title, author, dt))
      }
  }
```
