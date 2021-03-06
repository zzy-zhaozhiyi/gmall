package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author zzy
 * @create 2019-12-14 22:11
 */
@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
