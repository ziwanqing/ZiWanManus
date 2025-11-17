package com.example.ziwanaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 线程安全 + 自动修复 + 版本校验 的 Kryo 文件型 ChatMemory 实现
 * 支持并发访问、多会话隔离与类结构变更检测
 */
@Slf4j
public class FileBasedChatMemory implements ChatMemory {

    private final String BASE_DIR;
    private static final String FILE_MAGIC = "KRYO_V1";

    /** 每个 conversationId 使用独立锁，避免并发写入损坏文件 */
    private final ConcurrentHashMap<String, Object> fileLocks = new ConcurrentHashMap<>();

    private Object getLock(String conversationId) {
        return fileLocks.computeIfAbsent(conversationId, id -> new Object());
    }

    /** Kryo 实例线程隔离 */
    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        // 注册常用类（防止 HashMap 或内部类反序列化失败）
        kryo.register(ArrayList.class);
        kryo.register(HashMap.class);
        kryo.register(UserMessage.class);
        kryo.register(AssistantMessage.class);
        kryo.register(SystemMessage.class);
        kryo.register(Message.class);

        return kryo;
    });

    private static Kryo getKryo() {
        return kryoThreadLocal.get();
    }

    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new RuntimeException("创建目录失败：" + dir);
        }
    }

    // ---------------- ChatMemory 接口实现 ---------------- //

    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        synchronized (getLock(conversationId)) {
            List<Message> messageList = load(conversationId);
            messageList.addAll(messages);
            save(conversationId, messageList);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        synchronized (getLock(conversationId)) {
            List<Message> messageList = load(conversationId);
            return messageList.stream()
                    .skip(Math.max(messageList.size() - 10, 0))
                    .toList();
        }
    }

    @Override
    public void clear(String conversationId) {
        File file = getFile(conversationId);
        synchronized (getLock(conversationId)) {
            if (file.exists() && !file.delete()) {
                log.warn("删除文件失败：{}", file.getAbsolutePath());
            }
        }
    }

    // ---------------- 内部实现 ---------------- //

    private void save(String conversationId, List<Message> messages) {
        File file = getFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            output.writeString(FILE_MAGIC); // 文件头标记
            getKryo().writeObject(output, messages);
        } catch (IOException e) {
            log.error("保存会话 [{}] 消息失败：{}", conversationId, e.getMessage(), e);
        }
    }

    private List<Message> load(String conversationId) {
        File file = getFile(conversationId);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        List<Message> messages = new ArrayList<>();
        try (Input input = new Input(new FileInputStream(file))) {
            String magic = input.readString();
            if (!FILE_MAGIC.equals(magic)) {
                throw new IOException("非法文件版本或损坏");
            }
            messages = getKryo().readObject(input, ArrayList.class);
        } catch (Exception e) {
            log.warn("读取会话 [{}] 文件失败或损坏，删除重建：{}", conversationId, file.getName());
            file.delete();
        }
        return messages;
    }

    private File getFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
