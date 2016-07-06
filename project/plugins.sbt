
resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "oss sonatype" at "https://oss.sonatype.org/content/groups/public/",
  "digimead-maven" at "http://commondatastorage.googleapis.com/maven.repository.digimead.org/",
  "Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.8.0")
addSbtPlugin("org.digimead" % "sbt-dependency-manager" % "0.8.0.2-SNAPSHOT")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
addSbtPlugin("org.digimead" % "sbt-osgi-manager" % "0.4.1.5")

