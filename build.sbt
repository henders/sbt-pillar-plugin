sbtPlugin := true

coverageHighlighting := false

name := "sbt-pillar"
organization := "io.github.henders"
description := "A wrapper over the Pillar library to manage Cassandra migrations (https://github.com/comeara/pillar)"
homepage := Some(url("https://github.com/henders/sbt-pillar-plugin"))

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
publishMavenStyle := false
bintrayRepository := "sbt-plugins"
bintrayOrganization := None
bintrayPackageLabels := Seq("sbt", "sbt-plugin", "pillar")
publishArtifact in Test := false
pomIncludeRepository := { _ => false}
scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "de.kaufhof" %% "pillar" % "4.1.2",
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0",
  "org.scalactic" %% "scalactic" % "3.0.6",
  "org.scalatest" %% "scalatest" % "3.0.6" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "com.typesafe" % "config" % "1.3.0"
)

pomExtra :=
  <scm>
    <url>https://github.com/henders/sbt-pillar-plugin</url>
    <connection>scm:git:git@github.com:henders/sbt-pillar-plugin.git</connection>
    <developerConnection>scm:git:https://github.com/henders/sbt-pillar-plugin.git</developerConnection>
  </scm>
    <developers>
      <developer>
        <id>henders</id>
        <name>Shane Hender</name>
        <email>henders [at] gmail.com</email>
        <url>https://henders.github.io</url>
      </developer>
    </developers>

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
