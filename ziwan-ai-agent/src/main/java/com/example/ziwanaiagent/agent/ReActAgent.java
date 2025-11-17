package com.example.ziwanaiagent.agent;

import com.example.ziwanaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct 抽象：子类实现 think() 与 act()。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /** 返回是否需要执行 act() */
    public abstract boolean think();

    /** 执行 act 并返回结果摘要 */
    public abstract String act();

    @Override
    public String step() {
        try {
            if (!think()) {
                setState(AgentState.FINISHED);
                return "Thought completed — no action required";
            }
            return act();
        } catch (Exception e) {
            log.error("Error in ReAct step: {}", e.getMessage(), e);
            return "Error in ReAct step: " + e.getMessage();
        }
    }
}
