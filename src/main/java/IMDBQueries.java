import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("static-method")
public class IMDBQueries {

    /**
     * A helper class for pairs of objects of generic types 'K' and 'V'.
     *
     * @param <K>
     *          first value
     * @param <V>
     *          second value
     */
    class Tuple<K, V> {

        K first;

        V second;

        public Tuple(K f, V s) {
            this.first = f;
            this.second = s;
        }

        @Override
        public int hashCode() {
            return this.first.hashCode() + this.second.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this.first.equals(((Tuple<?, ?>) obj).first)
                && this.second.equals(((Tuple<?, ?>) obj).second);
        }
    }

    /**
     * All-rounder: Determine all movies in which the director stars as an actor
     * (cast). Return the top ten matches sorted by decreasing IMDB rating.
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return top ten movies and the director, sorted by decreasing IMDB rating
     */
    private List<Tuple<Movie, String>> queryAllRounder(final Collection<Movie> movies) {
        return movies.stream()
                     //movies raussuchen, für die gilt: director stars as an actor
                     .filter(movie -> {
                         //Schnittoperation auf die beiden Mengen, siehe doku zu retiainAll für mehr Infos
                         movie.getDirectorList().retainAll(movie.getCastList());
                         return !movie.getDirectorList().isEmpty();
                     })
                     //sortiere nach Rating
                     .sorted((o1, o2) -> {
                         double ratingValue1 = Double.valueOf(o1.getRatingValue());
                         double ratingValue2 = Double.valueOf(o2.getRatingValue());
                         if (ratingValue1 < ratingValue2) {
                             return 1;
                         } else if (ratingValue1 > ratingValue2) {
                             return -1;
                         } else {
                             return 0;
                         }
                     })
                     //map von Movie auf Tuple<Movie,String>, nimm dafür den ersten Director aus der Liste, da in der Query nur nach einem gefragt wird
                     .map(movie -> {
                         final String director = movie.getDirectorList().get(0);
                         return new Tuple<>(movie, director);
                     })
                     //top 10
                     .limit(10L)
                     //Ergebnisse sammeln und als Liste zurückgeben
                     .collect(Collectors.toList());
    }

    /**
     * Under the Radar: Determine the top ten US-American movies until (including)
     * 2015 that have made the biggest loss despite an IMDB score above
     * (excluding) 8.0, based on at least 1,000 votes. Here, loss is defined as
     * budget minus gross.
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return top ten highest rated US-American movie until 2015, sorted by
     *         monetary loss, which is also returned
     */
    private List<Tuple<Movie, Long>> queryUnderTheRadar(List<Movie> movies) {
        return movies.stream()
                     .filter(movie -> Integer.valueOf(movie.getYear()) <= 2015)
                     .filter(movie -> Float.valueOf(movie.getRatingValue()) > 8.0 && Utils.parseNumber(movie.getRatingCount()) >= 1000)
                     .map(movie -> {
                         //format: $313,837,577
                         final Long budget = Utils.parseNumber(movie.getBudget());
                         final Long gross = Utils.parseNumber(movie.getGross());

                         //loss := budget - gross, will be positive if movie is a flop
                         final Long loss = budget - gross;

                         return new Tuple<>(movie, loss);
                     })
                     .sorted((o1, o2) -> {
                         if (o1.second < o2.second) {
                             return 1;
                         } else if (o1.second > o2.second) {
                             return -1;
                         } else {
                             return 0;
                         }
                     })
                     .limit(10)
                     .collect(Collectors.toList());
    }

