package ue_inforet_crawler; /**
 * Created by Daniel on 15.11.2016.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;

public class MovieWriter {

    private final ArrayList<JsonObject> objects;

    private final String directory;

    private String filename;

    public MovieWriter(final String directory) {
        this.directory = directory;

        objects = new ArrayList<>(1);
        filename = "";
    }

    private static JsonArray createJsonArray(final Iterable<String> list) {
        final JsonArrayBuilder arrBuilder = Json.createArrayBuilder();

        for (final String s : list) {
            arrBuilder.add(s);
        }
        return arrBuilder.build();
    }

    public void addMovie(final Movie movie) {
        final JsonObjectBuilder objBuilder = Json.createObjectBuilder();

        objBuilder.add("title", movie.getTitle());
        objBuilder.add("year", movie.getYear());
        objBuilder.add("genreList", createJsonArray(movie.getGenreList()));
        objBuilder.add("countryList", createJsonArray(movie.getCountryList()));
        objBuilder.add("description", movie.getDescription());
        objBuilder.add("budget", movie.getBudget());
        objBuilder.add("gross", movie.getGross());
        objBuilder.add("ratingValue", movie.getRatingValue());
        objBuilder.add("ratingCount", movie.getRatingCount());
        objBuilder.add("duration", movie.getDuration());
        objBuilder.add("castList", createJsonArray(movie.getCastList()));
        objBuilder.add("characterList", createJsonArray(movie.getCharacterList()));
        objBuilder.add("directorList", createJsonArray(movie.getDirectorList()));
        objBuilder.add("url", movie.getUrl());

        if (objects.isEmpty()) {
            filename = movie.getTitle().toLowerCase();
        }
        objects.add(objBuilder.build());
    }

    public void writeFile() {
        writeFile(this.filename);
    }

    private void writeFile(final String filename) {
        // final Map<String, Object> properties = new HashMap<>(1);
        final String outPath = this.directory.concat(this.filename.replaceAll("[^a-z0-9]", "")).concat(".json");
        final File outDir = new File(directory);

        try {
            if (!outDir.exists()) {
                outDir.mkdir();
            }
        } catch (final SecurityException s) {
            System.out.println("SecurityException: " + s.getMessage());
            System.exit(-1);
        }

        // properties.put(JsonGenerator.PRETTY_PRINTING, true);
        // final JsonGeneratorFactory factory = Json.createGeneratorFactory(properties);

        try (JsonGenerator generator = Json.createGenerator(new OutputStreamWriter(new FileOutputStream(new File(outPath)), "UTF-8"))) {
            generator.writeStartArray();

            if (!objects.isEmpty()) {
                for (final JsonObject movie : objects) {
                    // generator.writeStartObject();
                    generator.write(movie);
                    // generator.writeEnd();
                }
            }

            generator.writeEnd();
            generator.close();
        } catch (final JsonGenerationException jg) {
            System.out.println("JsonGenerationException: " + jg.getMessage());
            System.exit(-1);
        } catch (final IOException io) {
            System.out.println("IOException: " + io.getMessage());
            System.exit(-1);
        }
    }
}
