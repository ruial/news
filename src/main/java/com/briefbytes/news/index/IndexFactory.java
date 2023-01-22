package com.briefbytes.news.index;

import java.io.IOException;

public interface IndexFactory {
    Index createIndex() throws IOException;
}
