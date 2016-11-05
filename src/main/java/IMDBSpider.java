import java.io.IOException;

public class IMDBSpider {

    public IMDBSpider() {
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
    public void fetchIMDBMovies(final String movieListJSON, final String outputDir)
        throws IOException {
        // TODO add code here
    }

    /**
     * Helper method to remove html and formating from text.
     *
     * @param text
     *          The text to be cleaned
     * @return clean text
     */
    protected static String cleanText(String text) {
        return text.replaceAll("\\<.*?>", "").replace("&nbsp;", " ")
                   .replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    public static void main(String argv[]) throws IOException {
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
}
