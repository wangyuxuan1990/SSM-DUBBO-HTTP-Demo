package com.wangyuxuan.dubbo.http.rsp;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @Auther: wangyuxuan
 * @Date: 2018/12/25 10:02
 * @Description: 响应
 */

@Getter
@Setter
public class HttpResponse implements Serializable {

    private static final long serialVersionUID = 4661452136014800895L;

    private boolean success;//成功标志

    private String code;//信息码

    private String description;//描述
}
