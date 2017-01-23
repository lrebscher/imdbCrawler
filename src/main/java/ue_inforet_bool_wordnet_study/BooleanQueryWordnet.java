// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool_wordnet_study;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import ue_inforet_bool.ParseUtils;

public class BooleanQueryWordnet {

    private static final Pattern MV_PATTERN = Pattern.compile("MV: ", Pattern.LITERAL);

    private static final Pattern PLOT_PATTERN = Pattern.compile("PL: ", Pattern.LITERAL);

    private static final Pattern SUSPENDED_PATTERN = Pattern.compile(ParseUtils.SUSPENDED, Pattern.LITERAL);

    private IndexWriter indexWriter;

    private Directory index;


    private HashMap<String, Collection<String>> synSets;

    private Map<String, Collection<String>> synSetsAdj;

    private Map<String, Collection<String>> synSetsAdv;

    private Map<String, Collection<String>> synSetsNoun;

    private Map<String, Collection<String>> synSetsVerb;

    private Map<String, Collection<String>> excSets;

    private Map<String, Collection<String>> excSetsAdj;

    private Map<String, Collection<String>> excSetsAdv;

    private Map<String, Collection<String>> excSetsNoun;

    private Map<String, Collection<String>> excSetsVerb;

    private HashMap[] synSetsColl;

    private HashMap[] excSetsColl;


    /**
     * DO NOT ADD ADDITIONAL PARAMETERS TO THE SIGNATURE
     * <p>
     * OF THE CONSTRUCTOR.
     */
    public BooleanQueryWordnet() { /* TODO you may insert code here	*/ }

    /**
     * A method for parsing the WortNet synsets.
     * <p>
     * The data.[noun, verb, adj, adv] files contain the synsets.​
     * <p>
     * The [noun, verb, adj, adv].exc	files contain the base forms
     * <p>
     * of irregular words.
     * <p>
     * <p>
     * <p>
     * Please refer to ​
     * <p>
     * http://wordnet.princeton.edu/man/wndb.5WN.html
     * <p>
     * regarding the syntax of these plain files.​
     * <p>
     * <p>
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param wordnetDir the directory of the wordnet files
     */
    public void buildSynsets(String wordnetDir) {
        if (!wordnetDir.endsWith("/")) {
            wordnetDir += "/";
        }

        final String[] FILENAMES = { "data.adj", "data.adv", "data.noun", "data.verb", "adj.exc", "adv.exc", "noun.exc", "verb.exc" };

        synSets = new HashMap<>(61000);
        synSetsAdj = new HashMap<>(15250);
        synSetsAdv = new HashMap<>(15250);
        synSetsNoun = new HashMap<>(15250);
        synSetsVerb = new HashMap<>(15250);

        synSetsColl = new HashMap[4];
        synSetsColl[0] = (HashMap) synSetsAdj;
        synSetsColl[1] = (HashMap) synSetsAdv;
        synSetsColl[2] = (HashMap) synSetsNoun;
        synSetsColl[3] = (HashMap) synSetsVerb;
        excSetsColl = new HashMap[4];

        BufferedReader reader;
        String[] tokenLine;
        String curToken;
        String line;
        int i;
        for (i = 0; i < FILENAMES.length / 2; i++) {

            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(wordnetDir + FILENAMES[i]), StandardCharsets.ISO_8859_1));
                for (int j = 1; j < 30; j++) {
                    reader.readLine();
                }

