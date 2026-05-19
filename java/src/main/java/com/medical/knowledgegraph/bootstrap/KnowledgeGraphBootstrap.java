package com.medical.knowledgegraph.bootstrap;

import com.medical.config.MedicalGraphProperties;
import com.medical.service.sync.SymptomIcdSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class KnowledgeGraphBootstrap implements ApplicationRunner {

    private final MedicalGraphProperties graphProperties;
    private final KnowledgeGraphSeedData seedData;
    private final SymptomIcdSyncService symptomIcdSyncService;

    @Override
    public void run(ApplicationArguments args) {
        if (!graphProperties.isBootstrapOnStartup()) {
            return;
        }
        try {
            seedData.seedIfNeeded();
            if (graphProperties.isSyncToRdbOnStartup()) {
                symptomIcdSyncService.syncFromNeo4j();
            }
        } catch (Exception e) {
            log.warn("知识图谱启动初始化失败（Neo4j 可能未就绪）: {}", e.getMessage());
        }
    }
}
