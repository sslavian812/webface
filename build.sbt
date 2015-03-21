name := "webface"

version := "1.0"

lazy val `webface` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq( jdbc , anorm , cache , ws )

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.11"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  