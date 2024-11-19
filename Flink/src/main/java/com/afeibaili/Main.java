package com.afeibaili;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Main {
    public static void main(String[] args) {
        KafkaSource<String> source = KafkaSource.<String>builder().setBootstrapServers("master:9092").setGroupId("afei").setStartingOffsets(OffsetsInitializer.earliest()).setValueOnlyDeserializer(new SimpleStringSchema()).setTopics("ods_mall_log").build();

        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setParallelism(2);

        DataStreamSource<String> streamSource = environment.fromSource(source, WatermarkStrategy.noWatermarks(), "kafka");

        ObjectMapper objectMapper = new ObjectMapper();

        streamSource.map(new MapFunction<String, String>() {
            @Override
            public String map(String value) throws Exception {
                JsonNode node = objectMapper.readTree(value);
                return node.asText();
            }
        }).print();

    }
}