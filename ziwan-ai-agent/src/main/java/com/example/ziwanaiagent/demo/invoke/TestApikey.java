package com.example.ziwanaiagent.demo.invoke;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public interface TestApikey {

    @Value("${spring.ai.dashscope.api-key}")
    String API_KEY = "";


}
