package com.event.processing.notifier.config;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

public class KafkaPartitioner implements Partitioner {

  @Override
  public int partition(String s, Object o, byte[] bytes, Object o1, byte[] bytes1, Cluster cluster) {
    //TODO: update algorithsm
    int numPartitions = cluster.partitionCountForTopic(s);
    if (o == null) return 0;
    String key = o.toString();

    int hash = key.hashCode() & 0x7fffffff;

    return hash % numPartitions;
  }

  @Override
  public void close() {

  }

  @Override
  public void configure(Map<String, ?> map) {

  }
}
