package com.table_ai.flink.manager.service;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Properties;

@Service
public class CepRuleService {

    @Value("${cep.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${cep.kafka.topic}")
    private String topic;

    private KafkaProducer<String, String> producer;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = new KafkaProducer<>(props);
    }

    public void updateRule(String jobId, String ruleJson) {
        // Send rule update to Kafka.
        // Key = jobId (to ensure ordering if needed, or to route to specific partition)
        // Value = ruleJson
        producer.send(new ProducerRecord<>(topic, jobId, ruleJson));
        System.out.println("Published CEP rule update for Job: " + jobId);
    }

    @PreDestroy
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}
