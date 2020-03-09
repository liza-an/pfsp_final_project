name := "streaming-ucu-final-project"

ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.12.8"

// These options will be used for *all* versions.
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding", "UTF-8",
  "-Xlint")

val akkaVersion = "2.5.20"

val commonDependencies = Seq(
  "org.apache.kafka" % "kafka-clients" % "2.4.0" withSources()
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("javax.jms", "jms")
    exclude("javax.ws.rs", "javax.ws.rs-api")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri"),
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.slf4j" % "slf4j-log4j12" % "1.7.25",
  "junit" % "junit" % "4.12" % Test,
  "io.spray" %% "spray-json" % "1.3.5"

)

val streamsDependencies = Seq(
  "org.apache.kafka" %% "kafka-streams-scala" % "2.4.0" withSources()
    exclude("com.fasterxml.jackson.core", "jackson-databind")
    exclude("com.fasterxml.jackson.datatype", "jackson-datatype-jdk8")
    exclude("javax.ws.rs", "javax.ws.rs-api"),
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1" artifacts Artifact("javax.ws.rs-api", "jar", "jar"),
  "org.apache.kafka" % "kafka-streams-test-utils" % "2.4.0" % Test
)

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.8" % Test
)

def dockerSettings(debugPort: Option[Int] = None) = Seq(
  dockerfile in docker := {
    val artifactSource: File = assembly.value
    val artifactTargetPath = s"/project/${artifactSource.name}"
    val scriptSourceDir = baseDirectory.value / "../scripts"
    val projectDir = "/project/"
    val conFile = baseDirectory.value / "../application.conf"
    new Dockerfile {
      from("anapsix/alpine-java:latest")
      add(artifactSource, artifactTargetPath)
      copy(scriptSourceDir, projectDir)
      copy(conFile, projectDir)
      run("chmod", "+x", s"/project/start.sh")
      entryPoint(s"/project/start.sh")
      cmd(projectDir, s"${name.value}", s"${version.value}")
    }
  },

  imageNames in docker := Seq(
    ImageName(
      registry = Some(sys.env("REGISTRY_URI")),
      namespace = Some("ucu-class"),
      repository = name.value,
      tag = Some(s"${sys.env("STUDENT_NAME")}-${version.value}")
    )
    //    , ImageName(s"rickerlyman/${name.value}:latest")
  )
)

envFileName in ThisBuild := ".env"

lazy val root = (project in file("."))
  .settings(name := "streaming-ucu-final-project")
  .aggregate(news_collector, tesla_stocks_collector, musk_tweets_collector, streaming_app)

lazy val news_collector = (project in file("news-collector"))
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    name := "news-collector",
    libraryDependencies ++= commonDependencies ++ akkaDependencies ++ Seq(
      "com.typesafe.play" %% "play-json" % "2.8.0"
    ),
    assemblyMergeStrategy in assembly := {
      case x if x.endsWith("module-info.class")  => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) =>
        (xs map {_.toLowerCase}) match {
          case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) => MergeStrategy.discard
          case _ => MergeStrategy.last
        }
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    dockerSettings()
  )

lazy val tesla_stocks_collector = (project in file("tesla-stocks-collector"))
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    name := "tesla-stocks-collector",
    libraryDependencies ++= commonDependencies ++ akkaDependencies ++ Seq(
    ),
    dockerSettings()
  )

lazy val musk_tweets_collector = (project in file("musk-tweets-collector"))
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    name := "musk-tweets-collector",
    libraryDependencies ++= commonDependencies ++ akkaDependencies ++ Seq(
    ),
    dockerSettings()
  )

lazy val streaming_app = (project in file("streaming-app"))
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    name := "streaming-app",
    libraryDependencies ++= commonDependencies ++ streamsDependencies ++ akkaDependencies ++ Seq(
      "com.typesafe.play" %% "play-json" % "2.8.0"
    ),
    assemblyMergeStrategy in assembly := {
      case x if x.endsWith("module-info.class")  => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) =>
        (xs map {_.toLowerCase}) match {
          case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) => MergeStrategy.discard
          case _ => MergeStrategy.last
        }
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    dockerSettings(),
    mainClass in assembly := Some("ua.ucu.edu.StreamingApp")
  )
