package com.myself.demo;

import com.myself.springFramework.annotation.GPAutowired;
import com.myself.springFramework.annotation.GPController;
import com.myself.springFramework.annotation.GPRequestMapping;
import com.myself.springFramework.annotation.GPRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@GPController
@GPRequestMapping("/demo")
public class DemoAction {
    @GPAutowired
    private IDemoService demoService;

    @GPRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @GPRequestParam("name") String name) throws IOException {
        String result = demoService.get(name);
        try{
            response.getWriter().write(result);
        }catch(Exception e){
            e.printStackTrace();
            response.getWriter().write(Arrays.toString(e.getStackTrace()));
        }
    }

    @GPRequestMapping("/remove")
    public void remove(HttpServletRequest request, HttpServletResponse response,
                       @GPRequestParam("id") String name) throws IOException{
        response.getWriter().write("success");
    }
}
