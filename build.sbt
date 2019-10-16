sbtPlugin := true

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

publishTo := sonatypePublishTo.value

// Don't update to sbt 1.3.x
// https://github.com/sbt/sbt/issues/5049
crossSbtVersions ++= Seq("1.2.8")

scriptedSettings
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
  releaseStepCommand("sonatypeReleaseAll"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
