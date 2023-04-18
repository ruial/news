package com.briefbytes.news.seed;

import com.briefbytes.news.downloader.Downloader;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public record SeedList(List<SeedDTO> seeds) {
    public record SeedDTO(String seedName, String seedUrl, String type, String subreddit,
                          String feedUrl, boolean fetchContent) {
    }

    public List<Seed> getSeeds(Downloader downloader) {
        return seeds.stream().map(seedDTO -> switch (seedDTO.type) {
            case "hackernews" -> new HackerNews(downloader);
            case "reddit" -> new Reddit(seedDTO.subreddit, downloader);
            case "rss" -> new RssFeed(seedDTO.seedName, seedDTO.seedUrl,
                    seedDTO.feedUrl, downloader, seedDTO.fetchContent);
            default -> throw new IllegalArgumentException("Invalid seed type: " + seedDTO.type);
        }).collect(Collectors.toList());
    }

    public static SeedList fromFile(String path) throws IOException {
        return new ObjectMapper().readValue(Paths.get(path).toFile(), SeedList.class);
    }
}
