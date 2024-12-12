organization := "com.github.xuwei-k"
name := "sbt-proguard"

// for scala-steward
val proguardBase = "com.guardsquare" % "proguard-base" % "7.6.1" % "runtime"
libraryDependencies += proguardBase

Compile / sourceGenerators += task {
  val source = s"""package sbtproguard
    |
    |private[sbtproguard] object SbtProGuardBuildInfo {
    |  def defaultProGuardVersion: String = "${proguardBase.revision}"
    |}
    |""".stripMargin
  val f = (Compile / sourceManaged).value / "sbtproguard" / "SbtProGuardBuildInfo.scala"
  IO.write(f, source)
  Seq(f)
}

pomPostProcess := { node =>
  import scala.xml.{NodeSeq, Node}
  val rule = new scala.xml.transform.RewriteRule {
    override def transform(n: Node) = {
      if (
        List(
          n.label == "dependency",
          (n \ "groupId").text == proguardBase.organization,
          (n \ "artifactId").text == proguardBase.name,
        ).forall(identity)
      ) {
        NodeSeq.Empty
      } else {
        n
      }
    }
  }
  new scala.xml.transform.RuleTransformer(rule).transform(node)(0)
}

packagedArtifacts := {
  val value = packagedArtifacts.value
  val pomFiles = value.values.filter(_.getName.endsWith(".pom")).toList
  assert(pomFiles.size == 2, pomFiles.map(_.getName))
  pomFiles.foreach { f =>
    assert(!IO.read(f).contains("proguard-base"))
  }
  value
}

homepage := Some(url("https://github.com/xuwei-k/sbt-proguard"))
licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/xuwei-k/sbt-proguard"),
    "scm:git@github.com:xuwei-k/sbt-proguard.git"
  )
)

pomExtra := {
  <developers>
    <developer>
      <id>xuwei-k</id>
      <name>Kenji Yoshida</name>
      <url>https://github.com/xuwei-k</url>
    </developer>
  </developers>
}

publishTo := sonatypePublishToBundle.value

enablePlugins(SbtPlugin)
scriptedDependencies := publishLocal.value
scriptedLaunchOpts ++= Seq("-Xms512m", "-Xmx512m", s"-Dproject.version=${version.value}")
scriptedBufferLog := false

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

sbtPluginPublishLegacyMavenStyle := {
  sys.env.isDefinedAt("GITHUB_ACTION") || isSnapshot.value
}
