package com.briefbytes.news.ui;

import com.briefbytes.news.index.Index;
import com.briefbytes.news.seed.Seed;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.vue.VueComponent;

import java.io.Closeable;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static io.javalin.apibuilder.ApiBuilder.*;

public class JavalinApp implements Closeable {

    private List<Seed> seeds;
    private Index index;
    private int port;
    private Duration timeout;
    private Javalin app;

    public JavalinApp(List<Seed> seeds, Index index, int port, Duration timeout) {
        this.seeds = seeds;
        this.index = index;
        this.port = port;
        this.timeout = timeout;
    }

    private boolean isDev(Context context) {
        return !getClass().getResource(getClass().getSimpleName() + ".class").getProtocol().equals("jar");
    }

    public void start() {
        // TODO expose prometheus metrics and healthcheck
        app = Javalin.create(config -> {
                    config.staticFiles.enableWebjars();
                    config.vue.vueAppName = "app";
                    config.vue.isDevFunction = this::isDev;
                })
                .routes(() -> {
                    get("/", new VueComponent("news-list"));
                    get("/news/{newsId}", new VueComponent("news-detail"));
                    get("/settings", new VueComponent("settings"));
                    get("/recommendations", new VueComponent("recommendations"));
                    get("/search", new VueComponent("search"));
                    path("/api", () -> {
                        get("/seeds", ctx -> ctx.json(seeds));
                        get("/seeds/{seed}", ctx -> {
                            String seed = ctx.pathParam("seed");
                            String count = ctx.queryParam("count");
                            String startDate = ctx.queryParam("startDate");
                            String endDate = ctx.queryParam("endDate");
                            try {
                                var news = index.latestNews(seed,
                                        Integer.parseInt(count),
                                        new Date(Long.parseLong(startDate)),
                                        new Date(Long.parseLong(endDate))
                                );
                                ctx.json(news);
                            } catch (NumberFormatException e) {
                                throw new BadRequestResponse("Invalid query parameters");
                            }
                        });
                        get("/news/{newsId}", ctx -> {
                            String newsId = ctx.pathParam("newsId");
                            var news = index.getNewsById(newsId);
                            if (news == null) throw new NotFoundResponse("News not found");
                            ctx.json(news);
                        });
                        get("/search/news", ctx -> {
                            String query = ctx.queryParam("query");
                            String count = ctx.queryParam("count");
                            String startDate = ctx.queryParam("startDate");
                            String endDate = ctx.queryParam("endDate");
                            try {
                                ctx.json(index.searchNews(query,
                                        Integer.parseInt(count),
                                        new Date(Long.parseLong(startDate)),
                                        new Date(Long.parseLong(endDate))));
                            } catch (NumberFormatException e) {
                                throw new BadRequestResponse("Invalid query parameters");
                            }
                        });
                        post("/recommendations/news", ctx -> {
                            String count = ctx.queryParam("count");
                            String startDate = ctx.queryParam("startDate");
                            String endDate = ctx.queryParam("endDate");
                            List<String> newsIds;
                            try {
                                newsIds = Arrays.asList(ctx.bodyAsClass(String[].class));
                            } catch (Exception e) {
                                throw new BadRequestResponse("Invalid request body");
                            }
                            try {
                                ctx.json(index.similarNews(newsIds,
                                        Integer.parseInt(count),
                                        new Date(Long.parseLong(startDate)),
                                        new Date(Long.parseLong(endDate))));
                            } catch (NumberFormatException e) {
                                throw new BadRequestResponse("Invalid query parameters");
                            }
                        });

                    });
                })
                .start(port);
        app.jettyServer().server().setStopTimeout(timeout.toMillis());
    }

    @Override
    public void close() {
        app.close();
    }
}
