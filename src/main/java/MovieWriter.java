/**
 * Created by Daniel on 15.11.2016.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
// import java.util.Map;
// import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;
// import javax.json.stream.JsonGeneratorFactory;

public class MovieWriter
{
	private ArrayList<JsonObject> objects;
	private String directory;
	private String filename;

	public MovieWriter()
	{
		this(".".concat(File.separator).concat("data").concat(File.separator));
	}

	public MovieWriter(final String directory)
	{
		this.objects = new ArrayList<>(1);
		this.directory = directory;
		this.filename = "";
	}

	public MovieWriter(final String directory, final String filename)
	{
		this.objects = new ArrayList<>(1);
		this.directory = directory;
		this.filename = filename;
	}

	public void setFilename(final String filename) { this.filename = filename; }
	public void setDirectory(final String directory) { this.directory = directory; }

	private JsonArray createJsonArray(final List<String> list)
	{
		JsonArrayBuilder arrBuilder = Json.createArrayBuilder();

		for(final String s : list) arrBuilder.add(s);
		return arrBuilder.build();
	}

	public void addMovie(Movie movie)
	{
		JsonObjectBuilder objBuilder = Json.createObjectBuilder();

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
		if(objects.isEmpty()) this.filename = movie.getTitle().toLowerCase();
		this.objects.add(objBuilder.build());
	}

	public void writeFile() { writeFile(this.filename); }

	public void writeFile(final String filename)
	{
		// final Map<String, Object> properties = new HashMap<>(1);
		final String outPath = this.directory.concat(this.filename.replaceAll("[^a-z0-9]", "")).concat(".json");
		final File outDir = (new File(this.directory));

		try { if(!outDir.exists()) outDir.mkdir(); }
		catch(final SecurityException s) { System.out.println("SecurityException: " + s.getMessage()); System.exit(-1); }

		// properties.put(JsonGenerator.PRETTY_PRINTING, true);
		// final JsonGeneratorFactory factory = Json.createGeneratorFactory(properties);

		try(JsonGenerator generator = Json.createGenerator(new OutputStreamWriter(new FileOutputStream(new File(outPath)), "UTF-8")))
		{
			generator.writeStartArray();

			if(!this.objects.isEmpty())
			{
				for(JsonObject movie : this.objects)
				{
					// generator.writeStartObject();
					generator.write(movie);
					// generator.writeEnd();
				}
			}

			generator.writeEnd();
			generator.close();
		}
		catch(final JsonGenerationException jg) { System.out.println("JsonGenerationException: " + jg.getMessage()); System.exit(-1); }
		catch(final IOException io) { System.out.println("IOException: " + io.getMessage()); System.exit(-1); }
	}
}
