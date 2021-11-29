package de.idealo.mongodb.slowops.collector;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import org.bson.Document;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.LinkedHashSet;


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
            "fieldWithSubDoc" : {"productId":123, "siteId":2},
            "fieldWithSubSubDoc" : {"productId":{"foo":123, "siteId":2}},
            "$and" : {$and:[{x:3},{y:5}]},
            "$and$or" : {$and:[{x:3},{$or: [{a:7},{b:9}] }]},
            "$orSameFields" : {$or: [{x:3}, {x:5}] },
            "$orSameFieldsInSubDocs" : {$or: [{x:{a:1, b:2}}, {x:{a:3, b:4}}] },
            "fieldWithElemMatch" : {$elemMatch: {
                                        "f1" : "v1",
                                        "f2" : "v2",
                                        "year" : {$gt:2015}}},
            "fieldWithInObjects" : {$in:[{a:1}, {b:2}]},
            "fieldWithInOneObject" : {$in:[{a:1}]},
            "fieldWithInIdenticalObjects" : {$in:[{a:1}, {a:2}]},
            "fieldWithInComplexObjects" : {$in:[{a:1, b:2}, {x:3, y:4}]},
            "fieldWithInComplexOneObject" : {$in:[{a:1, b:2}]},
            "fieldWithInComplexIdenticalObjects" : {$in:[{a:1, b:2}, {a:3, b:4}]},
          }



         extracted fields should be:
            "fieldSingleValue",
            "fieldMustExist.$exists",
            "fieldWithRange{$gt,$lt}",
            "fieldWithIn.$in",
            "fieldWithSubDoc{productId,siteId}"
            "fieldWithSubSubDoc.productId{foo,siteId}"
            "$and[x,y]"
            "$and[$or[a,b], x]" //x is last because elements are sorted within []
            "$orSameFields" : {$or: [x*] },
            "$orSameFieldsInSubDocs" : {$or: [{x{a, b}*},
            "fieldWithElemMatch.$elemMatch{year.$gt,f1,f2}"
            "fieldWithInObjects.$in[a, b]",
            "fieldWithInOneObject.$in[a]",
            "fieldWithInIdenticalObjects" : {$in:[a*]},
            "fieldWithInComplexObjects.$in[{a, b}, {x, y}]",
            "fieldWithInComplexOneObject" : {$in:[{a, b}]},
            "fieldWithInComplexIdenticalObjects" : {$in:[{a, b}*]},
         */

        Document fields = new Document("fieldSingleValue", 123)
                .append("fieldMustExist", new Document("$exists", 1))
                .append("fieldWithRange", new Document("$gt", 20).append("$lt", 30))
                .append("fieldWithIn", new Document("$in", Lists.newArrayList(1,2,3,4)))
                .append("fieldWithSubDoc", new Document("productId", 123).append("siteId", 2))
                .append("fieldWithSubSubDoc", new Document("productId", new Document("foo", 123).append("siteId", 2)))
                .append("$and", Lists.newArrayList(new Document("x", 1),new Document("y", 2)))
                .append("$and$or", Lists.newArrayList(new Document("x", 3),new Document("$or", Lists.newArrayList(new Document("a", 7),new Document("b", 9))   )))
                .append("$orSameFields", Lists.newArrayList(new Document("x", 1),new Document("x", 2)))
                .append("$orSameFieldsInSubDocs", Lists.newArrayList(new Document("x", new Document("a", 1).append("b", 2)), new Document("x", new Document("a", 3).append("b", 4))))
                .append("fieldWithElemMatch", new Document("$elemMatch", new Document("f1", "v1")
                        .append("f2", "v2")
                        .append("year", new Document("$gt", 2015))))
                .append("fieldWithInObjects", new Document("$in", Lists.newArrayList(new Document("a", 1),new Document("b", 2))  ))
                .append("fieldWithInOneObject", new Document("$in", Lists.newArrayList(new Document("a", 1))  ))
                .append("fieldWithInIdenticalObjects", new Document("$in", Lists.newArrayList(new Document("a", 1),new Document("a", 2))  ))
                .append("fieldWithInComplexObjects", new Document("$in", Lists.newArrayList(new Document("a", 1).append("b", 2), new Document("x", 3).append("y", 4))))
                .append("fieldWithInComplexOneObject", new Document("$in", Lists.newArrayList(new Document("a", 1).append("b", 2))))
                .append("fieldWithInComplexIdenticalObjects", new Document("$in", Lists.newArrayList(new Document("a", 1).append("b", 2), new Document("a", 3).append("b", 4))))
                ;


        LinkedHashSet<String> fieldsExpected = new LinkedHashSet<>();
        fieldsExpected.add("fieldSingleValue");
        fieldsExpected.add("fieldMustExist.$exists");
        fieldsExpected.add("fieldWithRange{$gt, $lt}");
        fieldsExpected.add("fieldWithIn.$in");
        fieldsExpected.add("fieldWithSubDoc{productId, siteId}");
        fieldsExpected.add("fieldWithSubSubDoc.productId{foo, siteId}");
        fieldsExpected.add("$and[x, y]");
        fieldsExpected.add("$and$or[$or[a, b], x]");
        fieldsExpected.add("$orSameFields[x*]");
        fieldsExpected.add("$orSameFieldsInSubDocs[x{a, b}*]");
        fieldsExpected.add("fieldWithElemMatch.$elemMatch{f1, f2, year.$gt}");
        fieldsExpected.add("fieldWithInObjects.$in[a, b]");
        fieldsExpected.add("fieldWithInOneObject.$in[a]");
        fieldsExpected.add("fieldWithInIdenticalObjects.$in[a*]");
        fieldsExpected.add("fieldWithInComplexObjects.$in[{a, b}, {x, y}]");
        fieldsExpected.add("fieldWithInComplexOneObject.$in[{a, b}]");
        fieldsExpected.add("fieldWithInComplexIdenticalObjects.$in[{a, b}*]");


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
        fieldsExpected = new LinkedHashSet<>();
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
        fieldsExpected = new LinkedHashSet<>();
        fieldsExpected.add("$match.matchingFP");
        fieldsExpected.add("$group._id");
        fieldsExpected.add("$group{_id, count.$sum}");
        assertEquals(fieldsExpected, entry.fields);
        //the same using hamcrest:
        MatcherAssert.assertThat(fieldsExpected, CoreMatchers.is(entry.fields));


    }

}