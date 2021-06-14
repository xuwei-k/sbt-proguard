package sbtproguard

import sbtproguard.proguard.Merge
import sbt._
import sbt.Keys._
import sbt.internal.inc.Analysis

import scala.sys.process.Process

object SbtProguard extends AutoPlugin {
  object autoImport extends ProguardKeys {
    lazy val Proguard: Configuration = config("proguard").hide
  }

  import autoImport._
  import ProguardOptions._

  override def requires: Plugins = plugins.JvmPlugin

  override def projectConfigurations: Seq[Configuration] = Seq(Proguard)

  override lazy val projectSettings: Seq[Setting[_]] = inConfig(Proguard)(baseSettings) ++ dependencies

  def baseSettings: Seq[Setting[_]] =
    Seq(
      proguardVersion := "7.0.1",
      proguardDirectory := crossTarget.value / "proguard",
      proguardConfiguration := proguardDirectory.value / "configuration.pro",
      artifactPath := proguardDirectory.value / (Compile / packageBin / artifactPath).value.getName,
      managedClasspath := Classpaths.managedJars(configuration.value, classpathTypes.value, update.value),
      proguardBinaryDeps := {
        val converter = fileConverter.value
        (Compile / compile).value match {
          case analysis: Analysis =>
            val javaRuntime = sys.props.get("java.home").map(home => file(home) / "lib/rt.jar").filter(_.isFile).toList
            javaRuntime ++ analysis.relations.allLibraryDeps.map(x => converter.toPath(x).toFile).toSeq
        }
      },
      proguardInputs := (Runtime / fullClasspath).value.files,
      proguardLibraries := proguardBinaryDeps.value filterNot proguardInputs.value.toSet,
      proguardOutputs := Seq(artifactPath.value),
      proguardDefaultInputFilter := Some("!META-INF/MANIFEST.MF"),
      proguardInputFilter := {
        val defaultInputFilterValue = proguardDefaultInputFilter.value
        _ => defaultInputFilterValue
      },
      proguardLibraryFilter := { f => None },
      proguardOutputFilter := { f => None },
      proguardFilteredInputs := filtered(proguardInputs.value, proguardInputFilter.value),
      proguardFilteredLibraries := filtered(proguardLibraries.value, proguardLibraryFilter.value),
      proguardFilteredLibraries ++= {
        // if new jdk
        // https://github.com/Guardsquare/proguard/blob/e8e749ce33076f5219a/docs/md/manual/configuration/examples.md#processing-different-types-of-applications--applicationtypes
        ProguardOptions.noFilter(
          sys.props
            .get("java.home")
            .map { home =>
              file(home) / "jmods/java.base.jmod"
            }
            .filter(_.isFile)
            .toList
        )
      },
      proguardFilteredOutputs := filtered(proguardOutputs.value, proguardOutputFilter.value),
      proguardMerge := false,
      proguardMergeDirectory := proguardDirectory.value / "merged",
      proguardMergeStrategies := ProguardMerge.defaultStrategies,
      proguardMergedInputs := mergeTask.value,
      proguardOptions := {
        jarOptions("-injars", proguardMergedInputs.value) ++
          jarOptions("-libraryjars", proguardFilteredLibraries.value) ++
          jarOptions("-outjars", proguardFilteredOutputs.value)
      },
      proguard / javaOptions := Seq("-Xmx256M"),
      autoImport.proguard := proguardTask.value
    )

  def dependencies: Seq[Setting[_]] =
    Seq(
      libraryDependencies += "com.guardsquare" % "proguard-base" % (Proguard / proguardVersion).value % Proguard
    )

  lazy val mergeTask: Def.Initialize[Task[Seq[ProguardOptions.Filtered]]] = Def.task {
    val streamsValue = streams.value
    val mergeDirectoryValue = proguardMergeDirectory.value
    val mergeStrategiesValue = proguardMergeStrategies.value
    val filteredInputsValue = proguardFilteredInputs.value
    if (proguardMerge.value) {
      val cachedMerge = FileFunction.cached(streamsValue.cacheDirectory / "proguard-merge", FilesInfo.hash) { _ =>
        streamsValue.log.info("Merging inputs before proguard...")
        IO.delete(mergeDirectoryValue)
        val inputs = filteredInputsValue map (_.file)
        Merge.merge(inputs, mergeDirectoryValue, mergeStrategiesValue.reverse, streamsValue.log)
        mergeDirectoryValue.allPaths.get.toSet
      }
      val inputs = inputFiles(filteredInputsValue).toSet
      cachedMerge(inputs)
      val filters = (filteredInputsValue flatMap (_.filter)).toSet
      val combinedFilter = if (filters.nonEmpty) Some(filters.mkString(",")) else None
      Seq(Filtered(mergeDirectoryValue, combinedFilter))
    } else filteredInputsValue
  }

  lazy val proguardTask: Def.Initialize[Task[Seq[File]]] = Def.task {
    writeConfiguration(proguardConfiguration.value, proguardOptions.value)
    val proguardConfigurationValue = proguardConfiguration.value
    val javaOptionsInProguardValue = (proguard / javaOptions).value
    val managedClasspathValue = managedClasspath.value
    val streamsValue = streams.value
    val outputsValue = proguardOutputs.value
    val cachedProguard = FileFunction.cached(streams.value.cacheDirectory / "proguard", FilesInfo.hash) { _ =>
      outputsValue foreach IO.delete
      streamsValue.log.debug("Proguard configuration:")
      proguardOptions.value foreach (streamsValue.log.debug(_))
      runProguard(proguardConfigurationValue, javaOptionsInProguardValue, managedClasspathValue.files, streamsValue.log)
      outputsValue.toSet
    }
    val inputs = (proguardConfiguration.value +: inputFiles(proguardFilteredInputs.value)).toSet
    cachedProguard(inputs)
    proguardOutputs.value
  }

  def inputFiles(inputs: Seq[Filtered]): Seq[File] =
    inputs flatMap { i => if (i.file.isDirectory) i.file.allPaths.get else Seq(i.file) }

  def writeConfiguration(config: File, options: Seq[String]): Unit =
    IO.writeLines(config, options)

  def runProguard(config: File, javaOptions: Seq[String], classpath: Seq[File], log: Logger): Unit = {
    require(classpath.nonEmpty, "Proguard classpath cannot be empty!")
    val options = javaOptions ++ Seq(
      "-cp",
      Path.makeString(classpath),
      "proguard.ProGuard",
      "-include",
      config.getAbsolutePath
    )
    log.debug("Proguard command:")
    log.debug("java " + options.mkString(" "))
    val exitCode = Process("java", options) ! log
    if (exitCode != 0) sys.error("Proguard failed with exit code [%s]" format exitCode)
  }
}
