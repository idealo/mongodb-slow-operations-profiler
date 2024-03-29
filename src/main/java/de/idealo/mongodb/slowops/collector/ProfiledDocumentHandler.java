package de.idealo.mongodb.slowops.collector;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import org.bson.Document;

import java.util.*;

/**
 * Created by kay.agahd on 24.10.16.
 */
public class ProfiledDocumentHandler {

    private final ServerAddress serverAddress;


    public ProfiledDocumentHandler(ServerAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    //5 examples of how the field "query" may be formatted:
    //query:
    //{ "query" : { "shopId" : 279073, "onlineProductIds" : { "$ne" : null } }, "user" : "" }
    //query.query:
    //{ "query" : { "query" : { "shopId" : 275417 }, "orderby" : { "_id" : NumberLong(1) } }, "user" : "pl-updMetaData" }
    //query.$query:
    //{ "query" : { "$query" : { "shopId" : 275418 }, "$comment" : "profiling comment" }, "user" : "profiling" }
    //{ "query" : { "$query" : { "shopId" : 283036, "smallPicture" : { "$ne" : null } }, "$orderby" : { "lastShopDataChange" : -1 } }, "user" : "" }
    // command.query:
    //{ "command" : { "count": "offer", "query" : { "shopId" : NumberLong(275417)} } }
    //since v3
    // query.filter:
    //{"query" : {"find" : "offer", "filter" : {"shopId" : 4640}, "sort" : {"_id" : 1}, "projection" : {"_id" : 1}}
    // query.filter.$operator:
    // "query" : { "find" : "offerlistSummary", "filter" : { "$and" : [ { "_id.siteId" : 4 }, { "_id.features" : [ ] }, { "$or" : [ { "_id.productId" : NumberLong(3795357) }, { "parentProductId" : NumberLong(3795357) } ] } ] }

    /*
    Queries:

    find({ "shopId" : 4640})
    "query" : {
		"find" : "offer",
		"filter" : {
			"shopId" : 4640
		}
	},
	wenn naechster Batch geholt wird:
	"query" : {
		"getMore" : NumberLong("14163338940"),
		"collection" : "offer"
	},
	find({ "shopId" : 4640},{_id:1})
	"query" : {
		"find" : "offer",
		"filter" : {
			"shopId" : 4640
		},
		"projection" : {
			"_id" : 1
		}
	},
    find({ "shopId" : 4640}).count()  = count({ "shopId" : 4640})
    "command" : {
		"count" : "offer",
		"query" : {
			"shopId" : 4640
		},
		"fields" : {

		}
	},
    find({ "shopId" : 4640}).sort({_id:1})
    "query" : {
		"find" : "offer",
		"filter" : {
			"shopId" : 4640
		},
		"sort" : {
			"_id" : 1
		}
	},
    find({ "shopId" : 4640},{_id:1}).sort({_id:1})
    "query" : {
		"find" : "offer",
		"filter" : {
			"shopId" : 4640
		},
		"sort" : {
			"_id" : 1
		},
		"projection" : {
			"_id" : 1
		}
	},
    find({ "shopId" : 4640, "onlineProductIds" : { "$ne" : null } })
    "query" : {
		"find" : "offer",
		"filter" : {
			"shopId" : 4640,
			"onlineProductIds" : {
				"$ne" : null
			}
		}
	},
    find({ "shopId" : 4640, "onlineProductIds" : { "$ne" : null } }).count() =  count({ "shopId" : 4640, "onlineProductIds" : { "$ne" : null } })
    "command" : {
		"count" : "offer",
		"query" : {
			"shopId" : 4640,
			"onlineProductIds" : {
				"$ne" : null
			}
		},
		"fields" : {

		}
	},
    find({ "shopId" : 4640, "onlineProductIds" : { "$ne" : null } }).sort({_id:1})
    "query" : {
		"find" : "offer",
		"filter" : {
			"shopId" : 4640,
			"onlineProductIds" : {
				"$ne" : null
			}
		},
		"sort" : {
			"_id" : 1
		}
	},
  //query.filter.$operator:
  "query" :
  { "find" : "offerlistSummary",
    "filter" :
    { "$and" :
      [
        { "_id.siteId" : 4 },
        { "_id.features" : [ ] },
        { "$or" :
          [
            { "_id.productId" : NumberLong(3795357) },
            { "parentProductId" : NumberLong(3795357) }
           ]
         }
       ]
    },

Unterschiedlichche Formate fuer getmore trotz gleicher Version 3.2.10:
Auf unterscheidlichen Maschinen:
offerStoreUK3:PRIMARY> db.system.profile.find({op:"getmore", "query.getMore":{$exists:true}}).pretty()
	{
	"op" : "getmore",
	"ns" : "offerStore.offer",
	"query" : {
		"getMore" : NumberLong("60593425309"),
		"collection" : "offer"
	},
	"cursorid" : 60593425309,
offerStoreUKshard2:SECONDARY> db.system.profile.find({ns:"offerStore.offer",op:"getmore"},{query:1}).sort({ts:1})
	{ "query" : { "shopId" : 22818 } }
    { "query" : { "exportFP.IDEALO" : { "$exists" : 1 }, "shopId" : 22818 } }
    { "query" : { "missingSince" : null, "categoryBokey" : "299099:7+C9LCEuzuxZjEKhW4zQfw" } }
Aber auch auf derselben Maschine unterschiedliche Formate:
offerStoreUK4:PRIMARY> db.system.profile.find({op:"getmore", "query.getMore":{$exists:true}}).limit(1).pretty()
{
	"op" : "getmore",
	"ns" : "offerStore.offer",
	"query" : {
		"getMore" : NumberLong("45779229895"),
		"collection" : "offer",
		"batchSize" : NumberLong(0)
	},
	"cursorid" : 45779229895,
	"keysExamined" : 665413,
	"docsExamined" : 665413,
	"keyUpdates" : 0,
	"writeConflicts" : 0,
	"numYield" : 5198,
	"locks" : {
		"Global" : {
			"acquireCount" : {
				"r" : NumberLong(10398)
			}
		},
		"Database" : {
			"acquireCount" : {
				"r" : NumberLong(5199)
			}
		},
		"Collection" : {
			"acquireCount" : {
				"r" : NumberLong(5199)
			}
		}
	},
	"nreturned" : 53773,
	"responseLength" : 4194314,
	"millis" : 8801,
	"execStats" : {

	},
	"ts" : ISODate("2016-10-20T08:58:02.001Z"),
	"client" : "172.16.70.7",
	"allUsers" : [
		{
			"user" : "__system",
			"db" : "local"
		}
	],
	"user" : "__system@local"
}
offerStoreUK4:PRIMARY> db.system.profile.find({ns:"offerStore.offer",op:"getmore","query.getMore":{$exists:false}}).limit(1).pretty()
{
	"op" : "getmore",
	"ns" : "offerStore.offer",
	"query" : {
		"merchantId" : {
			"$ne" : "1"
		},
		"importId" : "-1",
		"shopId" : 22818,
		"missingSince" : null
	},
	"cursorid" : 40592104010,
	"ntoreturn" : 0,
	"keyUpdates" : 0,
	"writeConflicts" : 0,
	"numYield" : 43850,
	"locks" : {
		"Global" : {
			"acquireCount" : {
				"r" : NumberLong(87702)
			},
			"acquireWaitCount" : {
				"r" : NumberLong(23101)
			},
			"timeAcquiringMicros" : {
				"r" : NumberLong(35965228)
			}
		},
		"Database" : {
			"acquireCount" : {
				"r" : NumberLong(43851)
			}
		},
		"Collection" : {
			"acquireCount" : {
				"r" : NumberLong(43851)
			}
		}
	},
	"nreturned" : 9899,
	"responseLength" : 4194625,
	"millis" : 161371,
	"execStats" : {

	},
	"ts" : ISODate("2016-10-18T08:50:56.709Z"),
	"client" : "172.16.70.6",
	"allUsers" : [
		{
			"user" : "__system",
			"db" : "local"
		}
	],
	"user" : "__system@local"
}
offerStoreUK4:PRIMARY>

Die per cursorid referenzierte Query sieht z.Bsp. so aus:
{
	"op" : "query",
	"ns" : "offerStore.offer",
	"query" : {
		"find" : "offer",
		"filter" : {
			"exportFP.IDEALO" : {
				"$exists" : 1
			},
			"shopId" : 290742
		},
		"projection" : {
			"exportFP.IDEALO" : 1,
			"missingSince" : 1,
			"_id" : 1
		},
		"skip" : 0,
		"singleBatch" : false,
		"maxTimeMS" : 0,
		"tailable" : false,
		"noCursorTimeout" : true,
		"awaitData" : false,
		"allowPartialResults" : false
	},
	"cursorid" : 63485548492,
	"keysExamined" : 261757,
	"docsExamined" : 261757,
	"keyUpdates" : 0,
	"writeConflicts" : 0,
	"numYield" : 2044,
	"locks" : {
		"Global" : {
			"acquireCount" : {
				"r" : NumberLong(4090)
			}
		},
		"Database" : {
			"acquireCount" : {
				"r" : NumberLong(2045)
			}
		},
		"Collection" : {
			"acquireCount" : {
				"r" : NumberLong(2045)
			}
		}
	},
	"nreturned" : 101,
	"responseLength" : 8395,
	"protocol" : "op_query",
	"millis" : 5335,
	"execStats" : {
		"stage" : "PROJECTION",
		"nReturned" : 101,
		"executionTimeMillisEstimate" : 4553,
		"works" : 261757,
		"advanced" : 101,
		"needTime" : 261656,
		"needYield" : 0,
		"saveState" : 2045,
		"restoreState" : 2044,
		"isEOF" : 0,
		"invalidates" : 0,
		"transformBy" : {
			"exportFP.IDEALO" : 1,
			"missingSince" : 1,
			"_id" : 1
		},
		"inputStage" : {
			"stage" : "FETCH",
			"filter" : {
				"exportFP.IDEALO" : {
					"$exists" : true
				}
			},
			"nReturned" : 101,
			"executionTimeMillisEstimate" : 4533,
			"works" : 261757,
			"advanced" : 101,
			"needTime" : 261656,
			"needYield" : 0,
			"saveState" : 2045,
			"restoreState" : 2044,
			"isEOF" : 0,
			"invalidates" : 0,
			"docsExamined" : 261757,
			"alreadyHasObj" : 0,
			"inputStage" : {
				"stage" : "IXSCAN",
				"nReturned" : 261757,
				"executionTimeMillisEstimate" : 390,
				"works" : 261757,
				"advanced" : 261757,
				"needTime" : 0,
				"needYield" : 0,
				"saveState" : 2045,
				"restoreState" : 2044,
				"isEOF" : 0,
				"invalidates" : 0,
				"keyPattern" : {
					"shopId" : 1,
					"missingSince" : 1,
					"merchantId" : 1
				},
				"indexName" : "shopId_1_missingSince_1_merchantId_1",
				"isMultiKey" : false,
				"isUnique" : false,
				"isSparse" : false,
				"isPartial" : false,
				"indexVersion" : 1,
				"direction" : "forward",
				"indexBounds" : {
					"shopId" : [
						"[290742, 290742]"
					],
					"missingSince" : [
						"[MinKey, MaxKey]"
					],
					"merchantId" : [
						"[MinKey, MaxKey]"
					]
				},
				"keysExamined" : 261757,
				"dupsTested" : 0,
				"dupsDropped" : 0,
				"seenInvalidated" : 0
			}
		}
	},
	"ts" : ISODate("2016-10-21T09:43:21.544Z"),
	"client" : "172.16.67.97",
	"allUsers" : [
		{
			"user" : "pl-compareFP",
			"db" : "offerStore"
		}
	],
	"user" : "pl-compareFP@offerStore"
}
Und das zugehoerige getmore sieht so aus:
offerstoreES:PRIMARY> db.getSiblingDB('offerStore').getCollection('system.profile').find({ns:'offerStore.offer',op:'getmore','query.getMore':63485548492}).limit(1).pretty()

{
	"op" : "getmore",
	"ns" : "offerStore.offer",
	"query" : {
		"getMore" : NumberLong("63485548492"),
		"collection" : "offer",
		"batchSize" : 101
	},
	"cursorid" : 63485548492,
	"keysExamined" : 334061,
	"docsExamined" : 334061,
	"keyUpdates" : 0,
	"writeConflicts" : 0,
	"numYield" : 2609,
	"locks" : {
		"Global" : {
			"acquireCount" : {
				"r" : NumberLong(5220)
			}
		},
		"Database" : {
			"acquireCount" : {
				"r" : NumberLong(2610)
			}
		},
		"Collection" : {
			"acquireCount" : {
				"r" : NumberLong(2610)
			}
		}
	},
	"nreturned" : 101,
	"responseLength" : 8376,
	"protocol" : "op_query",
	"millis" : 3131,
	"execStats" : {

	},
	"ts" : ISODate("2016-10-21T09:43:24.679Z"),
	"client" : "172.16.67.97",
	"allUsers" : [
		{
			"user" : "pl-compareFP",
			"db" : "offerStore"
		}
	],
	"user" : "pl-compareFP@offerStore"
}


db.find.distinct("classifierCatalogCategory") sieht so in system.profile aus:
{
	"op" : "command",
	"ns" : "offerStore.offer",
	"command" : {
		"distinct" : "offer",
		"key" : "classifierCatalogCategory",
		"query" : {

		}
	},
	"keyUpdates" : 0,
	"writeConflicts" : 0,
	"numYield" : 2160,
	"locks" : {
		"Global" : {
			"acquireCount" : {
				"r" : NumberLong(4322)
			}
		},
		"Database" : {
			"acquireCount" : {
				"r" : NumberLong(2161)
			}
		},
		"Collection" : {
			"acquireCount" : {
				"r" : NumberLong(2161)
			}
		}
	},
	"responseLength" : 22083,
	"protocol" : "op_command",
	"millis" : 1124,
	"execStats" : {

	},
	"ts" : ISODate("2016-10-27T09:32:55.434Z"),
	"client" : "127.0.0.1",
	"allUsers" : [ ],
	"user" : ""
}

am 2020-03-18:
mongos> db.slowops.distinct("op")
[
	"command",
	"command.$truncated",
	"command.aggregate",
	"command.collStats",
	"command.collstats",
	"command.count",
	"command.dbStats",
	"command.dbstats",
	"command.deleteIndexes",
	"command.distinct",
	"command.dropIndexes",
	"command.explain",
	"command.findAndModify",
	"command.findandmodify",
	"command.killCursors",
	"command.listIndexes",
	"command.query",
	"getmore",
	"getmore.getMore",
	"insert",
	"insert.insert",
	"killcursors",
	"killcursors.",
	"query",
	"query.$truncated",
	"query.find",
	"remove",
	"remove.q",
	"update",
	"update.q"
]

Changed in version 3.6.

A document containing the full command object associated with this operation. If the command document exceeds 50 kilobytes, the document has the following form:

"command" : {
  "$truncated": <string>,
  "comment": <string>
}

Bsp:
offerlistservice01:PRIMARY> db.system.profile.findOne({op:"update"})
{
	"op" : "update",
	"ns" : "offerlistservicefrontend.apiOfferlist",
	"command" : {
		"$truncated" : "{ q: { _id: { productId: 4850344, siteId: 2, segments: [ \"NOT_USED\" ] } }, u: { _id: { productId: 4850344, siteId: 2, segments: [ \"NOT_USED\" ] }, offerListId:...

update ohne $truncate:
offerlistservice01:PRIMARY> db.system.profile.findOne({op:"update","command.$truncated":{$exists:false}})
{
	"op" : "update",
	"ns" : "offerlistservicefrontend.rawPriceHistory",
	"command" : {
		"q" : {
			"_id" : {
				"productId" : NumberLong(6963948),
				"siteId" : 11,
				"historyDate" : ISODate("2020-03-18T00:00:00Z")
			}
		},
		"u" : {
			"_id" : {
				"productId" : NumberLong(6963948),
				"siteId" : 11,
				"historyDate" : ISODate("2020-03-18T00:00:00Z")
			},
			"priceHistoryEntries" : [


*/


    public ProfilingEntry filterDoc(Document doc) {

        boolean isCommand = false;
        Object tmp = doc.get("query");
        if(tmp == null) {
            tmp = doc.get("command");
            isCommand = true;
        }
        Object queryOrCommand = tmp;
        LinkedHashSet<String> fields = null;
        LinkedHashSet<String> sort = null;
        LinkedHashSet<String> proj = null;
        String op = "" + doc.get("op");
        if(queryOrCommand instanceof Document) {
            Document queryObj = (Document)queryOrCommand;
            removeUnnecessaryFields(queryObj);

            if(isCommand && "command".equals(op)) op += "." + getFirstKey(queryObj);//if op is "command" specify it
            Object innerQuery = null;
            if("getmore".equals(op)){
                Object getmoreObj = doc.get("originatingCommand"); //for "getmore" use "originatingCommand" to get the fields
                if(getmoreObj instanceof Document) {
                    queryObj = (Document)getmoreObj;
                    removeUnnecessaryFields(queryObj);
                    queryObj.remove("noCursorTimeout");
                    queryObj.remove("batchSize");
                    queryObj.remove("$readPreference");
                    //the following fields contain just the collection name, so it's redundant and can be removed and, if present, we specify the getmore op by a suffix
                    if(queryObj.remove("find") != null) op += ".find";
                    if(queryObj.remove("aggregate") != null) op += ".aggregate";
                }
            }else{
                innerQuery = queryObj.get("query");//test if "query.query" or "command.query"
            }
            if(innerQuery instanceof Document) {//format is "query.query" or "command.query"
                fields = getFields(innerQuery);
                Object orderbyObj = queryObj.get("orderby");
                if(orderbyObj != null) {
                    sort = getFields(orderbyObj);
                }
                Object projObj = queryObj.get("projection");
                if(projObj != null) {
                    proj = getFields(projObj);
                }
                if(fields.isEmpty() && queryObj.get("key") != null){//command.query is empty but command.key is not, i.e. db.find.distinct("someField");
                    fields.add(queryObj.get("key").toString());
                }
            }else {//format is "query.$query" or "command.$query" or "query.filter" or "query" or "command" or "getmore"
                Object innerDollarQuery = queryObj.get("$query");
                if(innerDollarQuery != null) {
                    fields = getFields(innerDollarQuery);
                    Object orderbyObj = queryObj.get("$orderby");
                    if(orderbyObj != null) {
                        sort = getFields(orderbyObj);
                    }
                    Object projObj = queryObj.get("$projection");
                    if(projObj != null) {
                        proj = getFields(projObj);
                    }
                }else {//format is "query.filter" or "query" or "command"
                    Object filterQuery = queryObj.get("filter");
                    if(filterQuery != null) { //format is query.filter
                        fields = getFields(filterQuery);
                        Object sortObj = queryObj.get("sort");
                        if (sortObj != null) {
                            sort = getFields(sortObj);
                        }
                        Object projObj = queryObj.get("projection");
                        if(projObj != null) {
                            proj = getFields(projObj);
                        }
                    }else {//format is "query" or "command"

                        Object pipeline = queryObj.get("pipeline");//test if it's an aggregation command
                        if(pipeline instanceof List) {
                            List<Document> pipelineList = (List<Document>)pipeline;
                            fields = new LinkedHashSet<String>();
                            for(Document pipelineObj : pipelineList){
                                LinkedHashSet<String> pFieldsSet = getFields(pipelineObj);
                                fields.addAll(pFieldsSet);
                            }
                        }else{

                            if("update".equals(op)){//for update operations, remove the updated document because it may be quite huge and does it does not matter for the analysis
                                queryObj.remove("u");
                            }
                            if(!"insert".equals(op)){//don't get fields for insert because there are no queried fields for inserts
                                fields = getFields(queryObj);
                                sort = null;
                                proj = null;
                            }

                        }

                    }
                }
            }
        }
        String ns = "" + doc.get("ns");
        String db = null;
        String col = null;
        String[] parts = ns.split("\\.");
        if(ns.length() > 1){
            db = parts[0];
            col = ns.substring(ns.indexOf(".")+1);
        }

        return new ProfilingEntry((Date) doc.get("ts"), serverAddress, db, col,
                op, "" + doc.get("user"), fields, sort, proj,
                getField(Integer.class, doc, "nreturned"),
                getField(Integer.class, doc, "responseLength"),
                getField(Integer.class, doc, "millis"),
                getField(Long.class, doc, "cursorId"),
                getField(Integer.class, doc, "keysExamined"),
                getField(Integer.class, doc, "docsExamined"),
                getField(Boolean.class, doc, "hasSortStage"),
                getField(Integer.class, doc, "ndeleted"),
                getField(Integer.class, doc, "ninserted"),
                getField(Integer.class, doc, "nModified")

        );
    }

    /**
     * remove unnecessary fields which contain many more sub documents which would blow up the ProfilingEntry without being useful for the analysis
     * @param doc
     */
    private void removeUnnecessaryFields(Document doc){
        doc.remove("$db");
        doc.remove("$clusterTime");
        doc.remove("$client");
        doc.remove("$configServerState");
        doc.remove("shardVersion");
        doc.remove("snapshot");

    }

    private String getFirstKey(Document doc){
        for(String key:doc.keySet()){
            return key;
        }
        return "";
    }

    private <T extends Object> T getField(Class<T> type, Document dbObj, String name) {
        if(dbObj != null) {
            final Object obj = dbObj.get(name);
            if(obj != null) {
                return (type.cast(obj));
            }
        }
        return null;
    }

    private LinkedHashSet<String> getFields(Object obj) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        if(obj instanceof Document) {
            Document dbObj = (Document)obj;
            //sorting keys on the same level is needed to group them up
            //else e.g. [a,b] and [b,a] would be 2 different groups
            //but both should be grouped to the same slow-op group
            List<String> sortedList = new ArrayList<>(dbObj.keySet());
            Collections.sort(sortedList);
            for(String key : sortedList){
                Object subObj = dbObj.get(key);
                if(subObj != null) {
                    if(subObj instanceof Document) {
                        key = handleSubDoc((Document)subObj, key, ".");

                    }else if(subObj instanceof Collection){
                        final ArrayList<String> sKeys = Lists.newArrayList();
                        for(Object sDoc : (Collection<Object>)subObj){
                            if(sDoc != null && sDoc instanceof Document) {
                                sKeys.add(handleSubDoc((Document)sDoc, "", ""));
                            }
                        }
                        //sorting is required because querying the fields [a, b] is the same as [b, a], thus has to be grouped into the same slow-op type
                        Collections.sort( sKeys );
                        if(!sKeys.isEmpty()){
                            final String keyBak = key;
                            key = key + "[";
                            String prevSubKey = null;
                            boolean isIdentical = true;
                            for(String k : sKeys){
                                key += k + ", ";
                                if(isIdentical && prevSubKey!=null && !prevSubKey.equals(k)) isIdentical=false;
                                prevSubKey = k;
                            }
                            if(isIdentical && sKeys.size()>1){
                                key = keyBak + "[" + prevSubKey + "*]"; //multiple occurrences of identical keys are shortened to only 1 key suffixed with *
                            }else {
                                key = key.substring(0, key.length() - 2); //cut last ,
                                key += "]";
                            }
                        }
                    }
                }
                result.add(key);
            }
        }
        return result;
    }

    private String handleSubDoc(Document subObj, String key, String separator){
        if(subObj.keySet().size() > 1){//multiple keys in subObj, so reflect the document with {}
            key += "{";
            List<String> sortedList = new ArrayList<>(getFields(subObj));//sorting is required because querying the fields {a, b} is the same as {b, a}, thus has to be grouped into the same slow-op type
            Collections.sort(sortedList);
            for(String sKey : sortedList){
                key += sKey + ", ";
            }
                key = key.substring(0, key.length() - 2); //cut last ,
                key += "}";
        }else{ //only one key so we can separate with separator (e.g. dot notation)
            LinkedHashSet<String> subDocKeys = getFields(subObj);
            for(String sKey : subDocKeys){
                key += separator + sKey;
            }
        }
        return key;
    }

}
