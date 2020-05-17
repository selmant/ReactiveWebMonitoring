import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings

val akkaVersion = "2.5.26"
val circeVersion = "0.11.1"
val akkaHttpVersion = "10.1.11"

lazy val `akka-sample-sharding-scala` = project
  .in(file("."))
  .settings(multiJvmSettings: _*)
  .settings(
    organization := "com.typesafe.akka.samples",
    scalaVersion := "2.12.8",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "org.iq80.leveldb" % "leveldb" % "0.7",
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
      "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.1-akka-2.5.x", // For Akka 2.5.x and Scala 2.11.x, 2.12.x, 2.13.x
      "com.ning" % "async-http-client" % "1.9.40",
      "org.jsoup" % "jsoup" % "1.8.1",
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "ch.qos.logback" % "logback-classic" % "1.1.4",
      "org.scalatest" %% "scalatest" % "3.0.7" % Test,
      "commons-io" % "commons-io" % "2.6" % Test,
      "io.kamon" % "sigar-loader" % "1.6.6-rev002",
      "com.github.scredis" %% "scredis" % "2.3.3",
      "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.5",
      "org.apache.kafka" %% "kafka" % "2.3.0",
      "com.typesafe.play" %% "play-json" % "2.7.4",
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime",
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
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
  .configs(MultiJvm)
