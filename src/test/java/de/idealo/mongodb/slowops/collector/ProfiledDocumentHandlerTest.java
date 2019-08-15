package de.idealo.mongodb.slowops.collector;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import org.bson.Document;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Created by kay.agahd on 24.10.16.
 */
public class ProfiledDocumentHandlerTest {

    @Test
    public void testFilterDoc() {

        ProfiledDocumentHandler dh = new ProfiledDocumentHandler(new ServerAddress("127.0.0.1"));

        /*
        profiling entry of "query" or "query.query" or "query.$query" or "command.query" or "query.filter":

         {
            "fieldSingleValue" : 123,
            "fieldMustExist" : {$exists:1},
            "fieldWithRange" : {$gt:20, $lt:30},
            "fieldWithIn" : {$in:[1,2,3,4]},
            "$and" : {$and:[{x:3},{y:5}]},
            "$and$or" : {$and:[{x:3},{$or: [{a:7},{b:9}] }]},
            "fieldWithElemMatch" : {$elemMatch: {
                                        "f1" : "v1",
                                        "f2" : "v2",
                                        "year" : {$gt:2015}}}
          }



         extracted fields (max 1 level deep) should be:
            "fieldSingleValue",
            "fieldMustExist.$exists",
            "fieldWithRange.$gt.$lt"},
            "fieldWithIn.$in",
            "$and: x|y.$and" //more than 1 level deep
            "$and$or: x|a|b.$or.$and" //more than 1 level deep
            "fieldWithElemMatch.$elemMatch"
         this would be too detailed:
            "fieldWithElemMatch.$elemMatch.f1.f2.year.$gt"
         */

        Document fields = new Document("fieldSingleValue", 123)
                .append("fieldMustExist", new Document("$exists", 1))
                .append("fieldWithRange", new Document("$gt", 20).append("$lt", 30))
                .append("fieldWithIn", new Document("$in", Lists.newArrayList(1,2,3,4)))
                .append("$and", Lists.newArrayList(new Document("x", 1),new Document("y", 2)))
                .append("$and2", Lists.newArrayList(new Document("x", 3),new Document("$or", Lists.newArrayList(new Document("a", 7),new Document("b", 9))   )))
                .append("fieldWithElemMatch", new Document("$elemMatch", new Document("f1", "v1")
                        .append("f2", "v2")
                        .append("year", new Document("$gt", 2015))))
        ;


        Set<String> fieldsExpected = new HashSet<String>();
        fieldsExpected.add("fieldSingleValue");
        fieldsExpected.add("fieldMustExist.$exists");
        fieldsExpected.add("fieldWithRange.$gt.$lt");
        fieldsExpected.add("fieldWithIn.$in");
        fieldsExpected.add("x|y.$and");
        fieldsExpected.add("x|a|b.$or.$and2");
        fieldsExpected.add("fieldWithElemMatch.$elemMatch");


        Document doc = new Document("query",
                               fields
                        );
        assertEquals(fieldsExpected, dh.filterDoc(doc).fields);

        doc = new Document("query",
                new Document("query",
                        fields
                ));
        assertEquals(fieldsExpected, dh.filterDoc(doc).fields);

        doc = new Document("query",
                new Document("$query",
                        fields
                ));
        assertEquals(fieldsExpected, dh.filterDoc(doc).fields);

        doc = new Document("command",
                new Document("query",
                        fields
                ));
        assertEquals(fieldsExpected, dh.filterDoc(doc).fields);

        doc = new Document("query",
                new Document("filter",
                        fields
                ));
        assertEquals(fieldsExpected, dh.filterDoc(doc).fields);


        //test "op"-field

        doc = new Document("op", "query")
                .append("query",
                        new Document("find", "offer")
                        .append("filter", new Document("_id", 1)));
        assertEquals("query", dh.filterDoc(doc).op);

        doc = new Document("op", "update")
                .append("query",
                        new Document("_id", 1));
        assertEquals("update", dh.filterDoc(doc).op);

        doc = new Document("op", "insert")
                .append("query",
                        new Document("insert", "offer")
                        .append("documents", "foo"));
        assertEquals("insert", dh.filterDoc(doc).op);

        doc = new Document("op", "remove")
                .append("query",
                        new Document("_id", 3));
        assertEquals("remove", dh.filterDoc(doc).op);

        doc = new Document("op", "command")
                .append("command",
                    new Document("count", "offer")
                    .append("query", new Document("field", "value"))
                    .append("fields", new Document()));
        assertEquals("command.count", dh.filterDoc(doc).op);

        doc = new Document("op", "command")
                .append("command",
                        new Document("distinct", "offer")
                        .append("key", "someFieldToBeDistinctlySelected")
                        .append("query", new Document()));
        assertEquals("command.distinct", dh.filterDoc(doc).op);

        doc = new Document("op", "command")
                .append("command",
                        new Document("collStats", "offer")
                        .append("scale", null));
        assertEquals("command.collStats", dh.filterDoc(doc).op);


        doc = new Document("op", "command")
                .append("command",
                        new Document("deleteIndexes", "offer")
                        .append("index", new Document("debugMD5", 1)));
        assertEquals("command.deleteIndexes", dh.filterDoc(doc).op);


        doc = new Document("op", "command")
                .append("command",
                        new Document("findAndModify", "offer")
                                .append("query", new Document("field", "value"))
                                .append("update", new Document()));
        ProfilingEntry entry = dh.filterDoc(doc);
        assertEquals("command.findAndModify", entry.op);
        fieldsExpected = new HashSet<String>();
        fieldsExpected.add("field");
        assertEquals(fieldsExpected, entry.fields);


        doc = new Document("op", "command")
                .append("command",
                        new Document("aggregate", "offer")
                                .append("pipeline", Lists.newArrayList(
                                        new Document("$match", new Document("matchingFP", "MAPPED")),
                                        new Document("$group", new Document("_id", "$shopId")),
                                        new Document("$group", new Document("_id", 1)
                                                                .append("count", new Document("$sum", 1)))
                                ))
                                .append("cursor", new Document()));
        entry = dh.filterDoc(doc);
        assertEquals("command.aggregate", entry.op);
        fieldsExpected = new HashSet<String>();
        fieldsExpected.add("$match.matchingFP");
        fieldsExpected.add("$group._id");
        fieldsExpected.add("$group._id.count");
        assertEquals(fieldsExpected, entry.fields);
        //the same using hamcrest:
        MatcherAssert.assertThat(fieldsExpected, CoreMatchers.is(entry.fields));


    }

}