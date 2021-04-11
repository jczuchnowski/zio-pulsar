val zioVersion = "1.0.5"

inThisBuild(
  List(
    name := "zio-pulsar",
    organization := "com.github.jczuchnowski",
    licenses := List("BSD 2-Clause" -> url("https://opensource.org/licenses/BSD-2-Clause")),
    developers := List(
      Developer("jczuchnowski", "Jakub Czuchnowski", "jakub.czuchnowski@gmail.com", url("https://github.com/jczuchnowski"))
    ),
    scalaVersion := "3.0.0-RC1"
  )
)

ThisBuild / publishTo := sonatypePublishToBundle.value

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val core = project
  .in(file("core"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"          %% "zio"               % zioVersion,
      "dev.zio"          %% "zio-streams"       % zioVersion,
      "org.apache.pulsar" % "pulsar-client"     % "2.7.1",
      "dev.zio"          %% "zio-test"          % zioVersion % Test,
      "dev.zio"          %% "zio-test-sbt"      % zioVersion % Test,
      "dev.zio"          %% "zio-test-junit"    % zioVersion % Test,
      "dev.zio"          %% "zio-test-magnolia" % zioVersion % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )

lazy val examples = project
  .in(file("examples"))
  .settings(
    skip in publish := true,
    moduleName := "examples",
    libraryDependencies ++= Seq(
      //"dev.zio" %% "zio-logging" % "0.5.6",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )
  .dependsOn(core)

lazy val root = project
  .in(file("."))
  .settings(
    skip in publish := true
  )
  .aggregate(
    core,
    examples
  )
