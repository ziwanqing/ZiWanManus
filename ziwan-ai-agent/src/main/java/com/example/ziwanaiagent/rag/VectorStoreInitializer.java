package com.example.ziwanaiagent.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 初始化向量知识库
 */
//@Configuration
@RequiredArgsConstructor
@Slf4j
public class VectorStoreInitializer {

    private final VectorStore pgVectorVectorStore;
    private final LoveAppDocumentLoader loveAppDocumentLoader;
    private final JdbcTemplate jdbcTemplate;  // 注入 JdbcTemplate

    @Bean
    public ApplicationRunner initVectorStoreRunner() {

        return args -> {

            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.vector_store", Long.class
            );
            // 1️⃣ 检查向量库是否已有数据
            if (count != null && count > 0) {
                log.info("✅ 向量库已有 {} 条文档，跳过初始化", count);
                return;
            }
            log.info("🚀 向量库为空，开始初始化文档向量化...");


            List<Document> documents = loveAppDocumentLoader.loadMarkdowns();

            int batchSize = 25; // DashScope 限制
            for (int i = 0; i < documents.size(); i += batchSize) {
                int end = Math.min(i + batchSize, documents.size());
                List<Document> batch = documents.subList(i, end);

                pgVectorVectorStore.add(batch);

                log.info("📌 已插入向量文档 {} 条", batch.size());
            }

            log.info("🎉 全部向量初始化完成，总计：{} 条", documents.size());
        };
    }
}
