val zioVersion = "1.0.6"

inThisBuild(
  List(
    organization := "com.github.jczuchnowski",
    licenses := List("BSD 2-Clause" -> url("https://opensource.org/licenses/BSD-2-Clause")),
    developers := List(
      Developer("jczuchnowski", "Jakub Czuchnowski", "jakub.czuchnowski@gmail.com", url("https://github.com/jczuchnowski"))
    ),
    scalaVersion := "3.0.0-RC2"
  )
)

ThisBuild / publishTo := sonatypePublishToBundle.value

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val core = project
  .in(file("core"))
  .settings(
    name := "zio-pulsar",
    libraryDependencies ++= Seq(
      "dev.zio"           % "zio_2.13"               % zioVersion % Provided,
      "dev.zio"           % "zio-streams_2.13"       % zioVersion % Provided,
      "dev.zio"           % "zio-json_2.13"          % "0.1.4"  % Provided,
      "org.apache.pulsar" % "pulsar-client"          % "2.7.1",
      "dev.zio"           % "zio-test_2.13"          % zioVersion % Test,
      "dev.zio"           % "zio-test-sbt_2.13"      % zioVersion % Test,
      "dev.zio"           % "zio-test-junit_2.13"    % zioVersion % Test,
      "dev.zio"           % "zio-test-magnolia_2.13" % zioVersion % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )

lazy val examples = project
  .in(file("examples"))
  .settings(
    publish / skip := true,
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
    publish / skip := true
  )
  .aggregate(
    core,
    examples
  )
