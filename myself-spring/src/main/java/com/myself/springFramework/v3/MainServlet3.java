package com.myself.springFramework.v3;

import com.myself.springFramework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class MainServlet3 extends HttpServlet {
    private static final String PARAMNAME = "initConfigParam";
    private static final String SCANPACKAGE = "scan.package";

    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList<String>();
    private Map<String,Object> ioc = new HashMap<String,Object>(8);
    private Map<String,Method> handlerMapping = new HashMap<String, Method>(8);
    private List<HandlerMapping> register = new ArrayList<>();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String url = requestURI.replace(contextPath, "");
        HandlerMapping handler = getHandler(url);
        if(null == handler){
            resp.getWriter().write("404 not found");
            return;
        }
        Map<String, Integer> paramIndexMapping = handler.getParamIndexMapping();
        Object[] submitData = new Object[paramIndexMapping.size()];
        Map<String,String[]> parameterMap = req.getParameterMap();
        for(Map.Entry<String,Integer> entry : paramIndexMapping.entrySet()){
            Integer value = entry.getValue();
            if(entry.getKey().equals(HttpServletRequest.class.getName())){
                submitData[value] = req;
                continue;
            }else if(entry.getKey().equals(HttpServletResponse.class.getName())){
                submitData[value] = resp;
                continue;
            }
            submitData[value] = Arrays.toString(parameterMap.get(entry.getKey())).replaceAll("\\[|\\]","").replaceAll(",\\s+",",");
        }
        try {
            handler.getMethod().invoke(handler.getController(),submitData);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        /*if(!handlerMapping.containsKey(url)){
            resp.getWriter().write("404 not found");
            return;
        }
        Method method = handlerMapping.get(url);
        Map<String,String[]> parameterMap = req.getParameterMap();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] submitData = new Object[parameterTypes.length];
        for(int i=0;i<parameterTypes.length;i++){
            if(parameterTypes[i] == HttpServletRequest.class){
                submitData[i] = req;
                continue;
            }else if(parameterTypes[i] == HttpServletResponse.class){
                submitData[i] = resp;
                continue;
            }else if(parameterTypes[i] == String.class){
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                for(int j=0;j<parameterAnnotations.length;j++){
                    for(Annotation a : parameterAnnotations[j]){
                        if(a instanceof GPRequestParam){
                            String value = ((GPRequestParam) a).value();
                            String data = Arrays.toString(parameterMap.get(value)).replaceAll("\\[|\\]","");
                            System.out.println("data="+data);
                            submitData[i] =data.replaceAll(",\\s+",",");
                                    System.out.println("submit[i]:"+submitData[i]);
                        }
                    }
                }
            }
        }
        System.out.println("submitData="+Arrays.toString(submitData));
        try {
            method.invoke(ioc.get(method.getDeclaringClass().getName()),submitData);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }*/
    }

    private HandlerMapping getHandler(String url) {
        for(HandlerMapping handler : register){
            if(url.equals(handler.getUrl())){
                return handler;
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置
        doLoadResource(config.getInitParameter(PARAMNAME));
        //扫描对应的包类
        doScannerPackage(contextConfig.getProperty(SCANPACKAGE));
        //保存对应的包名
        doInstance();
        //依赖注入
        doAutowired();
        //初始化HandlerMapping
        initHandlerMapping();
        System.out.println("Spring servlet init end!");
    }

    private void initHandlerMapping() {
        if(ioc.isEmpty())return;
        for (Map.Entry<String,Object> entry:ioc.entrySet()) {
            Object value = entry.getValue();
            Class<?> clazz = value.getClass();
            if(clazz.isAnnotationPresent(GPController.class)){
                GPRequestMapping gpRequestMapping = clazz.getAnnotation(GPRequestMapping.class);
                String baseUrl = gpRequestMapping.value();
                Method[] methods = clazz.getMethods();
                for(Method method : methods){
                    if(method.isAnnotationPresent(GPRequestMapping.class)){
                        String url = ("/"+baseUrl+"/"+method.getAnnotation(GPRequestMapping.class).value()).replaceAll("/+","/");
//                        handlerMapping.put(url,method);
                        register.add(new HandlerMapping(url,method,value));
                        System.out.println("Mapped:"+url+","+method);
                    }
                }
            }
        }
    }

    private void doAutowired()  {
        if(ioc.isEmpty())return;
        for(Map.Entry<String,Object> entry : ioc.entrySet()){
            Object value = entry.getValue();
            Class<?> clazz = value.getClass();
            if(clazz.isAnnotationPresent(GPController.class)){
                Field[] fields = clazz.getDeclaredFields();
                for (Field field: fields) {
                    if(field.isAnnotationPresent(GPAutowired.class)){
                        Class<?> type = field.getType();
                        field.setAccessible(true);
                        try {
                            field.set(value,ioc.get(type.getName()));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void doInstance() {
        if(classNames.isEmpty())return;
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(GPController.class)) {
                    Object instance = clazz.newInstance();
                    ioc.put(clazz.getName(), instance);
                } else if (clazz.isAnnotationPresent(GPService.class)) {
                    GPService gpService = clazz.getAnnotation(GPService.class);
                    String beanName = gpService.value();
                    if ("".equals(beanName)) beanName = clazz.getName();
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    for (Class cla : clazz.getInterfaces()) {
                        if (ioc.containsKey(cla.getName())) throw new RuntimeException("此" + cla.getName() + "已存在");
                        ioc.put(cla.getName(), instance);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void doScannerPackage(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        try {
            String urlResouce = URLDecoder.decode(url.getFile(), "UTF-8");
            File files = new File(urlResouce);
            System.out.println("files:"+urlResouce+",url="+url.getFile());
            for (File file:files.listFiles()) {
                String fileName = file.getName();
                if(file.isDirectory()){
                    doScannerPackage(scanPackage+"."+fileName);
                }else{
                    if(!fileName.endsWith(".class"))continue;
                    String className = scanPackage+"."+fileName.replace(".class","");
                    this.classNames.add(className);
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void doLoadResource(String location) {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            contextConfig.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != in){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class HandlerMapping{
        private String url;
        private Method method;
        private Object controller;
        private Map<String,Integer> paramIndexMapping;

        public String getUrl() {
            return url;
        }

        public Method getMethod() {
            return method;
        }

        public Object getController() {
            return controller;
        }

        public Map<String, Integer> getParamIndexMapping() {
            return paramIndexMapping;
        }

        public HandlerMapping(String url, Method method, Object controller) {
            this.url = url;
            this.method = method;
            this.controller = controller;
            paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
            System.out.println("init data="+paramIndexMapping);
        }

        private void putParamIndexMapping(Method method) {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for(int i=0;i<parameterAnnotations.length;i++){
                for(Annotation a : parameterAnnotations[i]){
                    if(a instanceof GPRequestParam){
                        String value = ((GPRequestParam) a).value();
                        paramIndexMapping.put(value,i);
                    }
                }
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            for(int i=0;i<parameterTypes.length;i++){
                Class<?> clazz = parameterTypes[i];
                if(clazz == HttpServletRequest.class){
                    paramIndexMapping.put(clazz.getName(),i);
                }else if(clazz == HttpServletResponse.class){
                    paramIndexMapping.put(clazz.getName(),i);
                }
            }
        }

    }
}
