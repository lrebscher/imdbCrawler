package ue_inforet_crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

public final class MovieReader {

    private MovieReader() {
    }

    /**
     * Read movies from JSON files in directory 'moviesDir' formatted according to
     * 'example_movie_avatar.json'.
     *
     * Each movie should contain the attributes: url, title, year, genreList,
     * countryList, description, budget, gross, ratingValue, ratingCount,
     * duration, castList, characterList
     *
     * Each attribute is treated as a String and names ending in 'list' like
     * 'genreList' refer to JSON lists.
     *
     * @param moviesDir
     *          The directory containing the set of JSON files, each ending with a
     *          suffix ".json".
     * @return A list of movies
     * @throws IOException
     */
    public static List<Movie> readMoviesFrom(final File moviesDir) throws IOException {
        final List<Movie> movies = new ArrayList<>();

        if (moviesDir == null) {
            System.out.println("NullPointerException: No such directory.");
            System.exit(-1);
        }
        if (moviesDir.listFiles() == null) {
            System.out.println("NullPointerException: No such directory.");
            System.exit(-1);
        }

        for (final File file : moviesDir.listFiles()) {
            if (file.getName().endsWith(".json")) {
                try (JsonReader reader = Json.createReader(new FileInputStream(file))) {
                    final JsonArray movie = reader.readArray();
                    if (movie.size() > 0) {
                        JsonObject m = (JsonObject) movie.get(0);
                        Movie obj = new Movie();
                        obj.setTitle(getString(m, "title"));
                        obj.setYear(getString(m, "year"));
                        obj.setUrl(getString(m, "url"));
                        obj.setGenreList(getJsonArray(m, "genreList"));
                        obj.setCountryList(getJsonArray(m, "countryList"));
                        obj.setDescription(getString(m, "description"));
                        obj.setBudget(getString(m, "budget"));
                        obj.setGross(getString(m, "gross"));
                        obj.setRatingValue(getString(m, "ratingValue"));
                        obj.setRatingCount(getString(m, "ratingCount"));
                        obj.setDuration(getString(m, "duration"));
                        obj.setCastList(getJsonArray(m, ("castList")));
                        obj.setCharacterList(getJsonArray(m, ("characterList")));
                        obj.setDirectorList(getJsonArray(m, "directorList"));
                        movies.add(obj);
                    }
                }
            }
        }
        return movies;
    }

    /**
     * A helper function to parse a JSON array.
     *
     * @param m
     *          The JSON object, containing an array under the attribute 'key'.
     * @param key
     *          The key of the array
     * @return A list containing the Strings in the JSON object.
     */
    private static List<String> getJsonArray(final JsonObject m, final String key) {
        try {
            final JsonArray array = m.getJsonArray(key);
            final List<String> result = new ArrayList<>();
            for (final JsonValue v : array) {
                result.add(((JsonString) v).getString());
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * A helper function to parse a JSON String.
     *
     * @param m
     *          The JSON object, containing a String under the attribute 'key'.
     * @param key
     *          The key of the array
     * @return The String in the JSON object.
     */
    private static String getString(JsonObject m, String key) {
        try {
            Object o = m.getString(key);
            if (o != null) {
                return (String) o;
            }
        } catch (final NullPointerException np) {
            System.out.println("NullPointerException: " + np.getMessage());
            System.exit(-1);
        } finally {
        }
        return "";
    }
}
