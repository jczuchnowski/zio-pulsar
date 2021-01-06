val zioVersion = "1.0.3"

inThisBuild(
  List(
    name := "zio-pulsar",
    organization := "zio.pulsar",
    version := "0.0.1",
    scalaVersion := "2.13.4"
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val core = project
  .in(file("core"))
  .settings(
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      "dev.zio"           %% "zio"               % zioVersion,
      "dev.zio"           %% "zio-streams"       % zioVersion,
      "org.apache.pulsar" %  "pulsar-client"     % "2.6.2",
      "dev.zio"           %% "zio-test"          % zioVersion % Test,
      "dev.zio"           %% "zio-test-sbt"      % zioVersion % Test,
      "dev.zio"           %% "zio-test-junit"    % zioVersion % Test,
      "dev.zio"           %% "zio-test-magnolia" % zioVersion % Test
    ),
    libraryDependencies := libraryDependencies.value.map(_.withDottyCompat(scalaVersion.value)),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )

lazy val examples = project
  .in(file("examples"))
  .settings(
    skip in publish := true,
    moduleName := "examples",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-logging" % "0.5.4",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
    )
  )
  .dependsOn(core)