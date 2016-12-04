package ue_inforet_bool;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lrebscher on 04.12.16.
 */
class Indexer {

    private Map<String, Collection<String>> titleIndex;

    /**
     * build inverted index
     *
     * term -> list of documentId
     *
     * for every search field -> one index
     *
     * for every searchable fiels one tokenMap
     * - title
     * - plot
     * - type
     * - year
     * - episodeTitle
     *
     * @param documentList documents to index
     */
    void buildTitleIndex(final Collection<Document> documentList) {
        titleIndex = new ConcurrentHashMap<>();
        documentList.stream()
                    .parallel()
                    .forEach(document -> processDocument(document.titleId, document.title, titleIndex));
    }

    /**
     *
     * @param documentId given documentId
     * @param tokens given tokens
     * @param index given index
     */
    private void processDocument(final String documentId, final String[] tokens, final Map<String, Collection<String>> index) {
        for (final String token : tokens) {
            if (index.containsKey(token)) {
                final Collection<String> documents = index.get(token);
                documents.add(documentId);
            } else {
                index.put(token, Collections.singleton(documentId));
            }
        }
    }

    public Map<String, Collection<String>> getTitleIndex() {
        return titleIndex;
    }

    boolean isTitleIndexBuild() {
        return titleIndex != null;
    }

}
