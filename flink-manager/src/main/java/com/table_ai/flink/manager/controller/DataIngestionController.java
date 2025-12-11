package com.table_ai.flink.manager.controller;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data")
public class DataIngestionController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final org.springframework.kafka.core.ConsumerFactory<String, String> consumerFactory;

    public DataIngestionController(KafkaTemplate<String, String> kafkaTemplate,
            org.springframework.kafka.core.ConsumerFactory<String, String> consumerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
    }

    @PostMapping("/send")
    public String sendData(@RequestParam String topic, @RequestBody String message) {
        kafkaTemplate.send(topic, message);
        return "Message sent to topic: " + topic;
    }

    @GetMapping("/consume")
    public java.util.List<String> consumeData(@RequestParam String topic) {
        java.util.List<String> messages = new java.util.ArrayList<>();
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("bootstrap.servers", "kafka-svc:9092");
        props.put("group.id", "flink-manager-peek-" + java.util.UUID.randomUUID());
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");

        org.springframework.kafka.core.DefaultKafkaConsumerFactory<String, String> cf = new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(
                props);

        try (org.apache.kafka.clients.consumer.Consumer<String, String> consumer = cf.createConsumer()) {
            consumer.subscribe(java.util.Collections.singletonList(topic));
            org.apache.kafka.clients.consumer.ConsumerRecords<String, String> records = consumer
                    .poll(java.time.Duration.ofMillis(5000));
            for (org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record : records) {
                messages.add(record.value());
            }
        }
        return messages;
    }
}
