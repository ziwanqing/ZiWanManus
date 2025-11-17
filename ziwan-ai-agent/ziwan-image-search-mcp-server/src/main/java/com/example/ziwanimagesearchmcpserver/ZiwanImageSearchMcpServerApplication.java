package com.example.ziwanimagesearchmcpserver;

import com.example.ziwanimagesearchmcpserver.tools.ImageSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ZiwanImageSearchMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZiwanImageSearchMcpServerApplication.class, args);
    }


    @Bean
    public ToolCallbackProvider imageSearchTool(ImageSearchTool imageSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageSearchTool)
                .build();
    }

}
