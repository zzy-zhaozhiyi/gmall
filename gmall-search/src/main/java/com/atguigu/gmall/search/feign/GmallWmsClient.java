package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author zzy
 * @create 2019-12-10 10:20
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
