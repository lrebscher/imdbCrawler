package ue_inforet_bool_wordnet_study;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by lrebscher on 25.01.17.
 */
public final class QueryExtender
{
	private QueryExtender() {}

	public static String modifiyQuery(final Map<String, Collection<String>> map, final String queryString)
	{
		final String[] searchParts = queryString.split("AND|OR|NOT");
		final HashSet<String> hashSet = new HashSet<>();

		String modifiedQuery = queryString;

		for(final String part : searchParts)
		{
			final String lowerCasePart = part.trim().replace("(", "").replace(")", "");
			final String term = lowerCasePart.substring(lowerCasePart.indexOf(':') + 1);
			final String key = lowerCasePart.substring(0, lowerCasePart.indexOf(':'));

			if(map.containsKey(term))
			{
				final List<String> synonyms = (List<String>)map.get(term);
				if(!synonyms.isEmpty())
				{
					final StringBuilder stringBuilder = new StringBuilder("(");
					stringBuilder.append(key).append(":").append(term).append(" OR ");

					for(final String synonym : synonyms)
					{
						if(synonyms.indexOf(synonym) == synonyms.size() - 1)
						{
							//last element
							stringBuilder.append(key).append(":").append(synonym).append(")");
						}
						else
						{
							stringBuilder.append(key).append(":").append(synonym).append(" OR ");
						}
					}

					if(!hashSet.contains(key + ":" + term))
					{
						modifiedQuery = modifiedQuery.replace(key + ":" + term, stringBuilder.toString());
						hashSet.add(key + ":" + term);
					}
				}
			}
		}
		return modifiedQuery;
	}
}
