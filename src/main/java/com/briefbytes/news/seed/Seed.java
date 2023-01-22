package com.briefbytes.news.seed;

import com.briefbytes.news.model.News;

import java.util.Objects;

public abstract class Seed implements Pollable<News> {

    private String seedName;
    private String seedUrl;

    public Seed(String seedName, String seedUrl) {
        Objects.requireNonNull(seedName, "Seed name is required");
        Objects.requireNonNull(seedUrl, "Seed url is required");
        this.seedName = seedName;
        this.seedUrl = seedUrl;
    }

    public String getSeedName() {
        return seedName;
    }

    public String getSeedUrl() {
        return seedUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seed seed = (Seed) o;
        return seedName.equals(seed.seedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seedName);
    }

    @Override
    public String toString() {
        return "Seed{" +
                "seedName='" + seedName + '\'' +
                ", seedUrl='" + seedUrl + '\'' +
                '}';
    }
}
