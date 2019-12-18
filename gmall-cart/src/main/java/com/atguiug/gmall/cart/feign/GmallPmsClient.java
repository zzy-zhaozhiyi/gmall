package com.atguiug.gmall.cart.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author zzy
 * @create 2019-12-14 22:09
 */
@FeignClient("pms-service")
public interface GmallPmsClient  extends GmallPmsApi {
}
