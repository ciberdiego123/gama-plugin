import sbt._
import Keys._
import com.typesafe.sbt.osgi.{OsgiKeys, SbtOsgi}
import com.typesafe.sbt.SbtScalariform.scalariformSettings

object GamaBuild extends Build {

  lazy val openmoleVersion = "6.0-SNAPSHOT"

  val gamaSettings = scalariformSettings ++ Seq(
    OsgiKeys.importPackage := Seq("*"),
    OsgiKeys.privatePackage := Seq("!scala.*"),
    OsgiKeys.requireCapability := """osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.7))""",
    //resolvers += "ISC-PIF Release" at "http://maven.iscpif.fr/public/",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    unmanagedBase := baseDirectory.value / "../lib",
    name := "openmole-gama",
    organization := "org.openmole",
    version := "6.0-SNAPSHOT",
    scalaVersion := "2.11.8",
    addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)
  )

  lazy val core = Project(
    id = "openmole-gama",
    base = file("./org.openmole.plugin.task.gama/")) enablePlugins(SbtOsgi) settings(gamaSettings: _*) settings(
      name := "task.gama",
      libraryDependencies += "org.openmole" %% "org-openmole-core-dsl" % openmoleVersion % "provided",
      libraryDependencies += "org.openmole" %% "org-openmole-plugin-task-external" % openmoleVersion % "provided",
      OsgiKeys.exportPackage := Seq("org.openmole.plugin.task.gama.*"),
      OsgiKeys.bundleActivator := Some("org.openmole.plugin.task.gama.Activator")
  )

}
