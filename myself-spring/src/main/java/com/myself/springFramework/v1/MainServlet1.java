package com.myself.springFramework.v1;

import com.myself.springFramework.annotation.GPAutowired;
import com.myself.springFramework.annotation.GPController;
import com.myself.springFramework.annotation.GPRequestMapping;
import com.myself.springFramework.annotation.GPService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MainServlet1 extends HttpServlet {

    private Map<String,Object> register = new HashMap<String,Object>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatcher(req,resp);
        } catch (Exception e) {
            resp.getWriter().write(
                    Arrays.toString(e.getStackTrace())
            );
        }
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");
        if(!register.containsKey(url))resp.getWriter().write("404 not found");
        Method method= (Method)this.register.get(url);
        Map<String,String[]> parameterMap = req.getParameterMap();
        method.invoke(register.get(method.getDeclaringClass().getName()),new Object[]{req,resp,parameterMap.get("name")[0]});
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        String initConfigParam = config.getInitParameter("initConfigParam");
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(initConfigParam);
        Properties p = new Properties();
        try {
            p.load(in);
            String scanPackage = p.getProperty("scan.package");
            doScanner(scanPackage);
            System.out.println("***"+register);
            if(register.isEmpty())return;
            for (String className: register.keySet()) {
                if(!className.contains("."))continue;
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(GPController.class)){
                    register.put(className,clazz.newInstance());
                    String baseUrl = "";
                    if(clazz.isAnnotationPresent(GPRequestMapping.class)){
                        baseUrl = clazz.getAnnotation(GPRequestMapping.class).value();
                    }
                    Method[] methods = clazz.getMethods();
                    for(Method method : methods){
                        if(!method.isAnnotationPresent(GPRequestMapping.class))continue;
                        GPRequestMapping annotation = method.getAnnotation(GPRequestMapping.class);
                        String url = (baseUrl+"/"+annotation.value()).replaceAll("/+","/");
                        register.put(url,method);
                        System.out.println("Mapped "+url+","+method);
                    }
                }else if(clazz.isAnnotationPresent(GPService.class)){
                    String value = clazz.getAnnotation(GPService.class).value();
                    if("".equals(value))value = className;
                    register.put(value,clazz.newInstance());
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for(Class cla : interfaces){
                        register.put(cla.getName(),clazz.newInstance());
                    }
                } else{
                    continue;
                }
                System.out.println("keySet:"+register.keySet());
            }
            for(Object obj : register.values()){
                if(null == obj)continue;
                Class<?> clazz = obj.getClass();
                if(clazz.isAnnotationPresent(GPController.class)){
                    Field[] fields = clazz.getDeclaredFields();
                    for(Field field : fields){
                        if(!field.isAnnotationPresent(GPAutowired.class))continue;
                        String beanName = field.getAnnotation(GPAutowired.class).value();
                        if("".equals(beanName)){
                            beanName = field.getType().getName();
                        }
                        field.setAccessible(true);
                        field.set(obj,register.get(beanName));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } finally {
            if(null != in){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("GP MVC Framwork init end!");
    }

    private void doScanner(String scanPackage) {
//        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File directory = new File("E:\\新建文件夹\\IDEA\\workspace\\myself-spring\\target\\classes\\com\\myself\\demo");
        System.out.println("======file="+directory);
        for(File file : directory.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                if(!file.getName().endsWith(".class"))continue;
                String clazzName = (scanPackage+"."+file.getName().replaceAll(".class",""));
                register.put(clazzName,null);
            }
        }
    }
}