    /**
     * The Pillars of Storytelling: Determine all movies that contain both
     * (sub-)strings "kill" and "love" in their lowercase description
     * (String.toLowerCase()). Sort the results by the number of appearances of
     * these strings and return the top ten matches.
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return top ten movies, which have the words "kill" and "love" as part of
     *         their lowercase description, sorted by the number of appearances of
     *         these words, which is also returned.
     */
    private List<Tuple<Movie, Integer>> queryPillarsOfStorytelling(final Collection<Movie> movies) {
        return movies.stream()
                     .filter(movie -> {
                         final String lowerCaseDescription = movie.getDescription().toLowerCase();
                         return lowerCaseDescription.contains("kill") && lowerCaseDescription.contains("love");
                     })
                     .map(movie -> {
                         final String lowerCaseDescription = movie.getDescription().toLowerCase();

                         //credits to http://stackoverflow.com/a/8910767
                         int killCount = lowerCaseDescription.length() - lowerCaseDescription.replace("kill", "").length();
                         int loveCount = lowerCaseDescription.length() - lowerCaseDescription.replace("love", "").length();
                         return new Tuple<Movie, Integer>(movie, killCount + loveCount);
                     })
                     .limit(10)
                     .collect(Collectors.toList());
    }

    /**
     * The Red Planet: Determine all movies of the Sci-Fi genre that mention
     * "Mars" in their description (case-aware!). List all found movies in
     * ascending order of publication (year).
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return list of Sci-Fi movies involving Mars in ascending order of
     *         publication.
     */
    private List<Movie> queryRedPlanet(final Collection<Movie> movies) {
        return movies.stream()
                     .filter(movie -> movie.getGenreList().contains("Sci-Fi"))
                     .filter(movie -> movie.getDescription().contains("Mars"))
                     .sorted((m1, m2) -> Integer.valueOf(m1.getYear()) > Integer.valueOf(m2.getYear()) ? 1 : 0)
                     .collect(Collectors.toList());
    }

    /**
     * Colossal Failure: Determine all US-American movies with a duration beyond 2
     * hours, a budget beyond 1 million and an IMDB rating below 5.0. Sort results
     * by ascending IMDB rating.
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return list of US-American movies with high duration, large budgets and a
     *         bad IMDB rating, sorted by ascending IMDB rating
     */
    private List<Movie> queryColossalFailure(final Collection<Movie> movies) {
        //duration format: 2h 10min, duration can be empty
        return movies.stream()
                     .filter(movie -> !movie.getDuration().isEmpty()
                         //beyond 2 hours, check first char (hour) if bigger or equal than 2, assume there is no movie with 2 digits hours
                         && Integer.valueOf(movie.getDuration().charAt(0)) >= 2
                         //budget over 1 mio.
                         && Utils.parseNumber(movie.getBudget()) > 1000000
                         //rating below 5.0
                         && Float.valueOf(movie.getRatingValue()) < 5.0)
                     //sort ascending by rating
                     .sorted((m1, m2) -> {
                         float rating1 = Float.valueOf(m1.getRatingValue());
                         float rating2 = Float.valueOf(m2.getRatingValue());

                         if (rating1 < rating2) {
                             return -1;
                         } else if (rating1 > rating2) {
                             return 1;
                         } else {
                             return 0;
                         }
                     })
                     .collect(Collectors.toList());
    }

    /**
     * Uncreative Writers: Determine the 10 most frequent character names of all
     * times ordered by frequency of occurrence. Filter any lowercase names
     * containing substrings "himself", "doctor", and "herself" from the result.
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return the top 10 character names and their frequency of occurrence;
     *         sorted in decreasing order of frequency
     */
    private List<Tuple<String, Integer>> queryUncreativeWriters(final List<Movie> movies) {
        //idea, hashMap with count
        final Map<String, Integer> charactersCount = new HashMap<>();

        movies.stream()
              .forEach(movie -> movie.getCharacterList()
                                     .forEach(character -> {
                                         if (!character.toLowerCase().isEmpty()
                                             && !character.toLowerCase().contains("herself")
                                             && !character.toLowerCase().contains("himself")
                                             && !character.toLowerCase().contains("doctor")) {

                                             if (charactersCount.containsKey(character)) {
                                                 charactersCount.put(character, charactersCount.get(character) + 1);
                                             } else {
                                                 charactersCount.put(character, 1);
                                             }
                                         }
                                     }));

        return charactersCount.entrySet()
                              .stream()
                              .sorted((o1, o2) -> {
                                  if (o1.getValue() > o2.getValue()) {
                                      return -1;
                                  } else if (o1.getValue() < o2.getValue()) {
                                      return 1;
                                  } else {
                                      return 0;
                                  }
                              })
                              .limit(10)
                              .map(entry -> new Tuple<>(entry.getKey(), entry.getValue()))
                              .collect(Collectors.toList());
    }

