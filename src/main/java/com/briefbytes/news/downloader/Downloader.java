package com.briefbytes.news.downloader;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.net.URL;

public interface Downloader {

    Safelist SAFELIST = Safelist.relaxed()
            .preserveRelativeLinks(false)
            .addEnforcedAttribute("a", "rel", "nofollow");

    default String clean(String text, String baseUri) {
        // this works well enough, otherwise could use Mozilla's JS readability library or a Java port
        return Jsoup.clean(text, baseUri, SAFELIST);
    }

    String download(URL url, boolean clean) throws IOException;
}
