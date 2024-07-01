name := "lambda-typed-actor"
organization := "science.aethyric"
version := "0.1-SNAPSHOT"

scalaVersion := "2.12.8"

val circeVersion = "0.11.1"

val libraries = Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0"
  ,"com.amazonaws" % "aws-lambda-java-log4j2" % "1.0.0"
  ,"com.typesafe.akka" %% "akka-actor-typed" % "2.5.21"
  ,"com.jayway.jsonpath" % "json-path" % "2.4.0"
  ,"com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.8"

  ,"org.scalatest" %% "scalatest" % "3.0.5" % "test" 
)


assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

allDependencies ++= libraries