    /**
     * Workhorse: Provide a ranked list of the top ten most active actors (i.e.
     * starred in most movies) and the number of movies they played a role in.
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return the top ten actors and the number of movies they had a role in,
     *         sorted by the latter.
     */
    private List<Tuple<String, Integer>> queryWorkHorse(final List<Movie> movies) {
        final Map<String, Integer> actorMap = new HashMap<>();

        movies.stream()
              .forEach(movie -> movie.getCastList().forEach(actor -> {
                  if (!actor.isEmpty()) {
                      if (actorMap.containsKey(actor)) {
                          actorMap.put(actor, actorMap.get(actor) + 1);
                      } else {
                          actorMap.put(actor, 1);
                      }
                  }
              }));

        return actorMap.entrySet()
                       .stream()
                       .sorted((o1, o2) -> {
                           if (o1.getValue() > o2.getValue()) {
                               return -1;
                           } else if (o1.getValue() < o2.getValue()) {
                               return 1;
                           } else {
                               return 0;
                           }
                       })
                       .limit(10)
                       .sorted((a1, a2) -> a1.getKey().compareToIgnoreCase(a2.getKey()))
                       .map(entry -> new Tuple<>(entry.getKey(), entry.getValue()))
                       .collect(Collectors.toList());
    }

    /**
     * Must See: List the best-rated movie of each year starting from 1990 until
     * (including) 2010 with more than 10,000 ratings. Order the movies by
     * ascending year.
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return best movies by year, starting from 1990 until 2010.
     */
    private List<Movie> queryMustSee(final Collection<Movie> movies) {
        final List<Movie> sortedMovies = new ArrayList<>(20);
        for (int year = 1990; year <= 2010; year++) {
            final int finalYear = year;
            final Optional<Movie> optionalLowestMovie = movies.stream()
                                                              .filter(movie -> Integer.valueOf(movie.getYear()) == finalYear
                                                                  && Utils.parseNumber(movie.getRatingCount()) > 10000)
                                                              .max((m1, m2) -> Double.compare(Double.valueOf(m1.getRatingValue()),
                                                                  Double.valueOf(m2.getRatingValue())));
            if (optionalLowestMovie.isPresent()) {
                sortedMovies.add(optionalLowestMovie.get());
            }
        }
        return sortedMovies;
    }

    /**
     * Rotten Tomatoes: List the worst-rated movie of each year starting from 1990
     * till (including) 2010 with an IMDB score larger than 0. Order the movies by
     * increasing year.
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return worst movies by year, starting from 1990 till (including) 2010.
     */
    private static List<Movie> queryRottenTomatoes(final Collection<Movie> movies) {
        final List<Movie> sortedMovies = new ArrayList<>(20);
        for (int year = 1990; year <= 2010; year++) {
            final int finalYear = year;
            final Optional<Movie> optionalLowestMovie = movies.stream()
                                                              .filter(movie -> Integer.valueOf(movie.getYear()) == finalYear
                                                                  && Double.valueOf(movie.getRatingValue()) > 0.0)
                                                              .min((m1, m2) -> Double.compare(Double.valueOf(m1.getRatingValue()),
                                                                  Double.valueOf(m2.getRatingValue())));
            if (optionalLowestMovie.isPresent()) {
                sortedMovies.add(optionalLowestMovie.get());
            }
        }
        return sortedMovies;
    }

