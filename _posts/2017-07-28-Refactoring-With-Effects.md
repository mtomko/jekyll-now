## Refactoring with effects

I was recently cleaning up some Scala code I'd written a few months ago when I realized I had been structuring code in a very
confusing way for a very long time. At work, we've been trying to combat the inevitable spaghetti of code that gets written by
different authors at different times, as requirements inevitably change. We all know that code should be made up of short,
easily digestible functions but we don't always get guidance on how to achieve that. In the presence of error handling and
nested data structures, the problem gets even harder.

### How to break up code into smaller pieces
The goal of this blog post is to describe a concrete strategy for structuring code so that the overall flow of control is 
clear to the reader, even months later; and so the smaller pieces are both digestible and testable. I'll start by giving an
example function, operating on some nested data types. Then I'll explore some ways to break it into smaller pieces. The key
insight is that we can use computational effects in the form of [monads](https://typelevel.org/cats/typeclasses/monad.html)
(more specifically, [MonadError](https://typelevel.org/cats/api/cats/MonadError.html)) to wrap smaller pieces and ultimately,
put them back together.

Let's not worry about MonadError yet, but instead look at some example code:

```scala

```

