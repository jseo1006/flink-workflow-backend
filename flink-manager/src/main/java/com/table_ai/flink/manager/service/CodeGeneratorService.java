package com.table_ai.flink.manager.service;

import com.table_ai.flink.manager.dto.WorkflowDefinition;
import com.squareup.javapoet.*;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.util.Properties;

@Service
public class CodeGeneratorService {

        public String generateJobCode(WorkflowDefinition workflow) {
                String jobName = workflow.getName().replaceAll("\\s+", "");
                String packageName = "com.table_ai.flink.generated";

                // Main method builder
                MethodSpec.Builder mainBuilder = MethodSpec.methodBuilder("main")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(void.class)
                                .addParameter(String[].class, "args")
                                .addException(Exception.class);

                // Env setup
                mainBuilder.addStatement("$T env = $T.getExecutionEnvironment()",
                                ClassName.get("org.apache.flink.streaming.api.environment",
                                                "StreamExecutionEnvironment"),
                                ClassName.get("org.apache.flink.streaming.api.environment",
                                                "StreamExecutionEnvironment"));

                // Iterate nodes to generate sources/operators
                for (WorkflowDefinition.Node node : workflow.getNodes()) {
                        if ("SOURCE".equalsIgnoreCase(node.getType())) {
                                generateSource(mainBuilder, node, jobName);
                        } else if ("PROCESS".equalsIgnoreCase(node.getType())) {
                                generateProcessLogic(mainBuilder, node);
                        } else if ("CEP".equalsIgnoreCase(node.getType())) {
                                generateCepLogic(mainBuilder, node, jobName);
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

        private void generateSource(MethodSpec.Builder builder, WorkflowDefinition.Node node, String jobName) {
                // Example: Kafka Source Generation
                builder.addComment("Source: " + node.getId());
                builder.addStatement("$T properties = new $T()", Properties.class, Properties.class);
                builder.addStatement("properties.setProperty($S, $S)", "bootstrap.servers",
                                "afc2ab972e55840898b5bc1160d464b2-f6b61b40e1eb9327.elb.ap-northeast-2.amazonaws.com:9092");
                builder.addStatement("properties.setProperty($S, $S)", "auto.offset.reset", "earliest");
                builder.addStatement("properties.setProperty($S, $S + java.util.UUID.randomUUID())", "group.id",
                                jobName + "-group-");

                String topic = (String) node.getProperties().getOrDefault("topic", "input-topic");

                builder.addStatement("$T<String> stream = env.addSource(new $T<>($S, new $T(), properties))",
                                ClassName.get("org.apache.flink.streaming.api.datastream", "DataStream"),
                                ClassName.get("org.apache.flink.streaming.connectors.kafka", "FlinkKafkaConsumer"),
                                topic,
                                ClassName.get("org.apache.flink.api.common.serialization", "SimpleStringSchema"));
        }

        private void generateProcessLogic(MethodSpec.Builder builder, WorkflowDefinition.Node node) {
                builder.addComment("Process: " + node.getId());
                String function = (String) node.getProperties().get("function");
                String condition = (String) node.getProperties().get("condition");

                if ("filter".equalsIgnoreCase(function) && condition != null) {
                        String[] parts = condition.split(" ");
                        if (parts.length == 3) {
                                String field = parts[0];
                                String operator = parts[1];
                                String value = parts[2];

                                builder.addCode("$T mapper = new $T();\n",
                                                ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper"),
                                                ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper"));

                                builder.addCode("stream = stream.filter(new $T<String>() {\n",
                                                ClassName.get("org.apache.flink.api.common.functions",
                                                                "FilterFunction"));
                                builder.addCode("    @Override\n");
                                builder.addCode("    public boolean filter(String value) throws Exception {\n");
                                builder.addCode("        try {\n");
                                builder.addCode("            $T root = new $T().readTree(value);\n",
                                                ClassName.get("com.fasterxml.jackson.databind", "JsonNode"),
                                                ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper"));

                                if (">".equals(operator)) {
                                        builder.addCode("            return root.has($S) && root.get($S).asInt() > $L;\n",
                                                        field, field,
                                                        value);
                                } else if ("<".equals(operator)) {
                                        builder.addCode("            return root.has($S) && root.get($S).asInt() < $L;\n",
                                                        field, field,
                                                        value);
                                } else {
                                        // Default deny or simple exists check
                                        builder.addCode("            return true;\n");
                                }

                                builder.addCode("        } catch (Exception e) {\n");
                                builder.addCode("            return false;\n"); // Drop invalid
                                builder.addCode("        }\n");
                                builder.addCode("    }\n");
                                builder.addCode("});\n");
                        }
                }
        }

        private void generateCepLogic(MethodSpec.Builder builder, WorkflowDefinition.Node node, String jobName) {
                builder.addComment("CEP Logic for " + node.getId());

                // 1. Define Rule State Descriptor
                builder.addStatement("$T<$T, $T> ruleStateDescriptor = new $T<>($S, $T.class, $T.class)",
                                ClassName.get("org.apache.flink.api.common.state", "MapStateDescriptor"),
                                String.class, String.class,
                                ClassName.get("org.apache.flink.api.common.state", "MapStateDescriptor"),
                                "rules-state",
                                String.class, String.class);

                // 2. Create Control Stream (Kafka Source for Rules)
                builder.addStatement("$T propertiesRules = new $T()", Properties.class, Properties.class);
                builder.addStatement("propertiesRules.setProperty($S, $S)", "bootstrap.servers",
                                "afc2ab972e55840898b5bc1160d464b2-f6b61b40e1eb9327.elb.ap-northeast-2.amazonaws.com:9092");
                builder.addStatement("propertiesRules.setProperty($S, $S)", "auto.offset.reset", "earliest");
                builder.addStatement("propertiesRules.setProperty($S, $S + java.util.UUID.randomUUID())", "group.id",
                                jobName + "-rules-group-");

                // Use SimpleStringSchema for deserialization
                builder.addStatement("$T<String> ruleStream = env.addSource(new $T<>($S, new $T(), propertiesRules))",
                                ClassName.get("org.apache.flink.streaming.api.datastream", "DataStream"),
                                ClassName.get("org.apache.flink.streaming.connectors.kafka", "FlinkKafkaConsumer"),
                                "cep-rules-control",
                                ClassName.get("org.apache.flink.api.common.serialization", "SimpleStringSchema"));

                // 3. Broadcast Result
                builder.addStatement("$T<$T> broadcastStream = ruleStream.broadcast(ruleStateDescriptor)",
                                ClassName.get("org.apache.flink.streaming.api.datastream", "BroadcastStream"),
                                String.class);

                // 4. Connect and Process with Explicit Types
                // Note: BroadcastProcessFunction<IN1, IN2, OUT>
                ClassName bpfClass = ClassName.get("org.apache.flink.streaming.api.functions.co",
                                "BroadcastProcessFunction");
                ClassName collectorClass = ClassName.get("org.apache.flink.util", "Collector");
                ClassName readOnlyStateClass = ClassName.get("org.apache.flink.api.common.state",
                                "ReadOnlyBroadcastState");

                builder.addCode("stream = stream.connect(broadcastStream).process(new $T<String, String, String>() {\n",
                                bpfClass);

                builder.addCode("    @Override\n");
                builder.addCode("    public void processElement(String value, ReadOnlyContext ctx, $T<String> out) throws Exception {\n",
                                collectorClass);
                builder.addCode("        // Apply rules from broadcast state\n");

                builder.addCode("        $T<String, String> state = ctx.getBroadcastState(ruleStateDescriptor);\n",
                                readOnlyStateClass);

                ClassName objectMapper = ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper");
                ClassName jsonNode = ClassName.get("com.fasterxml.jackson.databind", "JsonNode");

                builder.addCode("        boolean matchedAny = false;\n");

                builder.addCode("        $T mapper = new $T();\n", objectMapper, objectMapper);
                builder.addCode("        $T dataNode = mapper.readTree(value);\n", jsonNode);
                builder.addCode("\n");

                builder.addCode("        if (state.immutableEntries().iterator().hasNext()) {\n");
                builder.addCode("            for (java.util.Map.Entry<String, String> entry : state.immutableEntries()) {\n");
                builder.addCode("                String ruleJson = entry.getValue();\n");
                builder.addCode("                try {\n");
                builder.addCode("                    $T ruleNode = mapper.readTree(ruleJson);\n", jsonNode);
                builder.addCode("                    \n");
                builder.addCode("                    if (ruleNode.has(\"field\") && ruleNode.has(\"operator\") && ruleNode.has(\"threshold\")) {\n");
                builder.addCode("                        String field = ruleNode.get(\"field\").asText();\n");
                builder.addCode("                        String operator = ruleNode.get(\"operator\").asText();\n");
                builder.addCode("                        double threshold = ruleNode.get(\"threshold\").asDouble();\n");
                builder.addCode("                        \n");
                builder.addCode("                        if (dataNode.has(field)) {\n");
                builder.addCode("                            double dataValue = dataNode.get(field).asDouble();\n");
                builder.addCode("                            boolean matched = false;\n");
                builder.addCode("                            \n");
                builder.addCode("                            if (\">=\".equals(operator)) matched = dataValue >= threshold;\n");
                builder.addCode("                            else if (\"<=\".equals(operator)) matched = dataValue <= threshold;\n");
                builder.addCode("                            else if (\"=\".equals(operator)) matched = dataValue == threshold;\n");
                builder.addCode("                            else if (\">\".equals(operator)) matched = dataValue > threshold;\n");
                builder.addCode("                            else if (\"<\".equals(operator)) matched = dataValue < threshold;\n");
                builder.addCode("                            \n");
                builder.addCode("                            if (matched) {\n");
                builder.addCode("                                out.collect(value + \" [Matched Rule: \" + entry.getKey() + \"]\");\n");
                builder.addCode("                                matchedAny = true;\n");
                builder.addCode("                            }\n");
                builder.addCode("                        }\n");
                builder.addCode("                    }\n");
                builder.addCode("                } catch (Exception e) {\n");
                builder.addCode("                   // Ignore invalid rules\n");
                builder.addCode("                }\n");
                builder.addCode("            }\n");
                builder.addCode("        }\n");

                builder.addCode("        if (!matchedAny) {\n");
                builder.addCode("             out.collect(value + \" [No rules matched]\");\n");
                builder.addCode("        }\n");
                builder.addCode("    }\n");

                builder.addCode("    @Override\n");
                builder.addCode("    public void processBroadcastElement(String rule, Context ctx, $T<String> out) throws Exception {\n",
                                collectorClass);
                builder.addCode("        System.out.println(\"Received new rule: \" + rule);\n");
                builder.addCode("        ctx.getBroadcastState(ruleStateDescriptor).put(\"latest-rule\", rule);\n");
                builder.addCode("    }\n");
                builder.addCode("});\n");
        }

        private void generateSink(MethodSpec.Builder builder, WorkflowDefinition.Node node) {
                builder.addComment("Sink: " + node.getId());

                String type = (String) node.getProperties().getOrDefault("type", "console");

                if ("kafka".equalsIgnoreCase(type)) {
                        String topic = (String) node.getProperties().getOrDefault("topic", "output-topic");
                        String bootstrapServers = "afc2ab972e55840898b5bc1160d464b2-f6b61b40e1eb9327.elb.ap-northeast-2.amazonaws.com:9092";

                        builder.addCode("// Kafka Sink\n");
                        builder.addStatement("$T sinkProperties = new $T()", Properties.class, Properties.class);
                        builder.addStatement("sinkProperties.setProperty($S, $S)", "bootstrap.servers",
                                        bootstrapServers);

                        builder.addStatement("stream.addSink(new $T<>($S, new $T(), sinkProperties))",
                                        ClassName.get("org.apache.flink.streaming.connectors.kafka",
                                                        "FlinkKafkaProducer"),
                                        topic,
                                        ClassName.get("org.apache.flink.api.common.serialization",
                                                        "SimpleStringSchema"));
                } else {
                        builder.addStatement("stream.print()");
                }
        }
}
