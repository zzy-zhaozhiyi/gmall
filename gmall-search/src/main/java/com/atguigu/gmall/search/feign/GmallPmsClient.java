package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author zzy
 * @create 2019-12-10 10:19
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
