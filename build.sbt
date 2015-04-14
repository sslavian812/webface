name := "webface"

version := "1.0"

lazy val `webface` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

libraryDependencies ++= Seq( jdbc , anorm , cache , ws )

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.11"

libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "1.1.4"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.1"


libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "1.1.1"

libraryDependencies += "org.elasticsearch" % "elasticsearch" % "1.4.4"

libraryDependencies += "org.apache.spark" % "spark-graphx_2.10" % "1.1.1"

libraryDependencies += "org.specs" % "specs" % "1.2.5"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  