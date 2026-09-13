package com.nus.iss.guide;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlatformFeatureIndexer {

    private static final Logger log = LoggerFactory.getLogger(PlatformFeatureIndexer.class);

    private final VectorStore vectorStore;

    public PlatformFeatureIndexer(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void indexPlatformFeatures() {
        List<Document> features = PlatformFeatureCatalog.documents();
        vectorStore.add(features);
        log.info("Indexed {} platform features", features.size());
    }
}
