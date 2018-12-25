package com.wangyuxuan.dubbo.http.controller;

import com.alibaba.fastjson.JSON;
import com.wangyuxuan.dubbo.http.conf.HttpProviderConf;
import com.wangyuxuan.dubbo.http.req.HttpRequest;
import com.wangyuxuan.dubbo.http.rsp.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Auther: wangyuxuan
 * @Date: 2018/12/25 10:07
 * @Description:
 */

@Slf4j
@Controller
@RequestMapping("/dubboAPI")
public class DubboController implements ApplicationContextAware {

    protected ApplicationContext applicationContext;

    /**
     * 缓存 Map
     */
    private final Map<String, Class<?>> cacheMap = new HashMap<>();

    @Autowired
    private HttpProviderConf httpProviderConf;

    @ResponseBody
    @RequestMapping(value = "/{service}/{method}", method = RequestMethod.POST)
    public String api(HttpRequest httpRequest, HttpServletRequest request,
                      @PathVariable String service,
                      @PathVariable String method) {
        log.debug("ip:{}-httpRequest:{}", getIP(request), JSON.toJSONString(httpRequest));
        String invoke = invoke(httpRequest, service, method);
        log.debug("callback :{}", invoke);
        return invoke;
    }

    private String invoke(HttpRequest httpRequest, String service, String method) {
        httpRequest.setService(service);
        httpRequest.setMethod(method);

        HttpResponse response = new HttpResponse();

        log.debug("input param:{}", JSON.toJSONString(httpRequest));

        if (!CollectionUtils.isEmpty(httpProviderConf.getUsePackage())) {
            boolean isPac = false;
            for (String pac : httpProviderConf.getUsePackage()) {
                if (service.startsWith(pac)) {
                    isPac = true;
                    break;
                }
            }
            if (!isPac) {
                //调用的是未经配置的包
                log.error("service is not correct,service={}", service);
                response.setSuccess(false);
                response.setCode("2");
                response.setDescription("service is not correct,service=" + service);
                return JSON.toJSONString(response);
            }
        }

        try {
            Class<?> serviceCla = cacheMap.get(service);
            if(serviceCla == null){
                serviceCla = Class.forName(service);
                log.debug("serviceCla:{}", JSON.toJSONString(serviceCla));
                //设置缓存
                cacheMap.put(service, serviceCla);
            }
            Method[] methods = serviceCla.getMethods();
            Method targetMethod = null;
            for (Method m : methods){
                if(m.getName().equals(method)){
                    targetMethod = m;
                    break;
                }
            }

            if(targetMethod == null){
                //不包含被调用的方法
                log.error("method is not correct,method={}", method);
                response.setCode("2");
                response.setSuccess(false);
                response.setDescription("method is not correct,method=" + method);
                return JSON.toJSONString(response);
            }

            Object bean = this.applicationContext.getBean(serviceCla);
            Object result = null;
            Class<?>[] parameterTypes = targetMethod.getParameterTypes();
            if(parameterTypes.length == 0){
                //没有参数
                result = targetMethod.invoke(bean);
            }else if(parameterTypes.length == 1){
                //这里最多只运行一个参数。也就是说在真正的dubbo调用的时候只能传递一个BO类型，具体的参数列表可以写到BO中
                Object json = JSON.parseObject(httpRequest.getParam(), parameterTypes[0]);
                result = targetMethod.invoke(bean, json);
            }else {
                //多个参数在进行json解析的时候是无法赋值到多个参数对象中去的
                log.error("Can only have one parameter");
                response.setSuccess(false);
                response.setCode("2");
                response.setDescription("Can only have one parameter");
                return JSON.toJSONString(response);
            }
            return JSON.toJSONString(result);
        } catch (ClassNotFoundException e) {
            log.error("class not found", e);
            response.setSuccess(false);
            response.setCode("2");
            response.setDescription("class not found");
        } catch (InvocationTargetException e) {
            log.error("InvocationTargetException", e);
            response.setSuccess(false);
            response.setCode("2");
            response.setDescription("InvocationTargetException");
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException", e);
            response.setSuccess(false);
            response.setCode("2");
            response.setDescription("IllegalAccessException");
        }
        return JSON.toJSONString(response);
    }

    /**
     * 获取IP
     *
     * @param request
     * @return
     */
    private String getIP(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String s = request.getHeader("X-Forwarded-For");
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s)) {

            s = request.getHeader("Proxy-Client-IP");
        }
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s)) {

            s = request.getHeader("WL-Proxy-Client-IP");
        }
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s)) {
            s = request.getHeader("HTTP_CLIENT_IP");
        }
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s)) {

            s = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (s == null || s.length() == 0 || "unknown".equalsIgnoreCase(s)) {

            s = request.getRemoteAddr();
        }
        if ("127.0.0.1".equals(s) || "0:0:0:0:0:0:0:1".equals(s)) {
            try {
                s = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException unknownhostexception) {
                return "";
            }
        }
        return s;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
