package com.example.flink.manager.service;

import com.example.flink.manager.dto.WorkflowDefinition;
import com.squareup.javapoet.*;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.util.Properties;

@Service
public class CodeGeneratorService {

    public String generateJobCode(WorkflowDefinition workflow) {
        String jobName = workflow.getName().replaceAll("\\s+", "");
        String packageName = "com.example.flink.generated";

        // Main method builder
        MethodSpec.Builder mainBuilder = MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(String[].class, "args")
                .addException(Exception.class);

        // Env setup
        mainBuilder.addStatement("$T env = $T.getExecutionEnvironment()",
                ClassName.get("org.apache.flink.streaming.api.environment", "StreamExecutionEnvironment"),
                ClassName.get("org.apache.flink.streaming.api.environment", "StreamExecutionEnvironment"));

        // Iterate nodes to generate sources/operators
        // Simplified: Assuming linear flow for demo
        for (WorkflowDefinition.Node node : workflow.getNodes()) {
            if ("SOURCE".equalsIgnoreCase(node.getType())) {
                generateSource(mainBuilder, node);
            } else if ("CEP".equalsIgnoreCase(node.getType())) {
                generateCepLogic(mainBuilder, node);
            } else if ("SINK".equalsIgnoreCase(node.getType())) {
                generateSink(mainBuilder, node);
            }
        }

        mainBuilder.addStatement("env.execute($S)", jobName);

        TypeSpec jobClass = TypeSpec.classBuilder(jobName + "Job")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(mainBuilder.build())
                .build();

        JavaFile javaFile = JavaFile.builder(packageName, jobClass)
                .build();

        return javaFile.toString();
    }

    private void generateSource(MethodSpec.Builder builder, WorkflowDefinition.Node node) {
        // Example: Kafka Source Generation
        builder.addComment("Source: " + node.getId());
        builder.addStatement("$T<$T> properties = new $T<>()", Properties.class, Object.class, Properties.class);
        builder.addStatement("properties.setProperty($S, $S)", "bootstrap.servers", "localhost:9092"); // Should come
                                                                                                       // from node
                                                                                                       // props

        // This is pseudo-code for the generator logic.
        // In real impl, we would check node.getProperties().get("topic")
        String topic = (String) node.getProperties().getOrDefault("topic", "input-topic");

        builder.addStatement("$T<String> stream = env.addSource(new $T<>($S, new $T(), properties))",
                ClassName.get("org.apache.flink.streaming.api.datastream", "DataStream"),
                ClassName.get("org.apache.flink.streaming.connectors.kafka", "FlinkKafkaConsumer"),
                topic,
                ClassName.get("org.apache.flink.api.common.serialization", "SimpleStringSchema"));
    }

    private void generateCepLogic(MethodSpec.Builder builder, WorkflowDefinition.Node node) {
        builder.addComment("CEP Logic for " + node.getId());
        // Add Broadcast State logic here
        // 1. Define Rule Source (Kafka)
        // 2. Broadcast it
        // 3. Connect 'stream' with 'broadcast'
        builder.addCode("// TODO: Inject Dynamic CEP Broadcast Pattern Logic here\n");
    }

    private void generateSink(MethodSpec.Builder builder, WorkflowDefinition.Node node) {
        builder.addComment("Sink: " + node.getId());
        builder.addStatement("stream.print()"); // Default to print for demo
    }
}
