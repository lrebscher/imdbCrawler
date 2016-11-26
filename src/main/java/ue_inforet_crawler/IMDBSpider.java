package ue_inforet_crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class IMDBSpider {

    private IMDBSpider() {
    }

    public static void main(final String[] argv) throws IOException {
        String moviesPath = ".".concat(File.separator).concat("resources").concat(File.separator).concat("movies.json");
        String outputDir = ".".concat(File.separator).concat("data").concat(File.separator);

        if (argv.length == 2) {
            moviesPath = argv[0];
            outputDir = argv[1];
        } else if (argv.length != 0) {
            System.out.println("Call with: imdb.IMDBSpider.jar <moviesPath> <outputDir>");
            System.exit(0);
        }

        final IMDBSpider sp = new IMDBSpider();
        sp.fetchIMDBMovies(moviesPath, outputDir);
    }

    /**
     * For each title in file movieListJSON:
     * <p>
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
     * <p>
     * JSON file containing movie titles
     *
     * @param outputDir output directory name of JSON files with metadata of movies.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void fetchIMDBMovies(final String movieListJSON, final String outputDir) throws IOException {
        try {
            final File moviesInput = new File(movieListJSON);
            final JSONParser jsonParser = new JSONParser();
            final InputStreamReader reader = new InputStreamReader(new FileInputStream(moviesInput), "UTF-8");
            final JSONArray object = (JSONArray) jsonParser.parse(reader);

            object.forEach(entry -> {
                final JSONObject jsonObject = (JSONObject) entry;
                final String movieName = (String) jsonObject.get("movie_name");
                System.out.println("JSON movie name: " + movieName);
                final String movieDetailUrl = getUrlToFirstMovieHit(movieName);
                final MovieWriter writer = new MovieWriter(outputDir);

                writer.addMovie(extractMetaData(movieDetailUrl));
                writer.writeFile();
                // printMovie(movie);
            });
        } catch (final ParseException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * @param movieName movieName to search for
     * @return absolute url to detail page of first hit in search results
     */
    private static String getUrlToFirstMovieHit(final String movieName) {
        // get HTML from
        try {
            final Document doc = Jsoup.connect("http://akas.imdb.com/find?q=" + URLEncoder.encode(movieName, "UTF-8") + "&s=tt&ttype=ft").get();
            final Elements elements = doc.getElementsByClass("findList");

            if (elements.isEmpty()) {
                return "";
            }

            // expect to be the right one
            final Element firstElement = elements.get(0);
            // this is the wanted movie element
            final Element firstResult = firstElement.getAllElements().get(0);
            final Element linkToMovie = firstResult.getElementsByClass("result_text").get(0).getElementsByTag("a").get(0);

            final String relativeRedirection = linkToMovie.attr("href");
            final String foundMovieName = linkToMovie.text();

            System.out.println("first movie: \"" + foundMovieName + "\", redirection to: \"" + relativeRedirection + "\"\r\n");

            return "http://akas.imdb.com" + relativeRedirection;
        } catch (final IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * titleWrapper:
     * - titleYear (year)
     * - title
     * - genreList
     * - duration
     * <p>
     * ratingWrapper:
     * - "7,9 based on 900.164 user ratings" (ratingValue and ratingCount)
     * <p>
     * title via id="title"
     * <p>
     * access via attribute "itemprop"
     * <p>
     * example : itemprop="contentRating"
     * <p>
     * <p>
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
    private static Movie extractMetaData(final String urlStr) {
        final HtmlCleaner cleaner = new HtmlCleaner();
        final Movie movie = new Movie();
        URL url = null;

        try {
            url = new URL(urlStr);
        } catch (final MalformedURLException mu) {
            mu.printStackTrace();
            System.exit(-1);
        }
        TagNode node = null;
        try {
            node = cleaner.clean(url);
        } catch (final IOException io) {
            io.printStackTrace();
            System.exit(-1);
        }

        final String TITLE = "//div[@class='title_wrapper']/h1[@itemprop='name']/text()";
        final String YEAR = "//div[@class='title_wrapper']//span[@id='titleYear']/a/text()";
        final String GENRE = "//div[@class='article'][@id='titleStoryLine']//div[@itemprop='genre']/a/text()";
        final String COUNTRY = "//div[@id='titleDetails']/div[h4[@class='inline']='Country:']/a/text()";
        final String DESCRIPTION = "//div[@class='inline canwrap'][@itemprop='description']/text()";
        final String BUDGET = "//div[h4[@class='inline']='Budget:']/text()";
        final String GROSS = "//div[h4[@class='inline']='Gross:']/text()";
        final String RATING_VALUE = "//div[@class='ratingValue']//span[@itemprop='ratingValue']/text()";
        final String RATING_COUNT = "//div[@class='imdbRating']//span[@itemprop='ratingCount']/text()";
        final String DURATION = "//div[@class='subtext']/time[@itemprop='duration']/text()";
        final String CAST = "//table[@class='cast_list']//td[@itemprop='actor']/a/span[@itemprop='name']/text()";
        final String CHARACTER = "//table[@class='cast_list']//td[@class='character']/div/a/text()";
        final String DIRECTOR = "//span[@itemprop='director']//span[@itemprop='name']/text()";

        final String[] XPATHS = { TITLE, YEAR, GENRE, COUNTRY, DESCRIPTION, BUDGET, GROSS, RATING_VALUE, RATING_COUNT, DURATION, CAST, CHARACTER,
                                  DIRECTOR };
        final Object[][] results = new Object[XPATHS.length][];

        for (int i = 0; i < XPATHS.length; i++) {
            try {
                results[i] = node.evaluateXPath(XPATHS[i]);
            } catch (final XPatherException xp) {
                xp.printStackTrace();
                System.exit(-1);
            }
            if (i > 1 && i < 4 || i > 9) {
                if (results[i].length > 0) {
                    results[i][0] = toList(results[i]);
                }
            }
        }

        String temp = null;
        if (results[0].length > 0) {
            temp = results[0][0].toString();
        }
        if (temp != null) {
            if (temp.contains("&nbsp")) {
                temp = temp.substring(0, temp.indexOf("&nbsp"));
            }
        }

        movie.setTitle(temp.trim());
        movie.setUrl(urlStr);

        if (results[1].length > 0) {
            movie.setYear(results[1][0].toString().trim());
        }

        if (results[2].length > 0) {
            movie.setGenreList((List<String>) results[2][0]);
        }

        if (results[3].length > 0) {
            movie.setCountryList((List<String>) results[3][0]);
        }

        if (results[4].length > 0) {
            movie.setDescription(results[4][0].toString().replaceAll("\\n", " ").trim());
        }

        if (results[5].length > 0) {
            temp = results[5][0].toString();
        }

        if (temp != null) {
            temp = temp.replaceAll("\\(.*\\)", "").replaceAll("[^\\d.,$]", "").trim();
        }

        movie.setBudget(temp);
        if (results[6].length > 0) {
            temp = results[6][0].toString();
        }

        if (temp != null) {
            temp = temp.replaceAll("\\(.*\\)", "").replaceAll("[^\\d.,$]", "").trim();
        }

        movie.setGross(temp);

        if (results[7].length > 0) {
            movie.setRatingValue(results[7][0].toString().trim());
        }

        if (results[8].length > 0) {
            movie.setRatingCount(results[8][0].toString().trim());
        }

        if (results[9].length > 0) {
            movie.setDuration(results[9][0].toString().trim());
        }

        if (results[10].length > 0) {
            movie.setCastList((List<String>) results[10][0]);
        }

        if (results[11].length > 0) {
            movie.setCharacterList((List<String>) results[11][0]);
        }

        if (results[12].length > 0) {
            movie.setDirectorList((List<String>) results[12][0]);
        }
        movie.setUrl(urlStr);

        return movie;
    }

    private static List<String> toList(final Object[] results) {
        final List<String> list = new ArrayList<>(results.length);

        for (int i = 0; i < results.length; i++) {
            if (results[i] != null) {
                if (results[i] != "") {
                    list.add(results[i].toString().trim());
                }
            }
        }
        return list;
    }

    private static void printMovie(final Movie movie) {
        System.out.println("Title:\r\n\t" + movie.getTitle());
        System.out.println();
        System.out.println("Year:\r\n\t" + movie.getYear());
        System.out.println();
        System.out.println("Genre:");
        for (String s : movie.getGenreList()) System.out.println("\t" + s);
        System.out.println();
        System.out.println("Country:");
        for (String s : movie.getCountryList()) System.out.println("\t" + s);
        System.out.println();
        System.out.println("Description:\r\n\t" + movie.getDescription());
        System.out.println();
        System.out.println("Budget:\r\n\t" + movie.getBudget());
        System.out.println();
        System.out.println("Gross:\r\n\t" + movie.getGross());
        System.out.println();
        System.out.println("RatingValue:\r\n\t" + movie.getRatingValue());
        System.out.println();
        System.out.println("RatingCount:\r\n\t" + movie.getRatingCount());
        System.out.println();
        System.out.println("Duration:\r\n\t" + movie.getDuration());
        System.out.println();
        System.out.println("Cast:");
        for (String s : movie.getCastList()) System.out.println("\t" + s);
        System.out.println();
        System.out.println("Character:");
        for (String s : movie.getCharacterList()) System.out.println("\t" + s);
        System.out.println();
        System.out.println("Director:");
        for (String s : movie.getDirectorList()) System.out.println("\t" + s);
        System.out.println("\r\n");
    }
}