    /**
     * Magic Couples: Determine those couples that feature together in the most
     * movies. E.g., Adam Sandler and Allen Covert feature together in multiple
     * movies. Report the top ten pairs of actors, their number of movies and sort
     * the result by the number of movies.
     *
     * @param movies
     *          the list of movies which is to be queried
     * @return report the top 10 pairs of actors and the number of movies they
     *         feature together. Sort by number of movies.
     */
    public List<Tuple<Tuple<String, String>, Integer>> queryMagicCouple(
        List<Movie> movies) {
        // TODO Impossibly Hard Query: insert code here
        return new ArrayList<>();
    }

    public static void main(String argv[]) throws IOException {
        // String moviesPath = ".".concat(File.separator).concat("data").concat(File.separator).concat("movies").concat(File.separator);
        String moviesPath = ".".concat(File.separator).concat("data").concat(File.separator);

        if (argv.length == 1) {
            moviesPath = argv[0];
        } else if (argv.length != 0) {
            System.out.println("Call with: IMDBQueries.jar <moviesPath>");
            System.exit(0);
        }

        List<Movie> movies = MovieReader.readMoviesFrom(new File(moviesPath));

        System.out.println("All-rounder");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Movie, String>> result = queries.queryAllRounder(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() == 10) {
                for (Tuple<Movie, String> tuple : result) {
                    System.out.println("\t" + tuple.first.getRatingValue() + "\t"
                        + tuple.first.getTitle() + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Under the radar");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Movie, Long>> result = queries.queryUnderTheRadar(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<Movie, Long> tuple : result) {
                    System.out.println("\t" + tuple.first.getTitle() + "\t"
                        + tuple.first.getRatingCount() + "\t"
                        + tuple.first.getRatingValue() + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("The pillars of storytelling");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Movie, Integer>> result = queries
                .queryPillarsOfStorytelling(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<Movie, Integer> tuple : result) {
                    System.out.println("\t" + tuple.first.getTitle() + "\t"
                        + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("The red planet");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryRedPlanet(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Movie movie : result) {
                    System.out.println("\t" + movie.getTitle());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("ColossalFailure");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryColossalFailure(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Movie movie : result) {
                    System.out.println("\t" + movie.getTitle() + "\t"
                        + movie.getRatingValue());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Uncreative writers");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<String, Integer>> result = queries
                .queryUncreativeWriters(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<String, Integer> tuple : result) {
                    System.out.println("\t" + tuple.first + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Workhorse");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<String, Integer>> result = queries.queryWorkHorse(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && result.size() <= 10) {
                for (Tuple<String, Integer> actor : result) {
                    System.out.println("\t" + actor.first + "\t" + actor.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Must see");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = queries.queryMustSee(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && !result.isEmpty()) {
                for (Movie m : result) {
                    System.out.println("\t" + m.getYear() + "\t" + m.getRatingValue()
                        + "\t" + m.getTitle());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Rotten tomatoes");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Movie> result = IMDBQueries.queryRottenTomatoes(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty() && !result.isEmpty()) {
                for (Movie m : result) {
                    System.out.println("\t" + m.getYear() + "\t" + m.getRatingValue()
                        + "\t" + m.getTitle());
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
        }
        System.out.println("");

        System.out.println("Magic Couples");
        {
            IMDBQueries queries = new IMDBQueries();
            long time = System.currentTimeMillis();
            List<Tuple<Tuple<String, String>, Integer>> result = queries
                .queryMagicCouple(movies);
            System.out.println("Time:" + (System.currentTimeMillis() - time));

            if (result != null && !result.isEmpty()) {
                for (Tuple<Tuple<String, String>, Integer> tuple : result) {
                    System.out.println("\t" + tuple.first.first + ":"
                        + tuple.first.second + "\t" + tuple.second);
                }
            } else {
                System.out.println("Error? Or not implemented?");
            }
            System.out.println("");

        }
    }
}