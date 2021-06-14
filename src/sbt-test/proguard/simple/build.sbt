enablePlugins(SbtProguard)

Proguard / proguardOptions += "-dontoptimize"

Proguard / proguardOptions ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

Proguard / proguardOptions += ProguardOptions.keepMain("Test")

(Proguard / proguardInputs) := (Compile / dependencyClasspath).value.files

(Proguard / proguardFilteredInputs) ++= ProguardOptions.noFilter((Compile / packageBin).value)
