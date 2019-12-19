package com.atguigu.gmall.auth.config;

import lombok.Data;
import lombok.ToString;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
//@Component
@ConfigurationProperties(prefix = "sms")
@ToString
@Data //设置get/set方法
public class SmsTemplate {
	//@Value("${sms.host}")
	String host;
	//@Value("${sms.path}")
	String path;
	//@Value("${sms.method}")
	String method;
	//@Value("${sms.appcode}")
	String appcode;
	//	"TP1711063"
	//发送短信的模板方法
	public boolean sendSms(String phoneNum , String code , String template) {
		System.out.println("host:"+ host);
	    Map<String, String> headers = new HashMap<String, String>();
	    //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
	    headers.put("Authorization", "APPCODE " + appcode);
	    Map<String, String> querys = new HashMap<String, String>();
	    querys.put("mobile", phoneNum);
	    querys.put("param", "code:"+code);
	    querys.put("tpl_id", template);
	    Map<String, String> bodys = new HashMap<String, String>();
	    try {
	    	/**
	    	* 重要提示如下:
	    	* HttpUtils请从
	    	* https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
	    	* 下载
	    	*
	    	* 相应的依赖请参照
	    	* https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
	    	*/
	    	HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
	    	System.out.println(response.toString());
	    	//获取response的body
	    	//String str = EntityUtils.toString(response.getEntity());
	    	//System.out.println("短信发送后的响应："+ str);
	    	System.out.println(EntityUtils.toString(response.getEntity()));
	    	return true; //发送成功
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	return false;//发送失败
	    }
	}
}
