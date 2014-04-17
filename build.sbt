import _root_.java.io.File

name := "ddsl-play2"

organization := "com.kjetland"

version := "1.3"

publishTo := Some(Resolver.file("http://jschwartz73.github.io/ddsl-play2",
  new File("/Users/jeff/dev/myprojects/ddsl-play2.github.com"))
)

libraryDependencies ++= Seq(
  "com.kjetland" %% "ddsl" % "0.3.3"
)

resolvers += (
  "mbknorGithubRepoUrl" at "http://mbknor.github.com/m2repo/releases/"
)

play.Project.playScalaSettings
