package com.example.ziwanaiagent.app;


import com.example.ziwanaiagent.advisor.LoggerAdvisor;
import com.example.ziwanaiagent.chatmemory.FileBasedChatMemory;
import com.example.ziwanaiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.example.ziwanaiagent.rag.QueryRewriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;


    // 从类路径资源加载系统提示模板
    @Getter
    private final Resource systemResource;

    private final String SYSTEM_PROMPT;


    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel 模型
     */
    public LoveApp(ChatModel dashscopeChatModel, @Value("classpath:prompts/system-message.md") Resource systemResource) {
        this.systemResource = systemResource;
        // 加载外部 system prompt 文件
        try {
            this.SYSTEM_PROMPT = new String(systemResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
//        ChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(10)
//                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new LoggerAdvisor()
//                        new ForbiddenAdvisor()
                )
                .build();


    }


    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message 用户输入
     * @param chatId  用户对话 ID
     * @return AI 输出
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
        }
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message 用户输入
     * @param chatId  用户对话 ID
     * @return AI 输出
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }


    public record LoveReport(String title, List<String> suggestions) {

    }


    /**
     * AI 恋爱报告功能（实战结构化输出）
     *
     * @param message 用户输入
     * @param chatId  用户对话 ID
     * @return 恋爱报告
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system( this.SYSTEM_PROMPT +"每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

// AI 恋爱知识库问答功能

    @jakarta.annotation.Resource
    private VectorStore loveAppVectorStore;

//    @jakarta.annotation.Resource
//    private Advisor loveAppRagCloudAdvisor;

//    @jakarta.annotation.Resource
//    private VectorStore pgVectorVectorStore;

//    @jakarta.annotation.Resource
//    private QueryRewriter queryRewriter;



    /**
     * 和 RAG 知识库进行对话
     *
     * @param message 用户输入
     * @param chatId  用户对话 ID
     * @return AI 输出
     */
    public String doChatWithRag(String message, String chatId) {


        // 查询重写
//        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
//                .advisors(new LoggerAdvisor())
                // 应用 RAG 知识库问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库服务）
//                .advisors(loveAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                pgVectorVectorStore, "单身"
//                        )
//                )
                .call()
                .chatResponse();
        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
        }
        log.info("content: {}", content);
        return content;
    }


    // AI 调用工具能力
    @jakarta.annotation.Resource
    private ToolCallback[] allTools;

    /**
     * AI 恋爱报告功能（支持调用工具）
     *
     * @param message 用户输入
     * @param chatId  用户对话 ID
     * @return AI 输出
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new LoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
        }
        log.info("content: {}", content);
        return content;
    }




    @jakarta.annotation.Resource
    private ToolCallbackProvider toolCallbackProvider;


    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new LoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
        }
        log.info("content: {}", content);
        return content;
    }






}
