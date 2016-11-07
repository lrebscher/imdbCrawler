import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class IMDBSpider {

    private IMDBSpider() {
    }

    public static void main(final String[] argv) throws IOException {
        String moviesPath = "./data/movies.json";
        String outputDir = "./data";

        if (argv.length == 2) {
            moviesPath = argv[0];
            outputDir = argv[1];
        } else if (argv.length != 0) {
            System.out.println("Call with: IMDBSpider.jar <moviesPath> <outputDir>");
            System.exit(0);
        }

        IMDBSpider sp = new IMDBSpider();
        sp.fetchIMDBMovies(moviesPath, outputDir);
    }

    /**
     * For each title in file movieListJSON:
     *
     * <pre>
     * You should:
     * - First, read a list of 500 movie titles from the JSON file in 'movieListJSON'.
     *
     * - Secondly, for each movie title, perform a web search on IMDB and retrieve
     * movie’s URL: http://akas.imdb.com/find?q=<MOVIE>&s=tt&ttype=ft
     *
     * - Thirdly, for each movie, extract metadata (actors, budget, description)
     * from movie’s URL and store to a JSON file in directory 'outputDir':
     *    http://www.imdb.com/title/tt0499549/?ref_=fn_al_tt_1 for Avatar - store
     * </pre>
     *
     *          JSON file containing movie titles
     * @param outputDir
     *          output directory for JSON files with metadata of movies.
     * @throws IOException
     */
    private void fetchIMDBMovies(final String movieListJSON, final String outputDir) throws IOException {

        try {
            final File moviesInput = new File(getClass().getResource("/movies.json").toURI());
            final org.json.simple.parser.JSONParser jsonParser = new org.json.simple.parser.JSONParser();
            final Reader reader = new FileReader(moviesInput);
            final JSONArray object = (JSONArray) jsonParser.parse(reader);
            object.forEach(entry -> {
                final JSONObject jsonObject = (JSONObject) entry;
                final String movieName = (String) jsonObject.get("movie_name");
                System.out.println("JSON movie name: " + movieName);

                final String movieDetailUrl = getUrlToFirstMovieHit(movieName);

                //next step: parse html and extract metadata to json, save result in outputDir
                try {
                    final Document movieDocument = Jsoup.connect(movieDetailUrl).get();
                    extractMetadata(movieDocument);


                } catch (final IOException e) {
                    e.printStackTrace();
                }
            });

        } catch (final URISyntaxException | ParseException e) {
            //TODO error handling
            e.printStackTrace();
        }
    }

    /**
     *
     * @param movieName movieName to search for
     * @return absolute url to detail page of first hit in search results
     */
    private static String getUrlToFirstMovieHit(final String movieName) {
        //get HTML from
        try {
            final Document doc = Jsoup.connect("http://akas.imdb.com/find?q=" + movieName + "&s=tt&ttype=ft").get();
            final Elements elements = doc.getElementsByClass("findList");

            if (elements.isEmpty()) {
                return "";
            }

            //expect to be the right one
            final Element firstElement = elements.get(0);
            //this is the wanted movie element
            final Element firstResult = firstElement.getAllElements().get(0);
            final Element linkToMovie = firstResult.getElementsByClass("result_text")
                                                   .get(0)
                                                   .getElementsByTag("a")
                                                   .get(0);

            final String relativeRedirection = linkToMovie.attr("href");
            final String foundMovieName = linkToMovie.text();

            System.out.println("first movie: " + foundMovieName + " redirection to : " + relativeRedirection + "\n");

            return "http://akas.imdb.com" + relativeRedirection;

        } catch (final IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     *
     *
     * titleWrapper:
     * - titleYear (year)
     * - title
     * - genreList
     * - duration
     *
     * ratingWrapper:
     * - "7,9 based on 900.164 user ratings" (ratingValue and ratingCount)
     *
     * title via id="title
     *
     * access via attribute "itemprop"
     *
     * example : itemprop="contentRating"
     *
     *
     * url, CHECK
     * title, CHECK
     * year, CHECK
     * genreList, CHECK
     * countryList,
     * description,
     * budget,
     * gross,
     * ratingValue, CHECK
     * ratingCount, CHECK
     * duration, CHECK
     * castList,
     * characterList,
     * directorList
     */
    private static String extractMetadata(final Document movieDocument) {
        //TODO.. parse movieDocument and extract properties to JSON format
        return "";
    }

    /**
     * Helper method to remove html and formating from text.
     *
     * @param text
     *          The text to be cleaned
     * @return clean text
     */
    protected static String cleanText(final String text) {
        return text.replaceAll("\\<.*?>", "").replace("&nbsp;", " ")
                   .replace("\n", " ").replaceAll("\\s+", " ").trim();
    }
}
