package com.briefbytes.news.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;

public class LuceneIndexFactory implements IndexFactory {

    private Path indexPath;

    public LuceneIndexFactory(String indexPath) {
        this.indexPath = indexPath == null ? null : Path.of(indexPath);
    }

    @Override
    public Index createIndex() throws IOException {
        Analyzer analyzer = new LuceneIndex.NewsAnalyzer();
        Directory directory = indexPath == null ? new ByteBuffersDirectory() : FSDirectory.open(indexPath);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        // IndexWriter is thread safe, cannot have multiple open due to lock
        // Could have multiple index reader processes but would need to periodically call search manager refresh
        IndexWriter indexWriter = new IndexWriter(directory, config);
        SearcherManager searcherManager = new SearcherManager(indexWriter, true, true,null);
        ControlledRealTimeReopenThread<IndexSearcher> reopenThread = new ControlledRealTimeReopenThread<>(indexWriter, searcherManager, 2.0, 0.1);
        reopenThread.setName("ControlledRealTimeReopenThread");
        // JVM process dies if all user threads terminate
        reopenThread.setDaemon(true);
        reopenThread.start();
        return new LuceneIndex(analyzer, indexWriter, searcherManager, reopenThread);
    }
}
