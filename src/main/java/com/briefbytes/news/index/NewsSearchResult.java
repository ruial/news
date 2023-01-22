package com.briefbytes.news.index;

import com.briefbytes.news.model.News;

public record NewsSearchResult(News news, double relevance) { }
