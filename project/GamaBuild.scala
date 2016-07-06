import sbt._
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import sbt.Keys._
import sbt.dependency.manager._
import sbt.osgi.manager._
import sbt.dependency.manager.Keys._
import com.typesafe.sbt.osgi.{OsgiKeys, SbtOsgi}

object GamaBuild extends Build {

//  resolvers in OSGiConf += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

  lazy val openmoleVersion = "6.0-SNAPSHOT"

  val osgiSettings = Seq(
    OsgiKeys.importPackage := Seq("*"),
    OsgiKeys.privatePackage := Seq("!scala.*"),
    OsgiKeys.requireCapability := """osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.7))""",
    OsgiKeys.exportPackage := Seq("org.openmole.plugin.task.gama.*"),
    OsgiKeys.bundleActivator := Some("org.openmole.plugin.task.gama.Activator"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    unmanagedBase := baseDirectory.value / "../bundles",
    organization := "org.openmole",
    name := "openmole-gama"
  )

  val generateTask = taskKey[Unit]("compile all gama bundle")

    lazy val core = Project(
      id = "openmole-gama",
      base = file("./org.openmole.plugin.task.gama/")) enablePlugins(SbtOsgi) settings(osgiSettings ++ DependencyManager ++ OSGiManagerWithDebug(): _*) settings( //enablePlugins(SbtOsgi) settings(gamaSettings ++ DependencyManager ++ OSGiManagerWithDebug(): _*) settings(
      name := "task.gama",
      scalaVersion := "2.11.8",
      version := "6.0-SNAPSHOT",
      addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full),
      DMKey.dependencyFilter in DMConf := Some(sbt.DependencyFilter.fnToModuleFilter{m => m.configurations == Some("osgi")}),
      DMKey.dependencyOutput in DMConf := Some(baseDirectory.value / "../bundles"),
      resolvers in OSGiConf += typeP2("Eclipse Mars p2 update site" at "http://download.eclipse.org/releases/mars/"),
      resolvers in OSGiConf += typeP2("GAMA update site" at "http://vps226121.ovh.net/updates/"),
      libraryDependencies in OSGiConf += typeP2(OSGi.ECLIPSE_PLUGIN % "msi.gama.headless" % OSGi.ANY_VERSION withSources),
      //libraryDependencies in OSGiConf += typeP2(OSGi.ECLIPSE_PLUGIN % "org.eclipse.ui" % OSGi.ANY_VERSION withSources),
      libraryDependencies += "biz.aQute" % "bndlib" % "2.0.0.20130123-133441",
      libraryDependencies +=  "com.google.code.findbugs" % "jsr305" % "2.0.3" % "test",
      libraryDependencies += "org.openmole" %% "org-openmole-core-dsl" % openmoleVersion % "provided",
      libraryDependencies += "org.openmole" %% "org-openmole-plugin-task-external" % openmoleVersion % "provided",
      artifactPath in (Compile, packageBin) ~= { defaultPath =>
        file("bundles") / defaultPath.getName },
      onLoad in Global :=  {
        ((s: State) => { "osgiResolveRemote" :: s }) compose (onLoad in Global).value },
//        bundleTask in Compile := {
//          val result  = SbtOsgi.autoImport.OsgiKeys.bundle.value
//          result
//        },
        generateTask in Compile :=  Def.sequential(
          //https://github.com/sbt-android-mill/sbt-dependency-manager/blob/master/src/main/scala/sbt/dependency/manager/Keys.scala
          dependencyTaskFetch,
          compile in Compile,
          OsgiKeys.bundle
        ).value

  )

  override def rootProject = Some(core)



}
