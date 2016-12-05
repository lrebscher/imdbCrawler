package ue_inforet_bool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lrebscher on 04.12.16.
 */
class Indexer {

    private Map<String, Collection<String>> titleIndex;

    private Map<String, Collection<String>> plotIndex;

    private Map<String, Collection<String>> episodeIndex;

    private Map<String, Collection<String>> yearIndex;

    private Map<String, Collection<String>> typeIndex;

    private Map<String, Document> documentMap;

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
    void buildIndexes(final Collection<Document> documentList) {
        documentMap = new ConcurrentHashMap<>(documentList.size());
        titleIndex = new ConcurrentHashMap<>();
        plotIndex = new ConcurrentHashMap<>();
        episodeIndex = new ConcurrentHashMap<>();
        yearIndex = new ConcurrentHashMap<>();
        typeIndex = new ConcurrentHashMap<>();

        documentList.stream()
                    .parallel()
                    .forEach(document -> {
                        documentMap.put(document.titleId, document);

                        //title index
                        processDocument(document.titleId, document.title, titleIndex);

                        //plot index
                        processDocument(document.titleId, document.plot, plotIndex);

                        //episode index
                        if ("episode".equals(document.type)) {
                            processDocument(document.titleId, document.episodeTitle, episodeIndex);
                        }

                        //year index
                        if (document.year != null && !document.year.contains("????")) {
                            processDocument(document.titleId, Collections.singleton(document.year), yearIndex);
                        }

                        //type index
                        processDocument(document.titleId, Collections.singleton(document.type), typeIndex);
                    });
    }

    /**
     *
     * @param documentId given documentId
     * @param tokens given tokens
     * @param index given index
     */
    private static void processDocument(final String documentId, final Iterable<String> tokens, final Map<String, Collection<String>> index) {
        for (final String token : tokens) {
            if (index.containsKey(token)) {
                final Collection<String> documents = index.get(token);
                documents.add(documentId);
            } else {
                final Collection<String> docIds = new ArrayList<>();
                docIds.add(documentId);
                index.put(token, docIds);
            }
        }
    }

    public Map<String, Collection<String>> getTitleIndex() {
        return Collections.unmodifiableMap(titleIndex);
    }

    public Map<String, Collection<String>> getEpisodeIndex() {
        return Collections.unmodifiableMap(episodeIndex);
    }

    public Map<String, Collection<String>> getPlotIndex() {
        return Collections.unmodifiableMap(plotIndex);
    }

    public Map<String, Collection<String>> getYearIndex() {
        return Collections.unmodifiableMap(yearIndex);
    }

    public Map<String, Collection<String>> getTypeIndex() {
        return Collections.unmodifiableMap(typeIndex);
    }

    public Map<String, Document> getDocumentMap() {
        return Collections.unmodifiableMap(documentMap);
    }

    boolean doneBuilding() {
        return titleIndex != null && plotIndex != null && episodeIndex != null;
    }

}
