package com.yzhao.kafkassl

import java.lang.{Long => JLong}
import java.util.{Map => JMap}

import KafkaConsumerFromStartTime.{Offset, TimeStamp}
import KafkaConsumerConfig.KafkaConsumerConfig
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.{Consumer, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.OffsetRange

import scala.collection.JavaConverters._
import scala.util.Try

class KafkaConsumerFromStartTime(consumer: Consumer[String, String]) {
  def transactionOffsets(topic: String, startTime: Long): List[OffsetRange] = {
    val partitionsIds = partitionIdsFor(topic)
    val topicPartitions = partitionsIds.map(new TopicPartition(topic, _))
    val timestampsToSearch = topicPartitions.map((_, startTime)).toMap

    findOffsets(topicPartitions, timestampsToSearch).toList
  }

  private def partitionIdsFor(topic: String): List[Int] =
    consumer.partitionsFor(topic).asScala.toList.map(_.partition())

  private def offsetsForTimestamp(partitionsToTimes: Map[TopicPartition, TimeStamp]): Map[TopicPartition, Offset] = {
    val jPartitionsToTimestamps: JMap[TopicPartition, JLong] = partitionsToTimes.mapValues(JLong.valueOf).asJava
    val partitionToTimestamp = consumer.offsetsForTimes(jPartitionsToTimestamps)
    partitionToTimestamp.asScala
      .filter { _._2 != null } // By default, offsetsForTimes returns (topic -> null) if no messages can be found.
      .mapValues(_.offset)
      .toMap
  }

  private def currentOffsets(tps: Seq[TopicPartition]): Map[TopicPartition, Offset] = {
    val java = tps.toSet.asJava
    consumer.endOffsets(java).asScala.mapValues(_.longValue()).toMap
  }

  private def findOffsets(topicPartitions: Seq[TopicPartition],
    partitionsToTimestamps: Map[TopicPartition, TimeStamp]) = {

    val beginOffsets: Map[TopicPartition, Offset] = offsetsForTimestamp(partitionsToTimestamps)
    val endOffsets: Map[TopicPartition, Offset] = currentOffsets(topicPartitions)

    beginOffsets.map {
      case (tp, offset) =>
        OffsetRange.create(tp.topic(), tp.partition(), offset, endOffsets(tp))
    }
  }
}

object KafkaConsumerFromStartTime {
  type TimeStamp = Long
  type Offset = Long

  def apply(consumerConfig: KafkaConsumerConfig): KafkaConsumerFromStartTime =
    new KafkaConsumerFromStartTime(
      new KafkaConsumer[String, String](consumerConfig.asJava,
        new StringDeserializer(),
        new StringDeserializer())
    )
}

object KafkaConsumerConfig {
  def configToMap(config: Config): Map[String, AnyRef] =
    config.entrySet().asScala.map(e ⇒ e.getKey → e.getValue.unwrapped()).toMap[String, AnyRef]

  type KafkaConsumerConfig = Map[String, AnyRef]
  def apply(config: Config): KafkaConsumerConfig = configToMap(config)
}
