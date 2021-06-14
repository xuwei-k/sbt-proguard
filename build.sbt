organization := "com.github.xuwei-k"
name := "sbt-proguard"

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
