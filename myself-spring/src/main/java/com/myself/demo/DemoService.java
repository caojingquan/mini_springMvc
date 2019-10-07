package com.myself.demo;

import com.myself.springFramework.annotation.GPService;

@GPService
public class DemoService implements IDemoService{

    public String get(String name) {
        return "My name is "+name;
    }

}