                while ((line = reader.readLine()) != null) {
                    tokenLine = line.split(" ");
                    final String newKey = tokenLine[4];

                    int synNum = 0;
                    try {
                        synNum = Integer.parseInt(tokenLine[3], 16);
                    } catch (final NumberFormatException nf) {
                        System.err.println("Unexpected token in file " + FILENAMES[i] + ".");
                        System.exit(1);
                    }

                    if (newKey.contains("_")) {
                        continue;
                    }
                    final List<String> synonyms = new ArrayList<>();

                    for (int j = 0; j < 2 * (synNum - 1); j += 2) {
                        curToken = tokenLine[j + 6];
                        if (curToken.contains("_")) {
                            continue;
                        }
                        if (curToken.endsWith("(p)") || curToken.endsWith("(a)")) {
                            curToken = curToken.substring(0, curToken.length() - 3);
                        } else if (curToken.endsWith("(ip)")) {
                            curToken = curToken.substring(0, curToken.length() - 4);
                        }
                        synonyms.add(curToken);
                    }

                    if (synSets.isEmpty()) {
                        synSets.put(newKey, synonyms);
                        synSetsColl[i].put(newKey, synonyms);
                    } else if (synSets.containsKey(newKey)) {
                        final List<String> curValue = (List<String>) synSets.get(newKey);

                        for (final String synonym : synonyms) {
                            if (!curValue.contains(synonym)) {
                                curValue.add(synonym);
                            }
                        }

                        if (synSetsColl[i].containsKey(newKey)) {
                            final Collection<String> curValueSpec = (Collection<String>) synSetsColl[i].get(newKey);

                            for (final String synonym : synonyms) {
                                if (!curValueSpec.contains(synonym)) {
                                    curValueSpec.add(synonym);
                                }
                            }
                        }
                    } else {
                        synSets.put(newKey, synonyms);
                        synSetsColl[i].put(newKey, synonyms);
                    }

                    for (int j = 0; j < synonyms.size(); j++) {
                        final List<String> curSyns = new ArrayList<>();
                        curSyns.add(newKey);

                        for (int k = 0; k < synonyms.size(); k++) {
                            if (k != j) {
                                curSyns.add(synonyms.get(k));
                            }
                        }

                        if (synSets.isEmpty()) {
                            synSets.put(synonyms.get(j), curSyns);
                        } else if (synSets.containsKey(synonyms.get(j))) {
                            for (final String curSyn : curSyns) {
                                if (!synSets.get(synonyms.get(j)).contains(curSyn)) {
                                    synSets.get(synonyms.get(j)).add(curSyn);
                                }
                            }
                        } else {
                            synSets.put(synonyms.get(j), curSyns);
                        }
                    }
                }
            } catch (final IOException io) {
                System.err.println("Failed to read file " + FILENAMES[i] + ".");
                System.exit(1);
            }
        }

        excSets = new HashMap<>(6000, 1.0f);
        excSetsAdj = new HashMap<>(1500);
        excSetsAdv = new HashMap<>(1500);
        excSetsNoun = new HashMap<>(1500);
        excSetsVerb = new HashMap<>(1500, 1.0f);

        excSetsColl[0] = (HashMap) excSetsAdj;
        excSetsColl[1] = (HashMap) excSetsAdv;
        excSetsColl[2] = (HashMap) excSetsNoun;
        excSetsColl[3] = (HashMap) excSetsVerb;

        for (i = 4; i < FILENAMES.length; i++) {

            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(wordnetDir + FILENAMES[i]), StandardCharsets.ISO_8859_1));

                while ((line = reader.readLine()) != null) {
                    tokenLine = line.split(" ");
                    final List<String> exceptions = new ArrayList<>();

                    if (tokenLine[0].contains("_")) {
                        continue;
                    }

                    for (int j = 1; j < tokenLine.length; j++) {
                        curToken = tokenLine[j];
                        if (curToken.contains("_")) {
                            continue;
                        }
                        if (curToken.endsWith("(p)") || curToken.endsWith("(a)")) {
                            curToken = curToken.substring(0, curToken.length() - 3);
                        } else if (curToken.endsWith("(ip)")) {
                            curToken = curToken.substring(0, curToken.length() - 4);
                        }
                        exceptions.add(curToken);
                    }

                    if (excSetsColl[i - 4].isEmpty()) {
                        excSetsColl[i - 4].put(tokenLine[0], exceptions);
                    } else if (excSetsColl[i - 4].containsKey(tokenLine[0])) {
                        final List<String> curValue = (ArrayList<String>) excSetsColl[i - 4].get(tokenLine[0]);

                        for (int j = 0; j < curValue.size(); j++) {
                            if (!curValue.contains(exceptions.get(j))) {
                                curValue.add(exceptions.get(j));
                            }
                        }
                    } else {
                        excSetsColl[i - 4].put(tokenLine[0], exceptions);
                    }
                }
            } catch (final IOException io) {
                System.err.println("Failed to read file " + FILENAMES[i] + ".");
                System.exit(1);
            }
        }
    }

    /**
     * A method for reading the textual movie plot file and building a Lucene index.
     * <p>
     * The purpose of the index is to speed up subsequent boolean searches using
     * <p>
     * the {@link #booleanQuery(String) booleanQuery} method.
     * <p>
     * <p>
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param plotFile the textual movie plot file 'plot.list', obtainable from <a
     *                 <p>
     *                 href="http://www.imdb.com/interfaces"
     *                 <p>
     *                 >http://www.imdb.com/interfaces</a> for personal, non-commercial
     *                 <p>
     *                 use.
     */

    public void buildIndices(String plotFile) {
        final long parseStart = System.currentTimeMillis();
        System.out.print("Parsing start... ");

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1));
            String line;
            Document actualDocument = null;
            String tmpPlot = "";
            index = new RAMDirectory();
            final StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
            final IndexWriterConfig indexWriterConfig = new IndexWriterConfig(standardAnalyzer);
            indexWriter = new IndexWriter(index, indexWriterConfig);

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MV: ")) {
                    //head of document
                    actualDocument = new Document();
                    actualDocument.add(new TextField("id", line, Store.YES));

                    if (line.contains(ParseUtils.SUSPENDED)) {
                        line = SUSPENDED_PATTERN.matcher(line).replaceAll(Matcher.quoteReplacement(""));
                    }

                    String shortenedLine = MV_PATTERN.matcher(line).replaceAll(Matcher.quoteReplacement(""));

                    if (shortenedLine.startsWith("\"")) {
                        if (shortenedLine.contains("{")) {
                            //episode
                            actualDocument.add(new TextField("type", "episode", Store.YES));
                            final String episodeTitle = shortenedLine.substring(shortenedLine.lastIndexOf('{') + 1, shortenedLine.lastIndexOf('}'));
                            actualDocument.add(new TextField("episodetitle", episodeTitle, Store.YES));
                            // ParseUtils.tokenize(episodeTitle);
                            shortenedLine = shortenedLine.replace('{' + episodeTitle + '}', "");
                        } else {
                            //series
                            actualDocument.add(new TextField("type", "series", Store.YES));
                        }

                        final String year = ParseUtils.getYear(shortenedLine);
                        actualDocument.add(new TextField("year", year, Store.YES));
                        shortenedLine = ParseUtils.removeYear(shortenedLine, year);
                        final String title = shortenedLine.substring(shortenedLine.indexOf('\"') + 1, shortenedLine.lastIndexOf('\"'));
                        actualDocument.add(new TextField("title", title, Store.YES));
                        //ParseUtils.tokenize(title);
                    } else {
                        shortenedLine = ParseUtils.handleTypeSubString(actualDocument, shortenedLine);
                        //year
                        final String year = ParseUtils.getYear(shortenedLine);
                        actualDocument.add(new TextField("year", year, Store.YES));
                        shortenedLine = ParseUtils.removeYear(shortenedLine, year);
                        final String title = shortenedLine.trim();
                        actualDocument.add(new TextField("title", title, Store.YES));
                        // ParseUtils.tokenize(title);
                    }
                } else if (line.startsWith("------")) {
                    //end of document
                    if (actualDocument != null) {
                        //tokenize plot
                        //ParseUtils.tokenize(tmpPlot);
                        actualDocument.add(new TextField("plot", tmpPlot, Store.YES));
                        //reset tmpPlot
                        tmpPlot = "";
                        indexWriter.addDocument(actualDocument);
                    }
                } else if (line.startsWith("PL:")) {
                    //plot inside document
                    //collect all plot lines
                    final String plot = PLOT_PATTERN.matcher(line).replaceAll(Matcher.quoteReplacement(""));
                    tmpPlot = tmpPlot.concat(" " + plot);
                }
            }

            //close reader
            reader.close();
            indexWriter.commit();
            indexWriter.close();

            final long parseDuration = System.currentTimeMillis() - parseStart;
            System.out.println("Duration for parsing: " + parseDuration / 1000 + " s");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method for performing a boolean search on a textual movie plot file after
     * <p>
     * Lucene indices were built using the {@link #buildIndices(String) buildIndices}
     * <p>
     * method. The movie plot file contains entries of the <b>types</b> movie,
     * <p>
     * series, episode, television, video, and videogame. This method allows queries
     * <p>
     * following the Lucene query syntax on any of the <b>fields</b> title, plot, year,
     * <p>
     * episode, and type. Note that queries are case-insensitive and stop words are
     * <p>
     * removed.<br>
     * <p>
     * <br>
     * <p>
     * <p>
     * <p>
     * More details on the query syntax can be found at <a
     * <p>
     * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
     * <p>
     * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
     * <p>
     * <p>
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param queryString the query string, formatted according to the Lucene query syntax.
     * @return the exact content (in the textual movie plot file) of the title
     * <p>
     * lines (starting with "MV: ") of the documents matching the query
     */

    public Set<String> booleanQuery(String queryString) {
        // TODO: insert code here
        return new HashSet<>();
    }

    /**
     * A method for closing any open file handels or a ThreadPool.
     * <p>
     * <p>
     * <p>
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     */

    public void close() {
        // TODO: you may insert code here
        // DO NOTHING HERE FOR NOW
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("usage: java -jar BooleanQueryWordnet.jar <plot list file> <wordnet directory> <queries file> <results file>");
            System.exit(-1);
        }

        // build indices
        System.out.println("building indices...");
        long tic = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long mem = runtime.totalMemory();

        // the directory to the wordnet-files: [*.exc], [data.*]
        String plotFile = args[0];
        String wordNetDir = args[1];
        BooleanQueryWordnet bq = new BooleanQueryWordnet();
        bq.buildSynsets(wordNetDir);
        bq.buildIndices(plotFile);
        System.gc();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        System.out.println("runtime: " + (System.nanoTime() - tic) + " nanoseconds");
        System.out.println("memory: " + ((runtime.totalMemory() - mem) / (1048576l)) + " MB (rough estimate)");

        // parsing the queries that are to be run from the queries file
        List<String> queries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[2]), StandardCharsets.ISO_8859_1))) {
            String line;

            while ((line = reader.readLine()) != null) queries.add(line);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // parsing the queries' expected results from the results file
        List<Set<String>> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[3]), StandardCharsets.ISO_8859_1))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Set<String> result = new HashSet<>();
                results.add(result);

                for (int i = 0; i < Integer.parseInt(line); i++) {
                    result.add(reader.readLine());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // run queries
        for (int i = 0; i < queries.size(); i++) {
            String query = queries.get(i);
            Set<String> expectedResult = i < results.size() ? results.get(i) : new HashSet<>();
            System.out.println();
            System.out.println("query:           " + query);
            tic = System.nanoTime();
            Set<String> actualResult = bq.booleanQuery(query);

            // sort expected and determined results for human readability
            List<String> expectedResultSorted = new ArrayList<>(expectedResult);
            List<String> actualResultSorted = new ArrayList<>(actualResult);
            Comparator<String> stringComparator = new Comparator<String>() {

                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            };

            expectedResultSorted.sort(stringComparator);
            actualResultSorted.sort(stringComparator);
            System.out.println("runtime:         " + (System.nanoTime() - tic) + " nanoseconds.");
            System.out.println("expected result (" + expectedResultSorted.size() + "): " + expectedResultSorted.toString());
            System.out.println("actual result (" + actualResultSorted.size() + "):   " + actualResultSorted.toString());
            System.out.println(expectedResult.equals(actualResult) ? "SUCCESS" : "FAILURE");
        }

        bq.close();
    }
}