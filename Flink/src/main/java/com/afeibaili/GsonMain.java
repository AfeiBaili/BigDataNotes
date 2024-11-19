package com.afeibaili;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Gson测试类
 *
 * @author AfeiB
 * {@code @date}2024/11/18 下午8:09
 */

public class GsonMain {
    public static void main(String[] args) {
        Gson gson = new Gson();
//        System.out.println(gson.toJson(new Person("张三", 18)));
        Person person = gson.fromJson("{\"name2\":\"张三\",\"age2\":20}", Person.class);
        System.out.println(person);
    }
}

@Data
class Person {
    @SerializedName("name2")
    String name;
    @SerializedName("age2")
    int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
