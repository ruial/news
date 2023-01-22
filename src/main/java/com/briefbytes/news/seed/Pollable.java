package com.briefbytes.news.seed;

import java.io.IOException;
import java.util.List;

public interface Pollable<T> {
    Integer MAX_ITEMS_DEFAULT = 100;
    List<T> poll() throws IOException;
}
