package com.example.ziwanaiagent.controller;


import com.example.ziwanaiagent.agent.ZiWanManus;
import com.example.ziwanaiagent.app.LoveApp;
import com.example.ziwanaiagent.common.BaseResponse;
import com.example.ziwanaiagent.common.ResultUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;


    /**
     * 同步调用 AI 恋爱大师应用
     *
     * @param message 输入
     * @param chatId  对话 ID
     * @return 输出
     */
    @GetMapping("/love_app/chat/sync")
    public BaseResponse<String> doChatWithLoveAppSync(String message, String chatId) {
        String result = loveApp.doChat(message, chatId);
        return ResultUtils.success(result);
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message 输入
     * @param chatId  对话 ID
     * @return 输出流
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public BaseResponse<Flux<String>> doChatWithLoveAppSSE(String message, String chatId) {
        Flux<String> stringFlux = loveApp.doChatByStream(message, chatId);
        return ResultUtils.success(stringFlux);
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message 输入
     * @param chatId  对话 ID
     * @return 输出流
     */
    @GetMapping(value = "/love_app/chat/server_sent_event")
    public BaseResponse<Flux<ServerSentEvent<String>>> doChatWithLoveAppServerSentEvent(String message, String chatId) {
        Flux<ServerSentEvent<String>> map = loveApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
        return ResultUtils.success(map);
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message 输入
     * @param chatId  对话 ID
     * @return 输出流
     */
    @GetMapping(value = "/love_app/chat/sse_emitter")
    public BaseResponse<SseEmitter> doChatWithLoveAppServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        loveApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return ResultUtils.success(sseEmitter);
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message 输入
     * @return 输出流
     */
    @GetMapping("/manus/chat")
    public BaseResponse<SseEmitter> doChatWithManus(String message) {
        ZiWanManus ziWanManus = new ZiWanManus(allTools, dashscopeChatModel);
        SseEmitter sseEmitter = ziWanManus.runStream(message);
        return ResultUtils.success(sseEmitter);
    }
}
