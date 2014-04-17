name := "consumer-example"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "com.kjetland" %% "ddsl-play2" % "1.3"
)

resolvers += (
  "mbknorGithubRepoUrl" at "http://mbknor.github.com/m2repo/releases/"
)

play.Project.playJavaSettings
