ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "laundry-booker",
    libraryDependencies ++=
      Seq("org.typelevel" %% "cats-effect" % "3.3.14",
          "org.scalatest" %% "scalatest" % "3.2.14" % "test",
          "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.11" % "test",
          "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.40.11" % "test",
          "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
          "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1"
      )
)
