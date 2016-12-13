package ue_inforet_bool;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;

/**
 * Created by lrebscher on 04.12.16.
 */
public final class ParseUtils {

    public static final String SUSPENDED = "{{suspended}}";

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
    public static String handleTypeSubString(final org.apache.lucene.document.Document actualDocument, String shortendedLine) {
        final String type;
        if (shortendedLine.contains(DocumentType.TELEVISION)) {
            //television
            shortendedLine = TELEVISION_PATTERN.matcher(shortendedLine).replaceAll(Matcher.quoteReplacement(""));
            type = "television";

        } else if (shortendedLine.contains(DocumentType.VIDEO)) {
            //video
            shortendedLine = VIDEO_PATTERN.matcher(shortendedLine).replaceAll(Matcher.quoteReplacement(""));
            type = "video";

        } else if (shortendedLine.contains(DocumentType.VIDEO_GAME)) {
            //videogame
            shortendedLine = VIDEO_GAME_PATTERN.matcher(shortendedLine).replaceAll(Matcher.quoteReplacement(""));
            type = "videogame";
        } else {
            type = "movie";
        }

        actualDocument.add(new TextField("type", type, Store.YES));

        return shortendedLine;
    }

    public static String getYear(final String shortendedLine) {
        return shortendedLine.substring(shortendedLine.lastIndexOf('(') + 1, shortendedLine.lastIndexOf(')'));
    }

    public static String removeYear(final String shortenedLine, final String year) {
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
    public static List<String> tokenize(final String string) {
        final String lowerCasePlot = string.toLowerCase();
        final String[] tokens = DELIMITER_PATTERN.split(lowerCasePlot);
        final List<String> clearedTokens = new ArrayList<>(tokens.length);
        for (final String token : tokens) {

            if (!token.isEmpty()) {
                clearedTokens.add(token);
            }
        }

        return clearedTokens;
    }
}
