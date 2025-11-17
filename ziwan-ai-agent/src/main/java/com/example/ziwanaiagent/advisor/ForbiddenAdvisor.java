

package com.example.ziwanaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;


@Slf4j
public class ForbiddenAdvisor implements CallAdvisor, StreamAdvisor {




    // 定义敏感词列表
    private final List<String> forbiddenWords = Arrays.asList(
            "暴力", "色情", "赌博", "毒品", "诈骗", "反动"
    );


    @NotNull
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private ChatClientRequest before(ChatClientRequest request) {
        String text = request.prompt().getUserMessage().getText();
        if (containsForbiddenWords(text)){
            return new ChatClientRequest(new Prompt("你的输入包含敏感词"), request.context());
        }
        return request;
    }

    private void observeAfter(ChatClientResponse chatClientResponse) {
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        return chain.nextCall( before(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        Flux<ChatClientResponse> chatClientResponseFlux = chain.nextStream(before(chatClientRequest));
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponseFlux, this::observeAfter);
    }



    /**
     * 检查输入是否包含敏感词
     */
    private boolean containsForbiddenWords(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        for (String word : forbiddenWords) {
            if (input.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
