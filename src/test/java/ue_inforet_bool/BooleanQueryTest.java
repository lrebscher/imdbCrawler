package ue_inforet_bool;

import static ue_inforet_bool_wordnet_study.QueryExtender.modifiyQuery;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Created by lrebscher on 03.12.16.
 */
public class BooleanQueryTest {

    @Test
    public void parseQuery1() throws Exception {
        HashMap<String, Collection<String>> map = new HashMap<>();
        String[] synonyms = { "test", "fuck", "bae" };
        map.put("wall", Arrays.asList(synonyms));

        final String queryString = "plot:Elvis AND plot:is AND plot:wall AND type:movie";
        final String modifiedQuery = modifiyQuery(map, queryString);
        Assert.assertEquals("plot:Elvis AND plot:is AND (plot:wall OR plot:test OR plot:fuck OR plot:bae) AND type:movie", modifiedQuery);
    }

    @Test
    public void parseQuery2() throws Exception {
        HashMap<String, Collection<String>> map = new HashMap<>();

        String[] synonyms = { "test", "fuck", "bae" };
        String[] synonyms1 = { "Trick", "Truck", "Trock" };
        String[] synonyms2 = { "Weapon" };

        map.put("star", Arrays.asList(synonyms));
        map.put("trek", Arrays.asList(synonyms1));
        map.put("wars", Arrays.asList(synonyms2));

        final String queryString = "(title:Star AND title:Trek) OR (title:Star AND title:Wars) AND year:2016";
        final String modifiedQuery = modifiyQuery(map, queryString);
        Assert.assertEquals(
            "((title:Star OR title:test OR title:fuck OR title:bae) AND (title:Trek OR title:Trick OR title:Truck OR title:Trock)) OR ((title:Star OR title:test OR title:fuck OR title:bae) AND (title:Wars OR title:Weapon)) AND year:2016",
            modifiedQuery);
    }


}