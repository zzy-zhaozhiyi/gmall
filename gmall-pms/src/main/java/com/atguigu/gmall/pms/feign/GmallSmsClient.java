package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author zzy
 * @create 2019-12-05 15:01
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {


}
