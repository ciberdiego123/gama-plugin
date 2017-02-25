import sbt._
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import sbt.Keys._
import sbt.dependency.manager._
import sbt.osgi.manager._
import sbt.dependency.manager.Keys._
import com.typesafe.sbt.osgi.{OsgiKeys, SbtOsgi}
import sbt.IO._

object GamaBuild extends Build {

//  resolvers in OSGiConf += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

  lazy val openmoleVersion = "6.1"

  val osgiSettings = Seq(
    OsgiKeys.importPackage := Seq("*"),
    OsgiKeys.privatePackage := Seq("!scala.*"),
    OsgiKeys.requireCapability := """osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.7))""",
    OsgiKeys.exportPackage := Seq("org.openmole.plugin.task.gama.*"),
    OsgiKeys.bundleActivator := Some("org.openmole.plugin.task.gama.Activator"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    unmanagedBase := baseDirectory.value / "../bundles",
    organization := "org.openmole",
    updateOptions := updateOptions.value.withCachedResolution(true),
    name := "openmole-gama"
  )
  val deleteTaskGama = taskKey[Unit]("delete task gama jar")
  val deleteTaskOsgi = taskKey[Unit]("delete tasg osgi jar")
  val generateTask = taskKey[Unit]("compile all gama bundle")

    lazy val core = Project(
      id = "openmole-gama",
      base = file("./org.openmole.plugin.task.gama/")) enablePlugins(SbtOsgi) settings(osgiSettings ++ DependencyManager ++ OSGiManagerWithDebug(): _*) settings(
      name := "task.gama",
      scalaVersion := "2.11.8",
      version := "6.0-SNAPSHOT",
      addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full),
      DMKey.dependencyFilter in DMConf := Some(sbt.DependencyFilter.fnToModuleFilter{m =>
        println(m.name, " > "  , m.configurations == Some("osgi") &&  m.organization != "org.eclipse.osgi")
        (m.configurations == Some("osgi") && m.organization != "org.eclipse.osgi")}),
      DMKey.dependencyOutput in DMConf := Some(baseDirectory.value / "../bundles"),
      resolvers in OSGiConf += typeP2("Eclipse Mars p2 update site" at "http://download.eclipse.org/releases/mars/"),
      resolvers in OSGiConf += typeP2("GAMA update site" at "http://gama.unthinkingdepths.fr/"),
      libraryDependencies in OSGiConf += typeP2(OSGi.ECLIPSE_PLUGIN % "msi.gama.headless" % OSGi.ANY_VERSION withSources),
      //libraryDependencies in OSGiConf += typeP2(OSGi.ECLIPSE_PLUGIN % "org.eclipse.ui" % OSGi.ANY_VERSION withSources),
      libraryDependencies += "biz.aQute" % "bndlib" % "2.0.0.20130123-133441",
      libraryDependencies ++= Seq(
        "com.github.julien-truffaut"  %%  "monocle-core"    % "1.3.2",
        "com.github.julien-truffaut"  %%  "monocle-generic" % "1.3.2",
        "com.github.julien-truffaut"  %%  "monocle-macro"   % "1.3.2",
        "com.github.julien-truffaut"  %%  "monocle-state"   % "1.3.2",
        "com.github.julien-truffaut"  %%  "monocle-refined" % "1.3.2",
        "com.github.julien-truffaut"  %%  "monocle-unsafe"  % "1.3.2",
        "com.github.julien-truffaut"  %%  "monocle-law"     % "1.3.2" % "test"
      ),
      libraryDependencies +=  "com.google.code.findbugs" % "jsr305" % "2.0.3" % "test",
      libraryDependencies += "org.openmole" %% "org-openmole-core-dsl" % openmoleVersion % "provided",
      libraryDependencies += "org.openmole" %% "org-openmole-plugin-task-external" % openmoleVersion % "provided",
      artifactPath in (Compile, packageBin) ~= { defaultPath =>
        file("bundles") / defaultPath.getName },
        cleanFiles ++= (((baseDirectory.value / "../bundles" ) * "*.jar") get),
        onLoad in Global  :=  {
        ((s: State) => { "osgiResolveRemote" :: s }) compose (onLoad in Global).value },
      deleteTaskGama := delete(((baseDirectory.value / "../bundles" ) * "task-gama*") get),
      deleteTaskOsgi := delete(((baseDirectory.value / "../bundles" ) * "org.eclipse.osgi*") get),
        generateTask in Compile :=  Def.sequential(
          deleteTaskGama,
          dependencyTaskFetch,
          compile in Compile,
          OsgiKeys.bundle,
          deleteTaskOsgi
        ).value


  )

   override def rootProject = Some(core)



}
