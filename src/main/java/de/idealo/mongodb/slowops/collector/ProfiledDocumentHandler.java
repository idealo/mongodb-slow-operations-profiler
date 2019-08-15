package de.idealo.mongodb.slowops.collector;

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

*/


    public ProfilingEntry filterDoc(Document doc) {

        boolean isCommand = false;
        Object tmp = doc.get("query");
        if(tmp == null) {
            tmp = doc.get("command");
            isCommand = true;
        }
        Object queryOrCommand = tmp;
        Set<String> fields = null;
        Set<String> sort = null;
        String command = "" + doc.get("op");
        if(queryOrCommand != null  && queryOrCommand instanceof Document) {
            Document queryObj = (Document)queryOrCommand;
            if(isCommand) command += "." + getFirstKey(queryObj);
            Object innerQuery = queryObj.get("query");//test if "query.query" or "command.query"
            if(innerQuery != null && innerQuery instanceof Document) {//format is "query.query" or "command.query"
                fields = getFields(innerQuery);
                Object orderbyObj = queryObj.get("orderby");
                if(orderbyObj != null) {
                    sort = getFields(orderbyObj);
                }
                if(fields.isEmpty() && queryObj.get("key") != null){//command.query is empty but command.key is not, i.e. db.find.distinct("someField");
                    fields.add(queryObj.get("key").toString());
                }
            }else {//format is "query.$query" or "command.$query" or "query.filter" or "query" or "command"
                Object innerDollarQuery = queryObj.get("$query");
                if(innerDollarQuery != null) {
                    fields = getFields(innerDollarQuery);
                    Object orderbyObj = queryObj.get("$orderby");
                    if(orderbyObj != null) {
                        sort = getFields(orderbyObj);
                    }
                }else {//format is "query.filter" or "query" or "command"
                    Object filterQuery = queryObj.get("filter");
                    if(filterQuery != null) { //format is query.filter
                        fields = getFields(filterQuery);
                        Object sortObj = queryObj.get("sort");
                        if (sortObj != null) {
                            sort = getFields(sortObj);
                        }
                    }else {//format is "query" or "command"

                        Object pipeline = queryObj.get("pipeline");//test if it's an aggregation command
                        if(pipeline != null && pipeline instanceof List) {
                            List<Document> pipelineList = (List<Document>)pipeline;
                            fields = new HashSet<String>();
                            for(Document pipelineObj : pipelineList){
                                Set<String> pFieldsSet = getFields(pipelineObj);
                                fields.addAll(pFieldsSet);
                            }
                        }else{
                            fields = getFields(queryOrCommand);
                            sort = null;
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

        return new ProfilingEntry((Date)doc.get("ts"), serverAddress, db, col,
                command, "" + doc.get("user"), fields, sort, getInteger(doc, "nreturned"),
                getInteger(doc, "responseLength"), getInteger(doc, "millis"), getLong(doc, "cursorId"));
    }

    private String getFirstKey(Document doc){
        for(String key:doc.keySet()){
            return key;
        }
        return "";
    }

    private Integer getInteger(Document dbObj, String name) {
        if(dbObj != null) {
            final Object obj = dbObj.get(name);
            if(obj != null) {
                return (Integer)(obj);
            }
        }
        return null;
    }

    private Long getLong(Document dbObj, String name) {
        if(dbObj != null) {
            Object obj = dbObj.get(name);
            if(obj != null) {
                return (Long)(obj);
            }
        }
        return null;
    }

    private Set<String> getFields(Object obj) {
        HashSet<String> result = new HashSet<String>();
        if(obj != null && obj instanceof Document) {
            Document dbObj = (Document)obj;
            for(String key : dbObj.keySet()){
                Object subObj = dbObj.get(key);
                if(subObj != null) {
                    if(subObj instanceof Document) {
                        for(String sKey : ((Document) subObj).keySet()){
                            key += "." + sKey;
                        }
                    }else if(subObj instanceof Collection){
                        String collKey="";
                        for(Document sDoc : (Collection<Document>)subObj){

                            Set<String> subDoc = getFields(sDoc);
                            for(String sKey : subDoc){
                                collKey += sKey + "|";
                            }

                        }
                        if(!collKey.isEmpty()) collKey=collKey.substring(0, collKey.length()-1); //cut last |
                        key = collKey + "." + key;
                    }
                }
                result.add(key);
            }
        }
        return result;
    }

}
