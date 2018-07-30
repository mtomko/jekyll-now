name := "mtomko-blog-examples"

version := "0.1"

scalaVersion := "2.12.6"

scalacOptions += "-Ypartial-unification"
scalacOptions += "-language:higherKinds"
scalacOptions += "-feature"
scalacOptions += "-Xlint"

libraryDependencies += "org.typelevel" %% "cats-core" % "1.2.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "1.0.0-RC2"
libraryDependencies += "io.monix" %% "monix" % "3.0.0-RC1"
