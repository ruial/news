package com.briefbytes.news.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class CloseableUtils {
    private static final Logger LOG = LoggerFactory.getLogger(CloseableUtils.class);

    public static void closeQuietly(Closeable ...closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) {
                    c.close();
                    LOG.info("Closed: " + c.getClass().getSimpleName());
                }
            } catch (IOException e) {
                LOG.error("Unable to close: " + c.getClass().getSimpleName(), e);
            }
        }
    }

}
