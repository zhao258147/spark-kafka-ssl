package com.yzhao.kafkassl

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent

import scala.collection.JavaConverters._

object Main {

  def main(args: Array[String]): Unit = {
    implicit val spark: SparkSession = SparkSession
      .builder()
      .getOrCreate()

    import spark.implicits._

    val config: Config = ConfigFactory.load()

    val consumerConfig = config.entrySet().asScala.map(e ⇒ e.getKey → e.getValue.unwrapped()).toMap[String, AnyRef]

    val kafkaConsumer = KafkaConsumerFromStartTime(consumerConfig)

    val txOffsets = kafkaConsumer.transactionOffsets("test", 0)

    val consumerRecord: RDD[ConsumerRecord[String, String]] = KafkaUtils.createRDD[String, String](
      spark.sparkContext,
      consumerConfig.asJava,
      txOffsets.toArray,
      PreferConsistent
    )

    val values: RDD[String] = consumerRecord.map{ rd =>
      rd.value()
    }
    
    val s: LocalDateTime = LocalDateTime.now
    val time = s.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

    val seq = List(time)
    val ds: Dataset[String] = seq.toDS()
    val keyval = ds.map{ time: String =>
      time -> time
    }
    .toDF("key", "value")

    keyval.show

    keyval
      .write.format("kafka")
      .option("kafka.bootstrap.servers", "")
      .option("kafka.security.protocol", "SASL_SSL")
      .option("kafka.ssl.endpoint.identification.algorithm", "https")
      .option("kafka.sasl.mechanism", "PLAIN")
      .option("topic", "test.topic")
      .save

    spark.stop
  }
}
