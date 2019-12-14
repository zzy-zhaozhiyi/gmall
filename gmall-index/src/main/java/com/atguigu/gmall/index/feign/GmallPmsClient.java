package com.atguigu.gmall.index.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author zzy
 * @create 2019-12-13 18:31
 */
@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {

}
