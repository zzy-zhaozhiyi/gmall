package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author zzy
 * @create 2019-12-14 22:10
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
}
