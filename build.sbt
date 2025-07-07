ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.0"

lazy val root = (project in file("."))
  .settings(
    name := "storch-tenssorboard"
  )

//libraryDependencies ++= Seq(
// https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
libraryDependencies += "com.google.protobuf" % "protobuf-java" % "4.31.1"
// https://mvnrepository.com/artifact/org.tensorflow/proto
libraryDependencies += "org.tensorflow" % "proto" % "1.15.0"
// https://mvnrepository.com/artifact/net.mikera/core.matrix
libraryDependencies += "net.mikera" % "core.matrix" % "0.63.0"
// https://mvnrepository.com/artifact/com.rpl/specter
libraryDependencies += "com.rpl" % "specter" % "1.1.4"
//  "org.clojars.ghaskins" % "protobuf" % "3.1.0-1",
//  "com.google.protobuf" % "protobuf-java" % "3.1.0",
//  "org.tensorflow" % "proto" % "1.2.1",
//  "net.mikera" % "core.matrix" % "0.60.3",
//  "com.rpl" % "specter" % "1.0.1"
//)
//
//javacOptions ++= Seq("-Xmaxerrs", "1000")
//
//javadocOptions ++= Seq("-subpackages", "org.tensorflow.framework:org.tensorflow.util")
//
//resolvers += "Clojars Releases" at "https://clojars.org/repo"
//
//addSbtPlugin("com.github.os72" % "sbt-protobuf" % "0.1.1")
//addSbtPlugin("org.xerial.sbt" % "sbt-javadoc" % "0.3.0")