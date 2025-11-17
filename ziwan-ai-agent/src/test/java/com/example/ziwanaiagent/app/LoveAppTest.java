package com.example.ziwanaiagent.app;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void doChat() {
        //第一轮
        String message = "你好，我是紫菀";
        String chatId = UUID.randomUUID().toString();
        String content = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(content);

        //第二轮
        message = "我想让另一半更爱我，她叫（冰雹）";
        content = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(content);

        //第三轮
        message = "我的对象叫什么名字";
        content = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(content);


    }

    @Test
    void doChatByStream() {
    }

    @Test
    void doChatWithReport() {
        String message = "我想让另一半更爱我，她叫（冰雹）";
        String chatId = UUID.randomUUID().toString();
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void getSystemResource() {
    }

    @Test
    void doChatWithRag() {

        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);


    }

    @Test
    void doChatWithTools() {
        String chatId = UUID.randomUUID().toString();
        String message = "我的伴侣喜欢dog，请查找关于dog的图片，然后用PDF格式保存";
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }
}