package ue_inforet_bool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lrebscher on 04.12.16.
 */
final class ParseUtils {

    static final String SUSPENDED = "{{suspended}}";

    private static final Pattern DELIMITER_PATTERN = Pattern.compile("\\.|,| |\\?|:|!");

    private static final Pattern TELEVISION_PATTERN = Pattern.compile(DocumentType.TELEVISION, Pattern.LITERAL);

    private static final Pattern VIDEO_PATTERN = Pattern.compile(DocumentType.VIDEO, Pattern.LITERAL);

    private static final Pattern VIDEO_GAME_PATTERN = Pattern.compile(DocumentType.VIDEO_GAME, Pattern.LITERAL);

    private ParseUtils() {
        //hide constructor
    }

    /**
     * sets corresponding type if found and returns line with removed type
     *
     * @param actualDocument given document
     * @param shortendedLine given line
     * @return line with removed type
     */
    static String handleTypeSubString(final Document actualDocument, String shortendedLine) {
        if (shortendedLine.contains(DocumentType.TELEVISION)) {
            //television
            shortendedLine = TELEVISION_PATTERN.matcher(shortendedLine).replaceAll(Matcher.quoteReplacement(""));
            actualDocument.type = "television";

        } else if (shortendedLine.contains(DocumentType.VIDEO)) {
            //video
            shortendedLine = VIDEO_PATTERN.matcher(shortendedLine).replaceAll(Matcher.quoteReplacement(""));
            actualDocument.type = "video";

        } else if (shortendedLine.contains(DocumentType.VIDEO_GAME)) {
            //videogame
            shortendedLine = VIDEO_GAME_PATTERN.matcher(shortendedLine).replaceAll(Matcher.quoteReplacement(""));
            actualDocument.type = "videogame";
        }
        return shortendedLine;
    }

    static String getYear(final String shortendedLine) {
        return shortendedLine.substring(shortendedLine.lastIndexOf('(') + 1, shortendedLine.lastIndexOf(')'));
    }

    static String removeYear(final String shortenedLine, final String year) {
        return shortenedLine.replace('(' + year + ')', "");
    }

    /**
     * - convert terms (for indices, term queries, and phrase queries) to
     * lower case (case-insensitive search)
     *
     * - use as delimiters only blanks, dots, commas, colons, exclamation
     * marks, and question marks ( .,:!?)
     *
     * - leave all other special characters untouched
     *
     * @return List of tokens
     */
    static Collection<String> tokenize(final String plot) {
        final String lowerCasePlot = plot.toLowerCase();

        //TODO no "" double quotes allowed?
        final String[] tokens = DELIMITER_PATTERN.split(lowerCasePlot);
        final Collection<String> clearedTokens = new ArrayList<>(tokens.length);
        for (final String token : tokens) {
            if (!token.isEmpty()) {
                clearedTokens.add(token);
            }
        }

        return clearedTokens;
    }
}
