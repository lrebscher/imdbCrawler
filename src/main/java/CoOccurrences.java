import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;

import ue_inforet_bool.ParseUtils;

/**
 * Created by lrebscher on 28.01.17.
 */
public class CoOccurrences {

    private static final Pattern MV_PATTERN = Pattern.compile("MV: ", Pattern.LITERAL);

    private static final Pattern PLOT_PATTERN = Pattern.compile("PL: ", Pattern.LITERAL);

    private static final Pattern SUSPENDED_PATTERN = Pattern.compile(ParseUtils.SUSPENDED, Pattern.LITERAL);


    private final Map<String, Integer> occurrences;

    private final Map<CoOccurrence<String>, Integer> coOccurrences;

    private final StopWordHelper stopWordHelper;


    private CoOccurrences() {
        coOccurrences = new HashMap<>();
        occurrences = new HashMap<>();
        stopWordHelper = new StopWordHelper();
    }

    public static void main(String[] args) {
        final String plotListPath = args[0];
        final CoOccurrences coOccurrences = new CoOccurrences();
        coOccurrences.buildIndices(plotListPath);
    }

    private void buildIndices(final String plotFile) {
        System.out.println("Parsing start... ");

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1));
            String line;

            Document actualDocument = null;
            String tmpPlot = "";

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MV: ")) {
                    //head of document
                    actualDocument = new Document();

                    if (line.contains(ParseUtils.SUSPENDED)) {
                        line = SUSPENDED_PATTERN.matcher(line).replaceAll(Matcher.quoteReplacement(""));
                    }

                    String shortenedLine = MV_PATTERN.matcher(line).replaceAll(Matcher.quoteReplacement(""));

                    if (shortenedLine.startsWith("\"")) {

                        if (shortenedLine.contains("{")) {
                            //episode
                            final String episodeTitle = shortenedLine.substring(shortenedLine.lastIndexOf('{') + 1, shortenedLine.lastIndexOf('}'));
                            shortenedLine = shortenedLine.replace('{' + episodeTitle + '}', "");
                        } else {
                            //series
                        }

                        final String year = ParseUtils.getYear(shortenedLine);
                        shortenedLine = ParseUtils.removeYear(shortenedLine, year);

                        final String title = shortenedLine.substring(shortenedLine.indexOf('\"') + 1, shortenedLine.lastIndexOf('\"'));

                        final List<String> tokens = ParseUtils.tokenize(title);
                        addTokens(tokens);

                    } else {
                        shortenedLine = ParseUtils.handleTypeSubString(actualDocument, shortenedLine);

                        //year
                        final String year = ParseUtils.getYear(shortenedLine);

                        shortenedLine = ParseUtils.removeYear(shortenedLine, year);

                        final String title = shortenedLine.trim();

                        final List<String> tokens = ParseUtils.tokenize(title);
                        addTokens(tokens);
                    }
                } else if (line.startsWith("------")) {
                    //end of document
                    if (actualDocument != null) {

                        final List<String> tokens = ParseUtils.tokenize(tmpPlot);
                        addTokens(tokens);

                        //reset tmpPlot
                        tmpPlot = "";
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

            final Collection<CoOccurrence<String>> results = new ArrayList<>();

            coOccurrences.keySet()
                         .forEach(coOccurrence -> {
                             if (occurrences.get(coOccurrence.getLeftToken()) > 1000
                                 && occurrences.get(coOccurrence.getRightToken()) > 1000
                                 && !stopWordHelper.isStopWord(coOccurrence.getLeftToken(), coOccurrence.getRightToken())) {
                                 coOccurrence.score = computeScore(coOccurrence);
                                 results.add(coOccurrence);
                             }
                         });

            results.stream()
                   .sorted((a, b) -> Float.compare(a.score, b.score) * -1)
                   .limit(1000)
                   .forEachOrdered(coOccurrence -> System.out.println(
                       coOccurrence.getLeftToken() + ' ' + coOccurrence.getRightToken() + ' ' + coOccurrence.score));

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Does everything that it should not do.
     *
     * @param tokens passed tokens
     */
    private void addTokens(final Iterable<String> tokens) {
        String previousToken = null;
        for (final String token : tokens) {
            if (occurrences.containsKey(token)) {
                occurrences.put(token, occurrences.get(token) + 1);
            } else {
                occurrences.put(token, 1);
            }

            if (previousToken != null) {
                final CoOccurrence<String> coOccurrence = new CoOccurrence<>(previousToken, token);
                if (coOccurrences.containsKey(coOccurrence)) {
                    coOccurrences.put(coOccurrence, coOccurrences.get(coOccurrence) + 1);
                } else {
                    coOccurrences.put(coOccurrence, 1);
                }
            }

            previousToken = token;
        }
    }

    /**
     * will use the following formula to compute score:
     *
     *            2 * F(t, t')
     * s(t,t') =  -----------
     *            F(t) + F(t')
     *
     *
     * @param coOccurrence passed coOccurrence
     * @return score computed with documented formula
     */
    private float computeScore(final CoOccurrence<String> coOccurrence) {
        return (float) (2 * coOccurrences.get(coOccurrence)) / (float) (occurrences.get(coOccurrence.getLeftToken()) + occurrences.get(
            coOccurrence.getRightToken()));
    }
}


