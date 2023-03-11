import sbt._
import scala.sys.process._
import java.io.File


ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val nativeImageOpts = Seq(
  "--initialize-at-run-time=io.netty.handler.ssl.BouncyCastleAlpnSslUtils",
  "--initialize-at-run-time=io.netty.handler.codec.compression.ZstdOptions",

  "--initialize-at-run-time=io.netty.channel.DefaultFileRegion",
  "--initialize-at-run-time=io.netty.channel.epoll.Native",
  "--initialize-at-run-time=io.netty.channel.epoll.Epoll",
  "--initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop",
  "--initialize-at-run-time=io.netty.channel.epoll.EpollEventArray",
  "--initialize-at-run-time=io.netty.channel.kqueue.KQueue",
  "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop",
  "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventArray",
  "--initialize-at-run-time=io.netty.channel.kqueue.Native",
  "--initialize-at-run-time=io.netty.channel.unix.Limits",
  "--initialize-at-run-time=io.netty.channel.unix.Errors",
  "--initialize-at-run-time=io.netty.channel.unix.IovArray",

  "--initialize-at-run-time=io.netty.incubator.channel.uring.IOUringEventLoopGroup",
  "--initialize-at-run-time=io.netty.incubator.channel.uring.Native",
  "--initialize-at-run-time=io.netty.incubator.channel.uring.IOUring",
)


lazy val root = (project in file("."))
  .enablePlugins(
    SbtNativePackager,
    GraalVMNativeImagePlugin,
  )
  .settings(
    name := "zioquickstart",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.10",
      "dev.zio" %% "zio-http" % "0.0.4",
      "dev.zio" %% "zio-test" % "2.0.10" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    assembly / assemblyMergeStrategy := {
      case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
  .settings(
    Compile / mainClass := Some("com.example.HelloWorld"),
  )


lazy val nativeImage =
  taskKey[File]("Build a standalone executable using GraalVM Native Image")

nativeImage := {
  import sbt.Keys.streams
  val assemblyFatJar = assembly.value
  val assemblyFatJarPath = assemblyFatJar.getParent()
  val assemblyFatJarName = assemblyFatJar.getName()
  val outputPath = (baseDirectory.value / "out").getAbsolutePath()
  val outputName = "zioquickstart-executable"
  val nativeImageDocker = "graalvm-native-image-local"

  val cmd = s"""docker run
               | --volume ${assemblyFatJarPath}:/opt/assembly
               | --volume ${outputPath}:/opt/native-image
               | ${nativeImageDocker}
               | --static
               | ${nativeImageOpts.mkString(" ")}
               | -jar /opt/assembly/${assemblyFatJarName}
               | ${outputName}""".stripMargin.filter(_ != '\n')

  val log = streams.value.log
  log.info(s"Building native image from ${assemblyFatJarName}")
  log.info(cmd)
  val result = (cmd.!(log))

  if (result == 0) file(s"${outputPath}/${outputName}")
  else {
    log.error(s"Native image command failed:\n ${cmd}")
    throw new Exception("Native image command failed")
  }
}
