package com.afeibaili.window;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;
import java.util.ArrayList;

/**
 * 水位线
 *
 * @author AfeiB
 * {@code @date}2024/11/19 下午3:01
 */

public class WaterMark {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        ArrayList<Integer> strings = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            strings.add(i);
        }
        DataStreamSource<Integer> streamSource = environment.fromCollection(strings);
/*        WatermarkStrategy<Integer> integerWatermarkStrategy = WatermarkStrategy
                .<Integer>forMonotonousTimestamps()
                .withTimestampAssigner(new SerializableTimestampAssigner<Integer>() {
                    @Override
                    public long extractTimestamp(Integer element, long recordTimestamp) {
                        System.out.println("Element: " + element + ", Event Time: " + recordTimestamp);
                        return element * 1000L;
                    }
                });
        streamSource.assignTimestampsAndWatermarks(integerWatermarkStrategy)
                .windowAll(TumblingEventTimeWindows.of(Time.seconds(5)))
                .aggregate(new AggregateFunction<Integer, Integer, Integer>() {
                    @Override
                    public Integer createAccumulator() {
                        System.out.println("创建累加器");
                        return 0;
                    }

                    @Override
                    public Integer add(Integer value, Integer accumulator) {
                        return value + accumulator;
                    }

                    @Override
                    public Integer getResult(Integer accumulator) {
                        return accumulator;
                    }

                    @Override
                    public Integer merge(Integer a, Integer b) {
                        return 0;
                    }
                })
                .print();*/
        WatermarkStrategy<Integer> integerWatermarkStrategy = WatermarkStrategy.<Integer>forBoundedOutOfOrderness(Duration.ofSeconds(2))
                .withTimestampAssigner(new SerializableTimestampAssigner<Integer>() {
                    @Override
                    public long extractTimestamp(Integer element, long recordTimestamp) {
                        return element * 1000L;
                    }
                });
        streamSource.assignTimestampsAndWatermarks(integerWatermarkStrategy)
                .windowAll(TumblingEventTimeWindows.of(Time.seconds(5)))
                .reduce(Integer::sum)
                .print();
        environment.execute();
    }
}
