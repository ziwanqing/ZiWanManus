package com.example.ziwanaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.example.ziwanaiagent.agent.model.AgentState;
import com.example.ziwanaiagent.exception.ErrorCode;
import com.example.ziwanaiagent.exception.ThrowUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
public abstract class BaseAgent {

    // --- 循环检测配置 ---
    private int duplicateThreshold = 4;  // 连续重复次数阈值
    private int checkRange = 6;         // 回溯窗口大小
    private int stuckCount = 0;         // 累积 stuck 次数

    // 核心属性
    private String name;
    private String systemPrompt;
    private String nextStepPrompt;
    private AgentState state = AgentState.IDLE;
    private int currentStep = 0;
    private int maxSteps = 10;
    private ChatClient chatClient;
    private List<Message> messageList = new ArrayList<>();

    /**
     * 执行代理流程,运行代理
     */
    public String run(String userPrompt) {
        ThrowUtils.throwIf(this.state != AgentState.IDLE, ErrorCode.OPERATION_ERROR, "Cannot run agent from state :" + this.state);
        ThrowUtils.throwIf(StrUtil.isBlank(userPrompt), ErrorCode.PARAM_ERROR, "userPrompt cannot be empty");

        this.state = AgentState.RUNNING;
        messageList.add(new UserMessage(userPrompt));

        List<String> resultList = new ArrayList<>();

        try {
            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++;
                log.info("Running step {}, total step {}", currentStep, maxSteps);

                // 单步执行
                String stepResult = step();
                String result = "Step:" + currentStep + ":" + stepResult;
                resultList.add(result);

                // 检测循环
                if (isStuck()) {
                    stuckCount++;
                    log.warn("Detected potential loop at step {}, stuckCount={}", currentStep, stuckCount);
                    resultList.add("Warning: Detected loop, step " + currentStep);

                    if (stuckCount >= duplicateThreshold) {
                        log.error("Agent terminated due to repeated loop detection.");
                        resultList.add("Terminated: Repeated loop detected.");
                        this.state = AgentState.FINISHED;
                        break;
                    }
                } else {
                    // 如果未检测到循环，重置 stuckCount
                    stuckCount = 0;
                }
            }

            if (currentStep >= maxSteps) {
                this.state = AgentState.FINISHED;
                resultList.add("Terminated: Reached max step (" + maxSteps + ")");
            }

            return String.join("\n", resultList);

        } catch (Exception e) {
            this.state = AgentState.ERROR;
            log.error("Error running agent: {}", e.getMessage(), e);
            return "Error running agent:" + e.getMessage();
        } finally {
            this.clear();
        }
    }


    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果的 SSE 流
     */
    public SseEmitter runStream(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(300_000L); // 5 分钟超时

        // 异步 执行
        CompletableFuture.runAsync(() -> {
            try {
                // --- 基础校验 ---
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：提示词不能为空");
                    sseEmitter.complete();
                    return;
                }

                // --- 初始化 ---
                this.state = AgentState.RUNNING;
                messageList.add(new UserMessage(userPrompt));
                stuckCount = 0;

                // --- 循环执行步骤 ---
                while (currentStep < maxSteps && state != AgentState.FINISHED) {
                    currentStep++;
                    log.info("Executing step {}/{}", currentStep, maxSteps);

                    String stepResult = step();
                    String result = "Step " + currentStep + ": " + stepResult;
                    sseEmitter.send(result);

                    // 检查循环
                    if (isStuck()) {
                        stuckCount++;
                        String warnMsg = "Warning: Detected loop at step " + currentStep;
                        sseEmitter.send(warnMsg);
                        log.warn(warnMsg);

                        if (stuckCount >= duplicateThreshold) {
                            state = AgentState.FINISHED;
                            String terminateMsg = "Terminated: Repeated loop detected.";
                            sseEmitter.send(terminateMsg);
                            log.error(terminateMsg);
                            break;
                        }
                    } else {
                        stuckCount = 0;
                    }
                }

                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    String maxStepMsg = "Terminated: Reached max steps (" + maxSteps + ")";
                    sseEmitter.send(maxStepMsg);
                }

                sseEmitter.complete();

            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent", e);
                try {
                    sseEmitter.send("执行错误：" + e.getMessage());
                } catch (Exception ex) {
                    log.error("Error sending SSE message", ex);
                }
                sseEmitter.completeWithError(e);
            } finally {
                clear(); // 清理资源
            }
        });

        // SSE 超时回调
        sseEmitter.onTimeout(() -> {
            state = AgentState.ERROR;
            clear();
            log.warn("SSE connection timeout");
        });

        // SSE 完成回调
        sseEmitter.onCompletion(() -> {
            if (state == AgentState.RUNNING) {
                state = AgentState.FINISHED;
            }
            clear();
            log.info("SSE connection completed");
        });

        return sseEmitter;
    }

    /**
     * 循环检测
     * 检查最近 checkRange 条 assistant 消息中是否有重复回答
     */
    protected boolean isStuck() {
        if (messageList.size() < 2) return false;

        List<String> recentMessages = getStrings();

        // 检查重复
        for (int i = 0; i < recentMessages.size() - duplicateThreshold + 1; i++) {
            boolean allSame = true;
            String msg = recentMessages.get(i);
            for (int j = 1; j < duplicateThreshold; j++) {
                if (!msg.equals(recentMessages.get(i + j))) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) return true;
        }

        return false;
    }

    @NotNull
    private List<String> getStrings() {
        int start = Math.max(0, messageList.size() - checkRange);
        List<String> recentMessages = new ArrayList<>();
        for (int i = start; i < messageList.size(); i++) {
            Message m = messageList.get(i);

            // 判断是否是 Assistant 消息
            if (m instanceof org.springframework.ai.chat.messages.AssistantMessage) {
                // 获取内容
                String content = ((org.springframework.ai.chat.messages.AssistantMessage) m).getText();
                recentMessages.add(content);
            }
        }
        return recentMessages;
    }


    public abstract String step();

    protected void clear() {
        messageList.clear();
        currentStep = 0;
        stuckCount = 0;
    }
}
