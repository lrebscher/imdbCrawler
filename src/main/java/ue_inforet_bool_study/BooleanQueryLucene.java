// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool_study;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import ue_inforet_bool.ParseUtils;

public class BooleanQueryLucene {

    private static final Pattern MV_PATTERN = Pattern.compile("MV: ", Pattern.LITERAL);

    private static final Pattern PLOT_PATTERN = Pattern.compile("PL: ", Pattern.LITERAL);

    private static final Pattern SUSPENDED_PATTERN = Pattern.compile(ParseUtils.SUSPENDED, Pattern.LITERAL);

    private IndexWriter indexWriter;

    private Directory index;

    /**
     * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
     */
    public BooleanQueryLucene() {
    }

    /**
     * A method for reading the textual movie plot file and building a Lucene index.
     * The purpose of the index is to speed up subsequent boolean searches using
     * the {@link #booleanQuery(String) booleanQuery} method.
     *
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param plotFile
     *          the textual movie plot file 'plot.list', obtainable from <a
     *          href="http://www.imdb.com/interfaces"
     *          >http://www.imdb.com/interfaces</a> for personal, non-commercial
     *          use.
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
            indexWriter.close();

            final long parseDuration = System.currentTimeMillis() - parseStart;
            System.out.println("Duration for parsing: " + parseDuration / 1000 + " s");

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method for performing a boolean search on a textual movie plot file after
     * Lucene indices were built using the {@link #buildIndices(String) buildIndices}
     * method. The movie plot file contains entries of the <b>types</b> movie,
     * series, episode, television, video, and videogame. This method allows queries
     * following the Lucene query syntax on any of the <b>fields</b> title, plot, year,
     * episode, and type. Note that queries are case-insensitive and stop words are
     * removed.<br>
     * <br>
     * Examples of queries include the following:
     *
     * <pre>
     * title:"game of thrones" AND type:episode AND (plot:Bastards OR (plot:Jon AND plot:Snow)) -plot:son
     * title:"Star Wars" AND type:movie AND plot:Luke AND year:[1977 TO 1987]
     * plot:Berlin AND plot:wall AND type:television
     * plot:men~1 AND plot:women~1 AND plot:love AND plot:fool AND type:movie
     * title:westworld AND type:episode AND year:2016 AND plot:Dolores
     * plot:You AND plot:never AND plot:get AND plot:A AND plot:second AND plot:chance
     * plot:Hero AND plot:Villain AND plot:destroy AND type:movie
     * (plot:lover -plot:perfect) AND plot:unfaithful* AND plot:husband AND plot:affair AND type:movie
     * (plot:Innocent OR plot:Guilty) AND plot:crime AND plot:murder AND plot:court AND plot:judge AND type:movie
     * plot:Hero AND plot:Marvel -plot:DC AND type:movie
     * plot:Hero AND plot:DC -plot:Marvel AND type:movie
     * </pre>
     *
     * More details on the query syntax can be found at <a
     * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
     * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
     *
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param queryString
     *          the query string, formatted according to the Lucene query syntax.
     * @return the exact content (in the textual movie plot file) of the title
     *         lines (starting with "MV: ") of the documents matching the query
     */
    public Set<String> booleanQuery(String queryString) {
        final Set<String> set = new HashSet<>();

        //TODO Use	word	tokenization	and	stop	word	removal, but no	stemming

        try {
            final QueryParser queryParser = new QueryParser(queryString, new StandardAnalyzer());
            final Query query = queryParser.parse(queryString);

            final IndexReader indexReader = DirectoryReader.open(index);
            final IndexSearcher indexSearcher = new IndexSearcher(indexReader);

            //numHits ~ 1000 should be enough for these queries, maybe there is a way to get unlimited hits
            final TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(1000);
            indexSearcher.search(query, topScoreDocCollector);

            final ScoreDoc[] scoreDocs = topScoreDocCollector.topDocs().scoreDocs;

            for (final ScoreDoc scoreDoc : scoreDocs) {
                final int docId = scoreDoc.doc;
                final Document document = indexSearcher.doc(docId);
                set.add(document.get("id"));
            }

            return set;

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new HashSet<>();
    }

    /**
     * A method for closing any open file handels or a ThreadPool.
     *
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     */
    public void close() {
        // TODO: you may insert code here
    }

    public static void main(String[] args) {
        BooleanQueryLucene bq = new BooleanQueryLucene();
        if (args.length < 3) {
            System.err
                .println("usage: java -jar BooleanQuery.jar <plot list file> <queries file> <results file>");
            System.exit(-1);
        }

        // build indices
        System.out.println("building indices...");
        long tic = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long mem = runtime.totalMemory();
        bq.buildIndices(args[0]);
        System.out
            .println("runtime: " + (System.nanoTime() - tic) + " nanoseconds");
        System.out
            .println("memory: " + ((runtime.totalMemory() - mem) / (1048576l))
                + " MB (rough estimate)");

        // parsing the queries that are to be run from the queries file
        List<String> queries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(args[1]), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null)
                queries.add(line);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // parsing the queries' expected results from the results file
        List<Set<String>> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(args[2]), StandardCharsets.ISO_8859_1))) {
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
            Set<String> expectedResult = i < results.size() ? results.get(i)
                : new HashSet<>();
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

            System.out.println("runtime:         " + (System.nanoTime() - tic)
                + " nanoseconds.");
            System.out.println("expected result: " + expectedResultSorted.toString());
            System.out.println("actual result:   " + actualResultSorted.toString());
            System.out.println(expectedResult.equals(actualResult) ? "SUCCESS"
                : "FAILURE");
        }

        bq.close();
    }

}
