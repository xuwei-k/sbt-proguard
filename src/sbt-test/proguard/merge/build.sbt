enablePlugins(SbtProguard)

Proguard / proguardOptions += "-dontoptimize"

Proguard / proguardOptions ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

Proguard / proguardOptions += ProguardOptions.keepMain("Test")

Proguard / proguardMerge := true

Proguard / proguardMergeStrategies += ProguardMerge.discard("META-INF/.*".r)
