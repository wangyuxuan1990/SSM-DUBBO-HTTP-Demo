package com.wangyuxuan.dubbo.http.req;

import lombok.Getter;
import lombok.Setter;

/**
 * @Auther: wangyuxuan
 * @Date: 2018/12/25 10:01
 * @Description: 请求
 */

@Getter
@Setter
public class HttpRequest {

    private String param ;//入参

    private String service ;//请求service

    private String method ;//请求方法
}
