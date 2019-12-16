package com.atguigu.gmall.auth.feign;

import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author zzy
 * @create 2019-12-16 20:31
 */
@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
