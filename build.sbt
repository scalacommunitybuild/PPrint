val baseSettings = Seq(
  organization := "com.lihaoyi",
  name := "pprint",
  version := _root_.pprint.Constants.version,
  crossScalaVersions := Seq("2.13.11"),
  scalaVersion := crossScalaVersions.value.head,
  testFrameworks := Seq(new TestFramework("utest.runner.Framework")),
  publishTo := Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
  scmInfo := Some(ScmInfo(
    browseUrl = url("https://github.com/lihaoyi/PPrint"),
    connection = "scm:git:git@github.com:lihaoyi/PPrint.git"
  )),
  homepage := Some(url("https://github.com/lihaoyi/PPrint")),
  licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/mit-license.html")),
  developers += Developer(
    email = "haoyi.sg@gmail.com",
    id = "lihaoyi",
    name = "Li Haoyi",
    url = url("https://github.com/lihaoyi")
  )
)

baseSettings

lazy val pprint = _root_.sbtcrossproject.CrossPlugin.autoImport
  .crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    baseSettings,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "fansi" % "0.4.0",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
      "com.lihaoyi" %%% "sourcecode" % "0.3.0",
      "com.lihaoyi" %%% "utest" % "0.8.0" % Test
    ),

     Compile / unmanagedSourceDirectories ++= Seq(
       baseDirectory.value / ".." / "src",
       baseDirectory.value / ".." / "src-2",
       baseDirectory.value / ".." / "src-2.13",
     ),
    Test / unmanagedSourceDirectories ++= Seq(
      baseDirectory.value / ".." / "test" / "src",
      baseDirectory.value / ".." / "test" / "src-2",
      baseDirectory.value / ".." / "test" / "src-2.13",
      baseDirectory.value / ".." / "test" / "src-jvm",
    ),
    Compile / sourceGenerators += Def.task {
      val dir = (Compile / sourceManaged).value
      val file = dir/"pprint"/"TPrintGen.scala"

      val typeGen = for(i <- 2 to 22) yield {
        val ts = (1 to i).map("T" + _).mkString(", ")
        val tsBounded = (1 to i).map("T" + _ + ": Type").mkString(", ")
        val tsGet = (1 to i).map("get[T" + _ + "](cfg)").mkString(" + \", \" + ")
        s"""
          implicit def F${i}TPrint[$tsBounded, R: Type] = make[($ts) => R](cfg =>
            "(" + $tsGet + ") => " + get[R](cfg)
          )
          implicit def T${i}TPrint[$tsBounded] = make[($ts)](cfg =>
            "(" + $tsGet + ")"
          )
        """
      }
      val output = s"""
        package pprint
        trait TPrintGen[Type[_], Cfg]{
          def make[T](f: Cfg => String): Type[T]
          def get[T: Type](cfg: Cfg): String
          implicit def F0TPrint[R: Type] = make[() => R](cfg => "() => " + get[R](cfg))
          implicit def F1TPrint[T1: Type, R: Type] = {
            make[T1 => R](cfg => get[T1](cfg) + " => " + get[R](cfg))
          }
          ${typeGen.mkString("\n")}
        }
      """.stripMargin
      IO.write(file, output)
      Seq(file)
    }.taskValue
  )
  .nativeSettings(
    nativeLinkStubs := true
  )

lazy val pprintJVM = pprint.jvm
lazy val pprintJS = pprint.js
lazy val pprintNative = pprint.native
