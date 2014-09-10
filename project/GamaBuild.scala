import sbt._
import Keys._
import com.typesafe.sbt.osgi.SbtOsgi.{OsgiKeys, osgiSettings}


object GamaBuild extends Build {

  lazy val openmoleVersion = "3.0-SNAPSHOT"

  val gamaSettings = Defaults.defaultSettings ++ osgiSettings ++ Seq(
    OsgiKeys.importPackage := Seq("*"),
    OsgiKeys.privatePackage := Seq("!scala.*"),
    resolvers += "ISC-PIF Release" at "http://maven.iscpif.fr/public/",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq("org.openmole.core" %% "org-openmole-core-implementation" % openmoleVersion),
    unmanagedBase := baseDirectory.value / "../lib",
    name := "openmole-gama",
    organization := "org.openmole",
    version := "2.0-SNAPSHOT",
    scalaVersion := "2.11.2"
  )

  lazy val core = Project(
    id = "openmole-gama",
    base = file("./org.openmole.plugin.task.gama/"),
    settings = gamaSettings ++ Seq(
      name := "task.gama",
      libraryDependencies += "org.openmole.core" %% "org-openmole-plugin-task-external" % openmoleVersion,
      OsgiKeys.exportPackage := Seq("org.openmole.plugin.task.gama.*")
    )
  )

  lazy val ide = Project(
    id = "openmole-ide-gama",
    base = file("./org.openmole.ide.plugin.task.gama"),
    settings = gamaSettings ++ Seq(
      name := "task-ide-gama",
      libraryDependencies += "org.openmole.core" %% "org-openmole-core-model" % openmoleVersion,
      libraryDependencies += "org.openmole.ide" %% "org-openmole-ide-core-implementation" % openmoleVersion,
      OsgiKeys.exportPackage := Seq("org.openmole.ide.plugin.task.gama.*"),
      OsgiKeys.bundleActivator := Option("org.openmole.ide.plugin.task.gama.Activator")
    )
  ) dependsOn (core)

}
