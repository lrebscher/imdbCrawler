// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BooleanQuery {

    private static final Pattern MV_PATTERN = Pattern.compile("MV: ", Pattern.LITERAL);

    private static final Pattern PLOT_PATTERN = Pattern.compile("PL: ", Pattern.LITERAL);

    private static final Pattern SUSPENDED_PATTERN = Pattern.compile(ParseUtils.SUSPENDED, Pattern.LITERAL);

    static boolean DEBUG = false;

    private final Indexer indexer = new Indexer();

    /**
     * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
     */
    public BooleanQuery() {
    }

    /**
     * A method for reading the textual movie plot file and building indices. The
     * purpose of these indices is to speed up subsequent boolean searches using
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
    public void buildIndices(final String plotFile) {
        /**
         – movie: MV: <title> (<year>)
         – series: MV: "<title>" (<year>)
         – episode: MV: "<title>" (<year>) {<episodeTitle>}
         – television: MV: <title> (<year>) (TV)
         – video: MV: <title> (<year>) (V)
         – videogame: MV: <title> (<year>) (VG)

         an entry starts with “MV: ” and ends with horizontal lines
         (“--------------------------------------------”) or at the end of the corpus

         again, every document has up to five searchable fields:
         title, plot, type, year, episodeTitle

         */

        final long parseStart = System.currentTimeMillis();
        System.out.print("Parsing start...");

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
                StandardCharsets.ISO_8859_1));

            String line;
            final Collection<Document> documents = new ArrayList<>();

            Document actualDocument = null;
            String tmpPlot = "";

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MV: ")) {
                    //head of document
                    actualDocument = new Document();
                    actualDocument.titleId = line;

                    if (line.contains(ParseUtils.SUSPENDED)) {
                        line = SUSPENDED_PATTERN.matcher(line).replaceAll(Matcher.quoteReplacement(""));
                    }

                    String shortenedLine = MV_PATTERN.matcher(line).replaceAll(Matcher.quoteReplacement(""));

                    if (shortenedLine.startsWith("\"")) {

                        if (shortenedLine.contains("{")) {
                            //episode
                            actualDocument.type = "episode";
                            actualDocument.episodeTitle = ParseUtils.tokenize(shortenedLine.substring(shortenedLine.lastIndexOf('{') + 1,
                                shortenedLine.lastIndexOf('}')));

                        } else {
                            //series
                            actualDocument.type = "series";
                        }

                        final String year = ParseUtils.getYear(shortenedLine);
                        actualDocument.year = year;
                        shortenedLine = ParseUtils.removeYear(shortenedLine, year);

                        final String title = shortenedLine.substring(shortenedLine.indexOf('\"') + 1, shortenedLine.lastIndexOf('\"'));

                        if (DEBUG) {
                            System.out.println("Title: " + title + " - Year: " + year);
                        }

                        actualDocument.title = ParseUtils.tokenize(title);

                    } else {
                        shortenedLine = ParseUtils.handleTypeSubString(actualDocument, shortenedLine);

                        //year
                        final String year = ParseUtils.getYear(shortenedLine);
                        actualDocument.year = year;

                        shortenedLine = ParseUtils.removeYear(shortenedLine, year);

                        final String title = shortenedLine.trim();
                        actualDocument.title = ParseUtils.tokenize(title);

                        if (DEBUG) {
                            System.out.println("Title: " + title + " - Year: " + year);
                        }
                    }

                } else if (line.startsWith("------")) {
                    //end of document
                    if (actualDocument != null) {
                        //tokenize plot
                        actualDocument.plot = ParseUtils.tokenize(tmpPlot);

                        //reset tmpPlot
                        tmpPlot = "";
                        documents.add(actualDocument);
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

            final long parseDuration = System.currentTimeMillis() - parseStart;
            System.out.println("Duration for parsing: " + parseDuration / 1000 + " s");

            //build index
            indexer.buildIndexes(documents);

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A method for performing a boolean search on a textual movie plot file after
     * indices were built using the {@link #buildIndices(String) buildIndices}
     * method. The movie plot file contains entries of the <b>types</b> movie,
     * series, episode, television, video, and videogame. This method allows term
     * and phrase searches (the latter being enclosed in double quotes) on any of
     * the <b>fields</b> title, plot, year, episode, and type. Multiple term and
     * phrase searches can be combined by using the character sequence " AND ".
     * Note that queries are case-insensitive.<br>
     * <br>
     * Examples of queries include the following:
     *
     * <pre>
     * title:"game of thrones" AND type:episode AND plot:shae AND plot:Baelish
     * plot:Skywalker AND type:series
     * plot:"year 2200"
     * plot:Berlin AND plot:wall AND type:television
     * plot:Cthulhu
     * title:"saber rider" AND plot:april
     * plot:"James Bond" AND plot:"Jaws" AND type:movie
     * title:"Pimp my Ride" AND episodeTitle:mustang
     * plot:"matt berninger"
     * title:"grand theft auto" AND type:videogame
     * plot:"Jim Jefferies"
     * plot:Berlin AND type:videogame
     * plot:starcraft AND type:movie
     * type:video AND title:"from dusk till dawn"
     * </pre>
     *
     * More details on (a superset of) the query syntax can be found at <a
     * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
     * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
     *
     * DO NOT CHANGE THIS METHOD'S INTERFACE.
     *
     * @param queryString
     *          the query string, formatted according to the Lucene query syntax,
     *          but only supporting term search, phrase search, and the AND
     *          operator
     * @return the exact content (in the textual movie plot file) of the title
     *         lines (starting with "MV: ") of the documents matching the query
     */
    public Set<String> booleanQuery(final String queryString) {
        final String[] searchParts = queryString.split("AND");

        for (final String part : searchParts) {
            final String lowerCasePart = part.toLowerCase().trim();
            if (lowerCasePart.startsWith("title:")) {
                //title index

            } else if (lowerCasePart.startsWith("plot:")) {
                //plot index
                final String term = lowerCasePart.replace("plot:", "");
                if (indexer.getPlotIndex().containsKey(term)) {
                    final Collection<String> documents = indexer.getPlotIndex().get(term);

                }

            } else if (lowerCasePart.startsWith("type:")) {
                //type index

            } else if (lowerCasePart.startsWith("episodeTitle:")) {
                //episodeTitleIndex

            } else if (lowerCasePart.startsWith("year:")) {
                //yearIndex

            }
        }

        if (indexer.doneBuilding()) {
            indexer.getPlotIndex();
        }

        //1. split by AND
        //2. determine searchField (plot, title ..)
        //3. determine if term or phrase search ("asd asd" <--> phrase, asd <--> term)

        return new HashSet<>();
    }

    public static void main(String[] args) {
        BooleanQuery bq = new BooleanQuery();
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
    }

}