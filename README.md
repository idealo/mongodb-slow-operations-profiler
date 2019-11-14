# MongoDB slow operation profiler and visualizer

This java web application collects and stores slow operations from one or more mongoDB system(s) in order to visualize and analyze them.
Since v2.0.0 it may be easily extended to an administration tool by implementing commands to be executed against the configured database system(s).
The initial version of the software has been presented during the [MongoDB User Group Berlin on 4th of June 2013](http://www.meetup.com/MUGBerlin/events/119503502/).
Slides of the presentation can be found [here](http://www.slideshare.net/Kay1A/slow-ops). Version 2.4 has been presented in November 2018 at the [Percona Live Europe Conference](https://www.percona.com/live/e18/sessions/how-to-visually-spot-and-analyze-slow-mongodb-operations).

The following first screenshot demonstrates how slow operations are visualized in the diagram: The higher a point or circle on the y-axis, the slower was the execution time of this operation. The greater the diameter of the circle, the more slow operations of this type were executed at this time.

You can zoom-in by drawing a rectangle with the mouse around the area you are interested in. I suggest to zoom-in **first** horizontally, and then zoom-in vertically. Press Shift + drag mouse in order to move the visible area around. Double click returns to the initial viewport.

While the mouse hovers over the diagram, the corresponding slow operations are shown in bold and details of them are displayed on the right-hand side legend. The different colors in the legend are just to better distinguish the different entries and have no further meaning.

The more "Group by" checkboxes are checked, the more details constitute slow operation types, thus the more details you'll see for each slow operation type.


### Example

For example, the following screenshot shows that only one database has been **filtered**, defined in the search form by its label, server addresses, replica set and database name for a specific time period.
Since the slow operations are defined in the search form to be **grouped by** their collections, operations, queried and sorted fields, the legend below on the right shows these 4 details grouped together for the time period which is being hovered over by the mouse, here at 00:03 o'clock. As the the **resolution** is set to `Minute`, all slow operations occurred during within the minute hovered over by the mouse, here from 00:03 until 00:04 o'clock, are shown on the right-hand side in the legend.

In this example, from 23:58 to 00:06 o'clock occurred 5 different slow operation types which are printed in different colors in both the diagram and the legend. As the legend is sorted by y-axis, thus execution time in milliseconds, the **slowest** operation type is shown first. Instead, you may select "sort legend by:" `count-value` to see in the legend the **most profiled** slow operation types first (which correspond to the biggest circles).

In this screenshot, the slowest operation type at 00:03 o'clock happened on collection `apiOfferlist` by executing a `query` on both fields `_id.productId` and `_id.siteId`. This slow operation type occurred 112 times at this precise minute, its minimum duration was 2 ms, maximum 47 ms, average 10 ms and the sum of all these queries was 1.115 ms.

Below you see how many documents (min, max, avg, sum, stdDev) were returned by this operation. And last but not least you have some metrics about how many index keys were read (`rKeys`), how many documents were read (`rDocs`) and written (`wDocs`) and also that no in-memory sort (`memSort`) had to be done (no sort at all in this case since no `sort` field was defined). 

The second slowest operation type in this screenshot is a `getmore` operation. Since v2.9.0 the application shows also the originating query of the `getmore`operation. In this example, the query logically combined both fields `_id.productId`and `parentProductId` by OR and its result was logically combined by AND with the field `_id.siteId`. 

In general, multiple fields belonging to an operator expression are enclosed in square brackets `[]` and the last field is suffixed by `.$operator` e.g. `[a, b.$or]`. If the operator applies to only one field, square brackets are omitted, e.g. `a.$gt`.

The metrics about execution times, returned documents, read and written documents and/or index keys are to read in the same manner as above.



## Example screenshot of the analysis page


![Screenshot](img/slow_operations_gui_diagram_low.png "Screenshot of the analysis page")

### Summarized table

Since v1.0.3 the user analysis page, besides the diagram, has also a table of all selected slow operation types. The table is filterable. Columns are sortable and selectable to be hidden or shown. The sum over the values of columns, where it makes sense, is shown at the bottom of the columns.

For example, to see the most expensive slow operations first, just sort descending by column `Sum ms`.

Here is a reduced screenshot of the table without any filter on rows because the `Search` textbox is empty. However not all columns are shown. You can toggle a column to be shown or hidden by clicking its name in the table header. The table here is sorted by column `Sum ms`, so we see in the first row the most **expensive** slow operation type: either there were many of this type or they were generally slow. 

Let's interprete the first row to get you familar with. In the column `Group` you see the attributes you've grouped by (selected in the search form above). Here again, the slow ops occurred in the collection (`col`) "apiOfferlist". The slow operations (`op`) were "updates" and the field (`fields`) to select the documents to be modified was an `_id` field having a sub-document querying the 3 fields `productId`, `siteId` and `segments`. Sub-documents being queried on more than one field are enclosed by curly brackets `{}`. However, if only one sub-document field was queried, dot-notation is preferably used.

The rest of the columns in the data table should be self-explanatory. High values in the column `ms/ret` (and vice versa low values in column `ret/ms`) may indicate a performance problem for operations which return documents, because these columns show the time in ms needed to return 1 document respectively the number of documents returned within 1 ms.

![Screenshot](img/slow_operations_gui_table_low.png "Screenshot of the table")

<a name="sumtable_v2.9.0"></a>
Since version 2.9.0 the analysis page shows additional metrics about:
* number of read keys from the index (`rKeys`)
* number of read documents (`rDocs`)
* number of written documents (`wDocs`)
* if an in-memory sort happend (`memSort`)
* standard deviation of the execution times (`stdDev ms`)
* standard deviation of the number of returned documents (`stdDev ret`) 

If `rKeys` is 0 then no index has been used, resulting in a collection scan. In this case you should consider adding an index. If `rKeys`is much higher than the number of returned documents (column `Sum ret`), the database is scanning many index keys to find the result documents. Consider creating or adjusting indexes to improve query performance.

If `rDocs` is 0 then the query was "covered" which means that all information needed could be retrieved from the index, thus no document had to be fetched into memory. For write operations, if `rDocs`is higher than `wDocs`then some documents didn't need to be (re)written because they were already up-to-date.

The value of `wDocs` is the sum of how many documents have been deleted, inserted or modified by the corresponding slow operation type. This being said, you may tick **Operation**  in your **Group by** settings to know the number of written docs for the corresponding write-operation type (i.e. `remove`, `insert`, `update`).

The value of `memSort`is `true` if no index could be used to sort the documents. In this case you should consider adding or adjusting indexes so that no in-memory sort is needed anymore.



## Applicaton status page

Since v1.0.3 there is also a page to show the application status. Besides showing the status of the collector, means where and how many slow operations have been collected (read and written) since application restart, it shows also every registered database in a table. Since profiling works per database, each database to be profiled is in one row.

The table is filterable. Columns are sortable and selectable to be hidden or shown. The sum over the values of columns, where it makes sense, is shown at the bottom of the columns. The table is by default sorted by the columns `Label`, `ReplSet` and `Status` which gives a very good overview over a whole bunch of clusters. **Hint:** Hold shift key pressed while clicking the column headers in order to sort multiple columns.

Here is a reduced screenshot of some first rows of the table, ordered by columns `ReplSet` and `Status`, with a filter "datastore" applied on rows:

![Screenshot](img/slow_operations_app_status.png "Screenshot of the application status page")

At its right side, the table has a bunch of time slot columns (10 sec, 1 min, 10 min, 30 min, 1 hour, 12 hours, 1 day). These show the number of slow operations collected during these last time periods, so you can see already here which databases may behave abnormally. In such case, you may either analyse those databases or switching off collecting  or lower their `slowMs` threshold in order to profile less slow operations.

#### Actions

Since v2.0.2, a floating **Actions panel** is shown always on top and can be switched on or off. Both `refresh` and `analyse` actions were implemented already before v2.0.0. `refresh` gets and shows the latest data of the selected database(s). `analyse` opens the above mentionned analysis page to show the slow operation types of the last 24 hours of the selected node(s) respectively database(s). Both `collecting start/stop` and `set slowMs` were also already implemented before but since v2.0.0 they are only shown to authorized users. "Authorized users" are users who used the url parameter `adminToken` set to the right value (see below under "configuration" for more details).

Since v2.0.0. you may execute **commands** against the selected database system(s). Since v2.0.3 you can choose whether the command has to run against the corresponding database system (i.e. mongos-router) or against the individually selected nodes (i.e. mongod). The difference is that the command will run either against the entry point of the database system (i.e. router or primary) or against all selected nodes wich may be secondaries as well. Current implemented commands are:

+ list databases and their collections
+ show currently running operations (requires mongodb v3.2 or newer)
+ show index access statistics of all databases and their collections (requires mongodb v3.2 or newer)
+ show host info such as, mongodb version, operating system, kernel & libc version, CPU info (number of cores, MHz, architecture), amount of RAM, Numa enabled, page size, number of pages, max open files

The command result is shown in a new page in a filterable table. Columns are sortable as well, so you can detect immediately spikes. **Hint:** Hold shift key pressed while clicking the column headers in order to sort multiple columns.

Here is a cutout of a screenshot showing the result of the current-operation command. The table is sorted by column "secs running" in order to see slow operations first.

![Screenshot](img/slow_operations_command_result_page.png "Screenshot of the operation command result page")

Implementing new commands is quite easy: just create a new java class which implements the interface `de.idealo.mongodb.slowops.command.ICommand`. The interface has only 2 methods in order to execute the database command and to transform the result to a corresponding table structure.

This being said, from v2.0.0 on, the webapp may be extended from a pure monitoring and analyzing tool to an administration tool.

#### Dynamic configurations

Since v1.2.0, authorized users may dynamically upload new configurations in order to add, remove or change databases to be registered respectively to be profiled. The configuration of the collector writer may also be changed. "Authorized users" are users, who used the url parameter `adminToken` set to the right value (see [Configuration](#config) below for more details).
The uploaded config is **not** persisted server side and will be lost upon webapp restart. All servers of changed "profiled"-entries are (re)started. Also the collector needs to be restarted if its config changed. Even though stops and starts are executed simultaneously, it may take some time depending on how many changes need to be applied, thus how many readers, respectively the writer, are involved by the config change.


##   Setup

### Preconditions

Either:

1. java 1.8 or newer
2. maven 2.0 or newer
3. mongoDB 2.0 or newer

Or:

1. Docker

### Starting up

#### Starting up by using Docker

1. Download both files `Dockerfile` and `docker-compose.yaml` from
   github:
   - `https://raw.githubusercontent.com/idealo/mongodb-slow-operations-profiler/master/Dockerfile`
     e.g. by issuing the command `curl -O
     https://raw.githubusercontent.com/idealo/mongodb-slow-operations-profiler/master/Dockerfile`
   - `https://raw.githubusercontent.com/idealo/mongodb-slow-operations-profiler/master/docker-compose.yaml` e.g. by issuing the command `curl -O
     https://raw.githubusercontent.com/idealo/mongodb-slow-operations-profiler/master/docker-compose.yaml`
2.  Being in the folder of both downloaded files, spin up the docker
    containers by issuing the commad: `docker-compose up -d`
3. The application can be accessed through a web browser by the URL
   `http://localhost:8080/mongodb-slow
   -operations-profiler/app?adminToken=mySecureAdminToken` On the bottom
   of this page you can edit the current configuration and apply it by
   pressing the button `upload new config` 
4. To visualize and analyze slow operations either select one or more entries and click "analyse" or use the
 following URL `http://localhost:8080/mongodb-slow-operations-profiler/gui`
 
##### Docker does automatically the following:

* clone the project from github to your local computer in a temporary 
  docker container
* build the project by maven in a temporary docker container
* spin up one container named `collector-db` running one single mongod
  instance serving as collector database
* spin up one container named `test-db` running one single mongod
  instance serving as test database
* spin up one container named `profiler-webapp` running running a web 
  server (tomcat) to serve the webapp
* expose port 8080 to access the webapp

##### Some helpful Docker commands:

* access a running docker container: `docker exec -it <CONTAINER-NAME>
  /bin/bash` e.g. `docker exec -it test-db /bin/bash`
* stop a running docker container: `docker stop <CONTAINER-NAME>` e.g.
  `docker stop profiler-webapp`
* rebuild and start all stopped docker container belonging to this
  project: `docker-compose up -d --build`
* stop all stopped docker container belonging to this project:
  `docker-compose down`
  
Be aware that neither both mongod instances (`collector-db` and
`test-db`) nor the web server (`profiler-webapp`) are secured. This
means that mongodb can be accessed without authentication from within
their containers. Also SSL/TLS is not enabled.
  
#### Starting up by having already installed git, java, maven and mongodb

1. Clone the project:
`git clone https://github.com/idealo/mongodb-slow-operations-profiler.git`
2. Enter the server addresses, database and collection names in file "`mongodb-slow-operations-profiler/src/main/resources/config.json`" (see [Configuration](#config) below)
3. While being in the in the project folder "`mongodb-slow-operations-profiler/`", build a war file by executing in a shell:
`mvn package`
4. Deploy the resulted war file (e.g. "`mongodb-slow-operations-profiler-1.0.3.war`") on a java webserver (e.g. tomcat). Dependent on the above mentionned `config.json`, it may automatically start collecting slow operations. If no slow operations exist yet on the mongod's, the collector(s) will sleep 1 hour before retrying.
5. The application can be accessed through a web browser by the URL `http://your-server:your-port/mongodb-slow
-operations-profiler[your-version-number-if-less-than-2.10]/app`
6. To visualize and analyze slow operations either select one or more entries and click "analyse" or use the
 following URL `http://your-server:your-port/mongodb-slow-operations-profiler[your-version-number-if-less-than-2.10
 ]/gui`

### <a name="config"></a> Configuration

The application is configured by the file "`mongodb-slow-operations-profiler/src/main/resources/config.json`". It's a json formatted file and looks like this:

```json
{
  "collector":{
    "hosts":["myCollectorHost_member1:27017",
             "myCollectorHost_member2:27017",
             "myCollectorHost_member3:27017"],
    "db":"profiling",
    "collection":"slowops",
    "adminUser":"",
    "adminPw":"",
    "ssl":false
  },
  "profiled":[
    { "enabled":false,
      "label":"dbs foo",
      "hosts":["someHost1:27017",
               "someHost2:27017",
               "someHost3:27017"],
      "ns":["someDatabase.someCollection", "anotherDatabase.anotherCollection"],
      "adminUser":"",
      "adminPw":"",
      "ssl":false,
      "slowMS":250,
      "responseTimeoutInMs":2000
    },
    { "enabled": false,
      "label":"dbs bar",
      "hosts":["someMongoRouter:27017"],
      "ns":["someDatabase.someCollection", "anotherDatabase.*"],
      "adminUser":"",
      "adminPw":"",
      "ssl":false,
      "slowMS":250,
      "responseTimeoutInMs":2000
    }
  ],
  "yAxisScale":"milliseconds",
  "adminToken":"mySecureAdminToken",
  "defaultSlowMS":100,
  "defaultResponseTimeoutInMs":2000,
  "maxWeblogEntries":100
}
```
This example configuration defines first the `collector` running as a replica set consisting of 3 members on hosts "myCollectorHost_member[1|2|3]" on port 27017, using the collection "slowops" of database "profiling". Both `adminUser` and `adminPw` are empty because the mongodb instance runs without authentication. If mongod runs with authentication, the user must exist for the admin database with role "root".

After the definition of the collector follow the databases to be profiled. In this example, there are only two entries. However, keep in mind that the application will **resolve all members** of a replica set (even if only 1 member has been defined) respectively all shards and its replica set members of a whole mongodb cluster.

Fields of `profiled` entries explained:

* `enabled` = whether collecting has to be started automatically upon (re)start of the application
* `label` = a label of the database system in order to be able to filter, sort and group on it
* `hosts` = an array of members of the same replica set, or just a single host, or one or more mongo router of the same cluster
* `ns` = an array of the namespaces to be collected in the format of `databaseName.collectionName`. The placeholder `*` may be used instead of `databaseName` and/or `collectionName` to collect from all databases and/or all collections. Examples:
  * `databaseName.*` collects from all collections from database `databaseName`
  * `*.collectionName` collects from all databases from collection `collectionName`
  * `*.*` collects from all collections from all databases
* `adminUser`= if authentication is enabled, name of the user for database "admin" having role "root"
* `adminPw`= if authentication is enabled, passwort of the user
* `ssl`= if set to `true`, use ssl to connect to the server
* `slowMS`= threshold of slow operations in milliseconds
* `responseTimeoutInMs`= response timeout in ms


The field `yAxisScale` is to be set either to the value "milliseconds" or "seconds". It defines the scale of the y-axis in the diagram of the analysis page.

In v2.0.0 the field `adminToken` has been introduced to restrict access to administrative functionalities i.e. stop/start of collecting slow operations, setting the threshold `slowMs`, seeing the currently used configuration or uploading a new configuration.
To grant access to these functionalities, add the parameter `adminToken=` followed by your configured value, i.e. `mySecureAdminToken`, to the URL of the application status page, i.e. `http://your-server:your-port/mongodb-slow-operations-profiler[your-version-number-if-less-than-2.10]/app?adminToken=mySecureAdminToken`.

In v2.4.0 some new options have been introduced:
* `defaultResponseTimeoutInMs` defines a default response timeout for all `profiled` entries that don't have specified `responseTimeoutInMs` (default: 2000 ms)
* `defaultSlowMS` defines a default threshold of slow operations in milliseconds for all `profiled` entries that don't have specified `slowMS` (default: 100 ms)
* `maxWeblogEntries` defines the maximal number of log messages shown in the application status page (default: 100)


## Version history
* v2.10.0
   + new: `Dockerfile` and `docker-compose.yaml` added which allows to spin up both the webapp and the collector
    database in docker containers. Only Docker needs to be installed to run the application in this way. All the
     other dependencies (java, maven, mongodb) will be handled by Docker automatically and thus don't have to be set
      up by the
      user.  
   + new: change default `config.json` so that  
   + new: show status of single nodes as SINGLE
   + new: show log message in the application status page when the profiling writer was started or stopped 
   + update: version number is omitted in war file so that the URL to access the webapp stay the same when the
    version number changes
* v2.9.0
   + new: for `getmore` operations the profiler analyzes `originatingCommand` from the profiling entries, so `getmore` operations can now be related to the originating query (only for mongodb versions 3.6 and newer)
   + new: the profiler retrieves additional fields from the profiling entries such as `keysExamined`, `docsExamined`, `hasSortStage`, `ndeleted`, `ninserted` and `nModified` (don't blame me for the inconsitent camel case - it's mongodb.org's carelessness). The first three fields are available only for monogdb versions 3.2 and newer. 
   + update: in the analysis page, the legend of the diagram is reformatted for better readability
   + new: the analysis page shows additional metrics about:
     * number of read keys from the index (`rKeys`)
     * number of read documents (`rDocs`)
     * number written documents (`wDocs`)
     * if an in-memory sort happened (`memSort`)
     * standard deviation of the execution time (`StdDev ms`)
     * standard deviation of the number of returned documents (`StdDev ret`)

     For more details [see above](#sumtable_v2.9.0)
   + bugfix: on the analysis page in `Filter by`, searching for `Queried fields` or `Sorted fields` may have required special formatting in order to match these fields, mainly for embedded or complex fields, but now, queried and sorted fields can be copied as they are shown in the analysis page and pasted "as is" in the search form `Filter by` to filter by them    
* v2.8.0
   + new: slow operation query fields having sub documents are clearer noted i.e. a query like `field:{foo:{bar:3,baz:5}}` was formerly shown as `field.foo.bar.baz` but now it's `field.foo{bar,baz}`. The same goes for operators as for example `$elemMatch`: a query like `field:{$elemMatch:{foo:1, bar:2, baz:{$gt:3}}}` was formerly shown just as `field.$elemMatch` but now it's `field.$elemMatch{baz.$gt,foo,bar}`
   + update: the in v2.7.0 introduced recursively field detection is improved i.e. a query like `{$and:[{x:3},{$or:[{y:5},{z:7}]}]}` was shown in v2.7.0 as `x|y|z.$or.$and` but now it's `[x,[y,z.$or].$and]` in order to clearly point out which operator belongs to which field(s)
* v2.7.0
   + new: slow operation query fields are now recursively detected i.e. a criterion of a query like `{$and:[{x:3},{$or:[{y:5},{z:7}]}]}` was formerly shown just as `$and` (only 1 level deep) but now, also deeper nested levels are shown, e.g. `x|y|z.$or.$and`
   + new: the fingerprint of a query is better distinguished i.e. a query like `{field:{$or:[{x:3},{y:5}]}}` was formerly fingerprinted just as `$or` but now it's `field.$or` which is helpful to spot queries using operators which may cause performance issues
   + new: write slow operations into the collector database by using batches (BulkWrites), which increases the insert rate and consumes less resources
   + new: on the application status page show a log message if the system.profile collection could not be read fast enough because the insertion rate of slow operations was too high or the system.profile collection too small, so decrease the operations to be profiled (e.g. by lowering the slowMs threshold) and/or increase the size of system.profile collection
* v2.6.1
   + bugfix: removed debug code for the application status page which resulted in a MemSizeMB value incremented by 1
* v2.6.0
   + new: clicking "upload new config" in the application status page will resolve all members of all defined database systems. So use this functionality also if shards or replica set servers have been removed or added and your configured access points are unchanged.
   + update: only one instance of `MonogClient` per server:port and its configured settings (timeouts, ssl, user/pw etc.) is used and won't be closed after usage so connections can be pooled and re-used
   + update: tried to show status of arbiters running with authentication which is not possible due to a mongodb bug [SERVER-5479](https://jira.mongodb.org/browse/SERVER-5479) though. Therefore, connecting to arbiters is not possible hence the status `UNKNOWN` is still shown.
   + update: condensed monitoring log output
   + update: java mongodb driver updated from v3.8.0 to v3.10.2
* v2.5.2
   + bugfix: `current op` command did not work properly for some mongodb versions because the format of field `secs_running` could be either of type `Long` or `Integer`
   + bugfix: the data table of the application status page displayed the host info only for the first database entry for each host
* v2.5.1
   + new: data table of the application status page has added some important host info such as CPU frequency, number of cores, amount of RAM and mongodb version. This is helpful if you want to check at a glance many servers or even clusters if they differ in some important specifications or configurations.
   + new: command "host info" added to action panel in order to show even more info about the host
* v2.5.0
   + new option: a boolean `ssl` (default: `false`) for `collector` and `profiled` entries in order to connect using ssl
   + bugfix: `index access stats` command did not work anymore for newer versions of mongodb because some databases (admin, config, local) and collections (system.profile) are not allowed to be targeted
   + bugfix: `current op` command did not work for newer mongodb versions because the output format has changed. This is also the reason that the whole `command` is now shown instead of `query`. The latter seems to be obsolete for newer mongodb versions. `command` is much more verbose than `query` and may/should be itemized in the future to fit better the tabular structure. The `command` output is json-formatted as before done with `query`.
* v2.4.2
   + update: log at the bottom of the application status page when thread pool is going to be closed after max. response timeout although not all threads have terminated. This may be especially relevant if many mongoDB systems with many databases are to be profiled because for each of them one thread is getting server status updates (e.g. if the database profiler is running or stopped). However, if the webserver is limited in CPU cores, it can't handle all threads in parallel within the given max. response timeout. In such cases the user is now informed that not all threads could terminate, and hence the application status page might be incomplete. Adding more CPU cores, incrementing the max. response timeout (see options `responseTimeoutInMs` and `defaultResponseTimeoutInMs`) or profiling quicker responding or fewer mongoDB servers will alleviate or even avoid this issue.
* v2.4.1
   + bugfix: replica sets got not resolved (only sharded clusters and single nodes got resolved)
   + update: limit number of threads dependent on number of cores when using thread pools
   + update: default sorting of columns corresponds to the order of the columns from right to left: "label, replSet, status, host, database" instead of the former column sort order "label, replSet, host, status, database"
* v2.4.0
   + bugfix: the data table of the application status page could sometimes not be loaded du to race conditions and resultant deadlocks
   + new: user relevant log messages (see new option `maxWeblogEntries`) are shown at the bottom of the application status page, which is helpful for example to spot mongod hosts that could not sent a response within the configured `responseTimeoutInMs`
   + new: option `responseTimeoutInMs` which is helpful to fail fast (thus to show the application status page quick enough) when some defined mongo hosts are not responsive enough
   + new: option `defaultResponseTimeoutInMs` defines a default response timeout for all `profiled` entries that don't have specified `responseTimeoutInMs`
   + new: option `defaultSlowMS` has been introduced to define a default threshold of slow operations in milliseconds for all `profiled` entries that don't have specified `slowMS`
   + new: option `maxWeblogEntries` has been introduced to define the maximal number of log messages shown at the bottom of the application status page
   + update: the cache layer, introduced in v2.1.0 in order to load the application status page quicker, has been removed because it's more accurate to see actual than outdated cached values. Thanks to the new option `responseTimeoutInMs`, the application status page should be displayed within at most 2 times of the longest defined `responseTimeoutInMs` plus data transfer time.
   + update: line number added in log line
   + update: mongodb driver v3.8.0
* v2.3.0
   + new: namespace (`profiled.ns`) in config.json may use placeholder `*` for databse names (i.e. `*.myCollection`) in order to collect from myCollection from all databases
* v2.2.1
   + bugfix: the initial viewport of the diagram did not always cover the whole x-axis range
* v2.2.0
   + new: the diagram of the analysis page has now new options to redraw the y-axis either as avg, min, max or sum of the duration of the slow operation types, to easily spot spikes
   + update: localized formatting of numbers in the legend of the diagram and in the summarized table of the analysis page
   + update: option "exclude 14-days-operations" removed because newer versions of mongodb have fixed this
   + bugfix: labels of slow operations types in the legend of the diagram were mixed-up since last version v2.1.1
   + bugfix: the legend of the chronologically very first slow operation types was not shown next to the diagram
* v2.1.1
   + bugfix: the diagram displayed superfluously also the accumulation of all distinct slow operation types, resulting in big circles at the first occurrence of each distinct slow operation type, thus often shown at the very left on the x-axis
   + bugfix: after refreshing the application status page, the status of profiling, collecting and slowMs of nodes whose status changed within the cache time of 1 minute might have been wrongly reported because these values were not immediately updated in the cache
* v2.1.0
   + new: application status page loads much quicker if many mongod's or databases are registered because the status of mongod's and databases are now cached; a page reload will refresh the status of mongod's and databases in the background if it's older than 1 minute
* v2.0.3
   + new: option to run command against database system or against selected nodes
* v2.0.2
   + new: action panel is now floating on top so it's always visible which avoids scrolling down the whole page to use it
* v2.0.1
   + new: preset search filter on application status page with value of url parameter `lbl`
* v2.0.0
   + new: show tabular result of commands executed against the selected database system(s); implemented commands are:
     + show current running operations
     + list databases and their collections
     + show index access statistics of all databases and their collections (requires mongodb v3.2 or newer)
   + new: grant access to administrative functionalities only to authorized users
   + update: use mongodb-java-driver v3.4.2 instead of previously used v3.3.0
   + update: for consistent reads `ReadPreference.primaryPreferred()` is used instead of previously used `ReadPreference.secondaryPreferred()` - except for analysing collected slow operations which still uses `ReadPreference.secondaryPreferred()`
* v1.2.1
    + bugfix: removing profiling reader(s) when uploading a new config might have failed
    + update: both parameters fromDate and toDate are required on analyse page and will be set to default if not existent; other given parameters are applied independently
    + update: change index from {adr:1, db:1} to {adr:1, db:1, ts:-1} on profiling.slowops collection to accelerate start-up of profiling readers when they query for their newest written entry in order to continue from
    + update: change index from {ts:-1} to {ts:-1, lbl:1} on profiling.slowops collection to speed-up analyse queries having also "lbl" ("ts" is always provided)
* v1.2.0
    + new: show currently used configuration as json
    + new: upload new configuration as json (which is applied but not persisted server side); all servers of changed "profiled"-entries are (re)started; collector is restarted if its config changed
    + new: option to refresh only collector status
    + new: show number of reads and writes from current and removed/changed profilers and the collector which helps to see if all reads of the profilers got written by collector
    + update: socket timeout for analyze queries set from 10 to 60 seconds
* v1.1.2
    + bugfix: the sum on columns was not updated when rows were filtered on analysis page
    + new: column `ms/ret` in table on analysis page added which shows how much time was globally spent for one returned document
* v1.1.1
    + new: when using placeholder `*` to collect from all collections, exclude documents of namespace `db.system.profile` because reading from `db.system.profile` may be slower than defined by slowMS, resulting in a new entry in `db.system.profile` which is irrelevant for the analysis
* v1.1.0
    + new: namespace (`profiled.ns`) in config.json may use placeholder `*` for collection names (i.e. `mydb.*`) in order to collect from all collections of the given database
* v1.0.3
    + new: multiple databases and collections for different replica sets, clusters or single mongod's can be defined to be profiled
    + new: automatic resolving of all mongod's constituting the defined clusters and replica sets
    + new: overview of all resolved mongod's and their state i.e. primary or secondary, databases, being profiled or not, number of profiled slow operations per database in total and in the last time periods of 10 seconds, 1, 10, 30 minutes, 1, 12 and 24 hours
    + new: option to set `slowMs` threshold, to start or to stop profiling and collecting of slow operations for multiple selected databases with one click
    + new: just tick one or multiple databases to open the diagram showing slow operations graphically for the selected databases, mongod's, replica sets or whole clusters
    + new: besides the diagram showing slow operation graphically, a filterable and sortable table displays different metrics of these slow operations, so you can easily spot the most expensive operations within the chosen time period
    + new: option to show circles in the diagram as square root of count-value to reduce the diameter of the circles which is useful when there are too many slow operations of the same type resulting in circles which are too large to fit in the diagram
    + new: the fingerprint of a query is better distinguished i.e. a query like `{_id:{$in:[1,2,3]}}` was formerly fingerprinted just as `_id` but now it's `_id.$in` which is helpful to spot queries using operators which may cause performance issues
    + new: better identification of `command` i.e. `command` can be `count`, `findAndModify`, `distinct`, `collStats`, `aggregate` etc. which was formerly not itemized, so instead of seeing just `command` you see now `command.count` for example
    + new: better itemizing of aggregations, i.e. two pipelines `match` and `group` may be itemized as for example: `[$match._id, $group.city]` which makes identifying different aggregations easier
    + new: using java 1.8
* v0.1.1
    + new: sort legend by count or y-value
    + update: Eclipse's Dynamic Web Module facet changed from v2.5 to v3.0 (changes web-app tag in web.xml)
* v0.1.0
    + new: filter by date/time
    + new: filter by millis
    + change: deprecated tags removed from logback configuration
    + bugfix: respect time zone and day light saving time (data are still saved in GMT but now displayed dependent on time zone and day light saving time)
    + bugfix: zoom into graph could have resulted in a blank graph due to conflict with the previous date picker javascript library
    + bugfix: date formatting is now thread safe
* v0.0.2
    + logback configuration file
    + maven-war plugin update
* v0.0.1
    + initial release


## Third party libraries

* mongo-java-driver: [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
* slf4j: [MIT License](http://opensource.org/licenses/MIT)
* logback: [LGPL 2.1](http://www.gnu.org/licenses/old-licenses/lgpl-2.1)
* google-collections (Guava): [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
* jongo: [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
* jackson: [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
* bson4jackson: [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
* dygraph: [MIT License](http://opensource.org/licenses/MIT)
* bootstrap-datetimepicker: [Apache License 2.0](https://github.com/tarruda/bootstrap-datetimepicker)


## License

This software is licensed under [AGPL 3.0](http://www.gnu.org/licenses/agpl-3.0.html).
For details about the license, please see file "LICENSE", located in the same folder as this "README.md" file.



