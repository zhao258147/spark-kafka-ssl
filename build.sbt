name := "Spark-kafka-ssl"

version := "0.1"

scalaVersion := "2.11.12"

// Spark
val sparkVer = "2.3.1"
val spark = "org.apache.spark" %% "spark-core" % sparkVer % "provided"
val sparkSql = "org.apache.spark" %% "spark-sql" % sparkVer % "provided"
val sparkKafka = "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVer
val sparkSqlKafka = "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVer
val sparkStreaming = "org.apache.spark" %% "spark-streaming" % sparkVer % "provided"
val kafka = "org.apache.kafka" %% "kafka" % "0.10.2.1"

val ficus = "com.iheart" %% "ficus" % "1.4.3"

libraryDependencies ++= Seq(
  sparkStreaming,
  sparkSqlKafka,
  sparkKafka,
  sparkSql,
  spark,
  kafka,
  ficus
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
  case PathList("org","aopalliance", xs @ _*) => MergeStrategy.last
  case PathList("javax", "inject", xs @ _*) => MergeStrategy.last
  case PathList("javax", "servlet", xs @ _*) => MergeStrategy.last
  case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.last
  case PathList("org", "apache", xs @ _*) => MergeStrategy.last
  case PathList("org", "slf4j", "impl", xs @ _*) => MergeStrategy.last
  case PathList("com", "google", xs @ _*) => MergeStrategy.last
  case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
  case PathList("net", "jpountz", xs @ _*) => MergeStrategy.last
  case "about.html" => MergeStrategy.rename
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
  case "META-INF/mailcap" => MergeStrategy.last
  case "META-INF/mimetypes.default" => MergeStrategy.last
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case "overview.html" => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}