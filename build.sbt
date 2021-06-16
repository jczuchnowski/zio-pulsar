val zioVersion = "1.0.8"

inThisBuild(
  List(
    organization := "com.github.jczuchnowski",
    homepage := Some(url("https://github.com/jczuchnowski/zio-pulsar/")),
    licenses := List("BSD 2-Clause" -> url("https://opensource.org/licenses/BSD-2-Clause")),
    developers := List(
      Developer("jczuchnowski", "Jakub Czuchnowski", "jakub.czuchnowski@gmail.com", url("https://github.com/jczuchnowski"))
    ),
    scalaVersion := "3.0.0"
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val core = project
  .in(file("core"))
  .settings(
    name := "zio-pulsar",
    libraryDependencies ++= Seq(
      "dev.zio"          %% "zio"               % zioVersion % Provided,
      "dev.zio"          %% "zio-streams"       % zioVersion % Provided,
      "org.apache.pulsar" % "pulsar-client"     % "2.8.0",
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
    publish / skip := true,
    moduleName := "examples",
    libraryDependencies ++= Seq(
      //"dev.zio" %% "zio-logging" % "0.5.6",
      "dev.zio"        %% "zio"            % zioVersion,
      "dev.zio"        %% "zio-streams"    % zioVersion,
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
