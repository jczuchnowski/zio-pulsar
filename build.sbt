val zioVersion = "1.0.3"

lazy val root = project
  .in(file("."))
  .settings(
    inThisBuild(
      List(
        name := "zio-pulsar",
        organization := "zio.pulsar",
        version := "0.0.1",
        scalaVersion := "2.13.4"
      )
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      "dev.zio"           %% "zio"               % zioVersion,
      "org.apache.pulsar" % "pulsar-client"      % "2.6.2",
      "dev.zio"           %% "zio-test"          % zioVersion % Test,
      "dev.zio"           %% "zio-test-sbt"      % zioVersion % Test,
      "dev.zio"           %% "zio-test-junit"    % zioVersion % Test,
      "dev.zio"           %% "zio-test-magnolia" % zioVersion % Test
    ),
    libraryDependencies := libraryDependencies.value.map(_.withDottyCompat(scalaVersion.value)),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
