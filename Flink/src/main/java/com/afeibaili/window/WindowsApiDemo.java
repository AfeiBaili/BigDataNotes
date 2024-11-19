package com.afeibaili.window;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;

import java.util.ArrayList;

/**
 * 窗口API类
 *
 * @author AfeiB
 * {@code @date}2024/11/18 下午9:10
 */

public class WindowsApiDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            strings.add(i + "");
        }
        DataStreamSource<String> streamSource = environment.fromCollection(strings);
//        streamSource.windowAll(SlidingProcessingTimeWindows.of(Time.seconds(10), Time.seconds(2))); 基于时间的滑动窗口
//        streamSource.windowAll(TumblingProcessingTimeWindows.of(Time.seconds(10))); 基于时间的滚动窗口
//        streamSource.windowAll(ProcessingTimeSessionWindows.withGap(Time.seconds(10))); 基于时间的会话窗口

//        streamSource.countWindowAll(2,1); 基于计数的滑动窗口
        AllWindowedStream<String, GlobalWindow> countWindowAll = streamSource.countWindowAll(5);
//        SingleOutputStreamOperator<String> reduce = countWindowAll.reduce(new ReduceFunction<String>() {
//            @Override
//            public String reduce(String value1, String value2) throws Exception {
//                return Integer.parseInt(value1) + Integer.parseInt(value2) + "";
//            }
//        });
/*        countWindowAll.aggregate(new AggregateFunction<String, Integer, Integer>() {
            @Override
            public Integer createAccumulator() {
                System.out.println("创建累加器");
                return 0;
            }

            @Override
            public Integer add(String value, Integer accumulator) {
                System.out.println("相加");
                return Integer.parseInt(value) + accumulator;
            }

            @Override
            public Integer getResult(Integer accumulator) {
                System.out.println("返回结果");
                return accumulator;
            }

            @Override
            public Integer merge(Integer a, Integer b) {
                System.out.println("只有会话底层才会用到");
                return 0;
            }
        }).print();*/
        /*countWindowAll.process(new ProcessAllWindowFunction<String, String, GlobalWindow>() {
            @Override
            public void process(ProcessAllWindowFunction<String, String, GlobalWindow>.Context context, Iterable<String> elements, Collector<String> out) throws Exception {
                int count = 0;
                for (String element : elements) {
                    count += Integer.parseInt(element);
                }
                out.collect(count + ":包含这些数据=>" + elements);
            }
        }).print();*//*
        countWindowAll.aggregate(new AggregateFunction<String, Integer, Integer>() {
            @Override
            public Integer createAccumulator() {
                return 0;
            }

            @Override
            public Integer add(String value, Integer accumulator) {
                return 0;
            }

            @Override
            public Integer getResult(Integer accumulator) {
                return 0;
            }

            @Override
            public Integer merge(Integer a, Integer b) {
                return 0;
            }
        }, new ProcessAllWindowFunction<Integer, Integer, GlobalWindow>() {
            @Override
            public void process(ProcessAllWindowFunction<Integer, Integer, GlobalWindow>.Context context, Iterable<Integer> elements, Collector<Integer> out) throws Exception {

            }
        });*/
        environment.execute();
    }
}