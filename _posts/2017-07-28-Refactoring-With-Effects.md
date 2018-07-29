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
case object Ebook extends Format
object Format {
  def fromString(s: String): Option[Format] = ???
}

seaed trait DownloadType
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
  override val format: Format = EBook
}

case class ParseError[T](str: String)
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
def findBookById(id: Int): Task[Book] = {
  Task {
    DB.query(sql"""select * from catalog where id = $id""").map { row =>
      val id = row[Int]("id")
      val title = row[String]("title")
      val author = row[String]("author")
      val formatStr = row[String]("format")
      Format.fromString(formatStr) match {
        case None => Task.raiseError(new ParseError[Format](formatStr))
        case Some(PrintBook) => 
          PrintBook(id, title, author)
        case Some(EBook) =>
          val downloadTypeStr = row[Option[String]]("download_type")
          DownloadType.fromString(downloadTypeStr) match {
            case None => Task.raiseError(new ParseError[DownloadType](downloadStr))
            case Some(dt) =>
              EBook(id, title, author, dt)
          }
      }
    }
  }
}
```

That is a long function. Previously, I'd have refactored this by a
strategy I'm going to call "recursive descent", breaking out to a
new function every time the problem I was trying to solve changed:

```scala
def findBookById(id: Int): Task[Book] =
  Task {
    DB.query(sql"""select * from catalog where id = $id""").map { row =>
      val id = row[Int]("id")
      val title = row[String]("title")
      val author = row[String]("author")
      val formatStr = row[String]("format")
      val downloadTypeStr = row[Option[String]]("download_type")
      extractBook(id, title, author, formatStr, downloadTypeStr)
    }
  }

def extractBook(id: Int, title: String, author: String, formatStr: String, downloadTypeStr: String): Task[Book] = 
    Format.fromString(formatStr) match {
      case None => Task.raiseError(new ParseError[Format](formatStr))
      case Some(PrintBook) => 
          PrintBook(id, title, author)
        case Some(EBook) =>

          DownloadType.fromString(downloadTypeStr) match {
            case None => Task.raiseError(new ParseError[DownloadType](downloadStr))
            case Some(dt) =>
              EBook(id, title, author, dt)
          }
      }
    }
  }
}

```
