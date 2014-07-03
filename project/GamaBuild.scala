import sbt._
import Keys._
import com.typesafe.sbt.osgi.SbtOsgi.{OsgiKeys, osgiSettings}


object GamaBuild extends Build {


  val gamaSettings = Defaults.defaultSettings ++ osgiSettings ++ Seq(
    OsgiKeys.importPackage := Seq("*;resolution:=optional"),
    OsgiKeys.privatePackage := Seq("!scala.*"),
    resolvers += "ISC-PIF Release" at "http://maven.iscpif.fr/public/",
    libraryDependencies ++= Seq("org.openmole.core" %% "org-openmole-core-implementation" % "1.0-SNAPSHOT"),
    unmanagedBase := baseDirectory.value / "../lib",
    name := "openmole-gama",
    organization := "org.openmole",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.1"
  )


  lazy val core = Project(
    id = "openmole-core-gama",
    base = file("./org.openmole.plugin.task.gama/"),
    settings = gamaSettings ++ Seq(
      name := "task.gama",
      libraryDependencies += "org.openmole.core" %% "org-openmole-plugin-task-external" % "1.0-SNAPSHOT",
      OsgiKeys.exportPackage := Seq("org.openmole.plugin.task.gama.*")
    )
  )

  lazy val ide = Project(
    id = "openmole-ide-gama",
    base = file("./org.openmole.ide.plugin.task.gama"),
    settings = gamaSettings ++ Seq(
      name := "task-ide-gama",
      libraryDependencies += "org.openmole.core" %% "org-openmole-core-model" % "1.0-SNAPSHOT",
      libraryDependencies += "org.openmole.ide" %% "org-openmole-ide-core-implementation" % "1.0-SNAPSHOT",
      OsgiKeys.exportPackage := Seq("org.openmole.ide.plugin.task.gama.*","org.openmole.plugin.task.gama.*"),
      OsgiKeys.bundleActivator := Option("org.openmole.ide.plugin.task.gama.Activator")
    )
  ) dependsOn (core)

}
