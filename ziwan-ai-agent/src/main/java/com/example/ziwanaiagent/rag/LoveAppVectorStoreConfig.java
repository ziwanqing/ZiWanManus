package com.example.ziwanaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * 向量知识库配置
 */
//@Configuration
@Profile("!test")
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;


    @Resource
    private MyKeywordEnricher myKeywordEnricher;
    
    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载文档
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();

        // 添加关键词
        List<Document> documentList = myKeywordEnricher.enrichDocuments(documents);


        simpleVectorStore.add(documentList);



        return simpleVectorStore;
    }
}
