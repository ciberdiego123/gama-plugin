
name := "openmole-gama"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

resolvers += "ISC-PIF Release" at "http://maven.iscpif.fr/public/"

libraryDependencies += "org.openmole.core" %% "org-openmole-core-implementation" % "0.11.0-SNAPSHOT"

osgiSettings

OsgiKeys.exportPackage := Seq("org.openmole.plugin.task.gama.*")

OsgiKeys.importPackage := Seq("*;resolution:=optional")

OsgiKeys.privatePackage := Seq("!scala.*")


//unmanagedResourceDirectories in Global <<= baseDirectory { base => Seq(base / "lib/msi.gama.ext_1.0.0.jar") }

