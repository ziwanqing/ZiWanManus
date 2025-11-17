package com.example.ziwanaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class ZiWanManusTest {
    @Resource
    private ZiWanManus ziWanManus;
    @Test
    public void run() {
        String userPrompt = """
                制定一份详细的约会计划，
                并以 PDF 格式输出""";
        String result = ziWanManus.run(userPrompt);
        Assertions.assertNotNull( result);
    }

}