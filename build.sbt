lazy val circeVersion = "0.13.0"
lazy val akkaVersion    = "2.6.5"
lazy val akkaHttpVersion = "10.1.12"

lazy val `reactive-monitoring` = project
  .in(file("."))
  .settings(
    organization := "tr.edu.ege",
    scalaVersion := "2.13.1",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.3",
      "org.apache.kafka" %% "kafka" % "2.5.0",
      "com.github.scredis" %% "scredis" % "2.3.3",
      "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.3-akka-2.6.x",
      "com.ning" % "async-http-client" % "1.9.40",
      "org.jsoup" % "jsoup" % "1.8.1",
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "commons-io" % "commons-io" % "2.6" % Test,
    ),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
    fork in run := true,

    // disable parallel tests
    parallelExecution in Test := false,
  )