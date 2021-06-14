enablePlugins(SbtProguard)

Proguard / proguardOptions += "-dontoptimize"

Proguard / proguardOptions ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

Proguard / proguardOptions += ProguardOptions.keepMain("Test")

Proguard / proguardInputFilter := { file =>
  file.name match {
    case "scala-library.jar" => Some("!META-INF/**")
    case _                   => None
  }
}
