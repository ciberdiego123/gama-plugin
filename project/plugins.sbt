resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.7.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.5.1")