package com.event.processing.dlq_service.config;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

/**
 * Custom Kafka partitioner for distributing messages across partitions.
 * This class implements a simple hash-based partitioning strategy to ensure
 * that messages with the same key are sent to the same partition.
 * Following the Single Responsibility Principle, this class only handles
 * the partitioning of messages.
 */
public class KafkaPartitioner implements Partitioner {

  /**
   * Determines the partition for a given record.
   * Uses a simple hash-based algorithm to ensure that messages with the same key
   * are sent to the same partition.
   *
   * @param topic      The topic the record will be sent to
   * @param key        The key of the record (or null if no key)
   * @param keyBytes   Serialized key
   * @param value      The value of the record
   * @param valueBytes Serialized value
   * @param cluster    Information about the Kafka cluster
   * @return The partition number
   */
  @Override
  public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
    int numPartitions = cluster.partitionCountForTopic(topic);
    if (numPartitions <= 0) {
      return 0;
    }

    if (key == null) {
      return 0;
    }

    String keyStr = key.toString();
    int hash = keyStr.hashCode() & 0x7fffffff; // Make sure it's positive

    return hash % numPartitions;
  }

  @Override
  public void close() {
    // No resources to clean up
  }

  @Override
  public void configure(Map<String, ?> configs) {
    // No configuration needed
  }
}
