package refactoring.effects

import cats.effect.IO
import cats.implicits._
import monix.eval.Task

import scala.util.Try

object EffectsMonadMTest {

  val y = EffectsMonadM.findBookById[Try](5)

  val q = EffectsMonadM.findBookById[IO](5)

  val s = EffectsMonadM.findBookById[Task](5)

}
