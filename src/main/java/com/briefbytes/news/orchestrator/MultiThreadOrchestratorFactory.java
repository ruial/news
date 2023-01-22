package com.briefbytes.news.orchestrator;

import com.briefbytes.news.downloader.Downloader;
import com.briefbytes.news.index.Index;
import com.briefbytes.news.seed.Seed;

import java.util.List;

public class MultiThreadOrchestratorFactory implements OrchestratorFactory {

    private Index index;
    private Downloader downloader;
    private List<Seed> seeds;
    private int retentionDays;

    public MultiThreadOrchestratorFactory(Index index, Downloader downloader, List<Seed> seeds, int retentionDays) {
        this.index = index;
        this.downloader = downloader;
        this.seeds = seeds;
        this.retentionDays = retentionDays;
    }

    @Override
    public Orchestrator createOrchestrator() {
        return new MultiThreadOrchestrator(index, downloader, seeds, retentionDays);
    }

}
