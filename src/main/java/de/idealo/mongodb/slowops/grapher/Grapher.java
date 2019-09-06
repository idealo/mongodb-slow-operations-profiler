/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.grapher;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mongodb.DB;
import com.mongodb.MongoException;
import de.idealo.mongodb.slowops.dto.CollectorServerDto;
import de.idealo.mongodb.slowops.dto.SlowOpsDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import de.idealo.mongodb.slowops.util.ConfigReader;
import de.idealo.mongodb.slowops.util.Util;
import org.jongo.Aggregate.ResultsIterator;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 *
 *
 * @author kay.agahd
 * @since 04.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */




public class Grapher {

    private static final Logger LOG = LoggerFactory.getLogger(Grapher.class);

    private final int scale;

    private final MongoCollection coll;

    public Grapher() {
        coll = getProfilingCollection();
        final String scaleStr = ConfigReader.getString(ConfigReader.CONFIG, Util.Y_AXIS_SCALE,  Util.Y_AXIS_MILLISECONDS);

        if(scaleStr.equals(Util.Y_AXIS_MILLISECONDS)){
            scale = 1;
        }else {
            scale = 1000;//default
        }
        LOG.info("scaleStr: {} scale: {}",  scaleStr, scale);
    }

    private MongoCollection getProfilingCollection() {
        CollectorServerDto serverDto = ConfigReader.getCollectorServer();
;
        try{
            MongoDbAccessor mongo = new MongoDbAccessor(60000, -1, true, serverDto.getAdminUser(), serverDto.getAdminPw(), serverDto.getSsl(), serverDto.getHosts());
            DB db = mongo.getMongoDB(serverDto.getDb());
            Jongo jongo = new Jongo(db);
            MongoCollection result =  jongo.getCollection(serverDto.getCollection());

            if(result == null) {
                throw new IllegalArgumentException("Can't continue without profile collection for " + serverDto.getHosts());
            }

            return result;
        } catch (MongoException e) {
            LOG.error("Exception while connecting to: {}", serverDto.getHosts(), e);
        }
        return null;
    }


    public SlowOpsDto aggregateSlowQueries(StringBuffer filter, Object[] params, StringBuffer groupExp, StringBuffer groupTime) {
        final SlowOpsDto result = new SlowOpsDto();
        final String[] customFields = new String[] {"count", "minSec", "maxSec", "sumSec", "stdDevMs",
                "nRet", "minRet", "maxRet", "avgRet", "stdDevRet", "rKeys", "rDocs", "wDocs", "memSort"};

        //List<AggregatedProfiling> queryResult = null;
        ResultsIterator<AggregatedProfiling> queryResult = null;

        LOG.debug("filter: {}", filter);
        LOG.debug("groupExp: {}", groupExp);
        LOG.debug("groupTime: {}", groupTime);


        try {
            final Date begin = new Date();
            queryResult = coll.aggregate(filter.toString(), params)
            .and("{$group:{" +

                    "_id:{" +
                        groupExp.toString() +
                        groupTime.toString() +
                        "}," +
                    "count : { $sum : 1 }," +
                    "millis : { $sum : '$millis' }," +
                    "avgMs : { $avg : '$millis' }," +
                    "minMs : { $min : '$millis' }," +
                    "maxMs : { $max : '$millis' }," +
                    "stdDevMs : { $stdDevPop : '$millis' }," +
                    "nRet : { $sum : '$nret' }," +
                    "avgRet : { $avg : '$nret' }," +
                    "minRet : { $min : '$nret' }," +
                    "maxRet : { $max : '$nret' }," +
                    "stdDevRet : { $stdDevPop : '$nret' }," +
                    "firstts : { $first : '$ts' }," +
                    "keys : { $sum : '$keys' }," +
                    "docs : { $sum : '$docs' }," +
                    "del : { $sum : '$del' }," +
                    "ins : { $sum : '$ins' }," +
                    "mod : { $sum : '$mod' }," +
                    "sortstages : { $addToSet : '$sortstg' }" +
                    "}" +
                    "}" +
                  "}"
            )
            .and("{$sort:{" +
                    "firstts:1" +
                    "}" +
                   "}"
            )
            .as(AggregatedProfiling.class);
            LOG.debug("Duration in ms: {}", ((new Date()).getTime() - begin.getTime()));
        }catch(MongoException e) {
            LOG.warn("MongoException while aggreating.", e);
            result.setErrorMessage(e.getMessage());
            return result;
        }catch(IllegalArgumentException e) {
            LOG.warn("MongoException while aggreating.", e);
            result.setErrorMessage("Please enter only valid values!");
            return result;
        }


        final Map<Calendar, Set<AggregatedProfiling>> timeSeries = new TreeMap<Calendar, Set<AggregatedProfiling>>();
        final HashBiMap<String, Integer> groups = HashBiMap.create();
        final HashMap<String, AggregatedProfiling> labelSeries = new HashMap<String, AggregatedProfiling>();
        int index=0;
        Calendar minCalendar = new GregorianCalendar();
        double maxAvgMs=0;
        double maxMinMs=0;
        double maxMaxMs=0;
        long maxSumMs=0;

        for (AggregatedProfiling entry : queryResult) {
            final AggregatedProfilingId id = entry.getId();
            final String idLabel = id.getLabel(false);

            if(!groups.containsKey(idLabel)) {
                groups.put(idLabel, Integer.valueOf(index++));
            }

            if(!labelSeries.containsKey(idLabel)) {
                labelSeries.put(idLabel, entry.clone());//save copy of first entry for this label
            }else {//sum up the other same-label-entries with this one
                labelSeries.get(idLabel).combine(entry);
            }


            final Calendar calendar = id.getCalendar();

            Set<AggregatedProfiling> serie = timeSeries.get(calendar);

            if(serie == null) {
                serie = new HashSet<AggregatedProfiling>();
                timeSeries.put(calendar, serie);
            }

            serie.add(entry);

            if(minCalendar.after(calendar)) {
                minCalendar = (Calendar)calendar.clone();
            }
            if(maxAvgMs < entry.getAvgMs()) {
                maxAvgMs = entry.getAvgMs();
            }
            if(maxMinMs < entry.getMinMs()) {
                maxMinMs = entry.getMinMs();
            }
            if(maxMaxMs < entry.getMaxMs()) {
                maxMaxMs = entry.getMaxMs();
            }
            if(maxSumMs < entry.getMillis()) {
                maxSumMs = entry.getMillis();
            }
        }

        final StringBuffer labels = new StringBuffer("\"Date\",");
        final BiMap<Integer, String> inversedGroups = groups.inverse();
        for (int i = 0; i < index; i++) {
            labels.append("\"").append(inversedGroups.get(Integer.valueOf(i))).append("\",");
            for (int n = 0; n < customFields.length; n++) {
                labels.append("\"").append(customFields[n]).append("\",");
            }
        }
        if(labels.length() > 0) {
            labels.deleteCharAt(labels.length()-1);//remove last char
        }


        final StringBuffer dataGrid = new StringBuffer("[");
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        final DecimalFormat decimalFormat = new DecimalFormat("#", symbols);

        //write one line to expand the y-axis to the negated max value so zooming-in values near 0 becomes easier
        minCalendar.add(Calendar.MINUTE, -1);
        dataGrid.append("[new Date(\"").append(dateFormat.format(minCalendar.getTime())).append("\"),");//first group Date
        dataGrid.append(decimalFormat.format(maxAvgMs/-scale)).append(",0,");//first group Avg and Count
        dataGrid.append(decimalFormat.format(maxMinMs/-scale)).append(",");//first group MinMs
        dataGrid.append(decimalFormat.format(maxMaxMs/-scale)).append(",");//first group MaxMs
        dataGrid.append(decimalFormat.format(maxSumMs/-scale)).append(",");//first group SumMs
        dataGrid.append("0,");//first group StdDevMs
        dataGrid.append("0,");//first group NRet
        dataGrid.append("0,");//first group MinRet
        dataGrid.append("0,");//first group MaxRet
        dataGrid.append("0,");//first group AvgRet
        dataGrid.append("0,");//first group StdDevRet
        dataGrid.append("0,");//first group rKeys
        dataGrid.append("0,");//first group rDocs
        dataGrid.append("0,");//first group wDocs
        dataGrid.append("0,");//first group memSort

        final int lineSize = (groups.size()-1)*(customFields.length+1);//first group already added, thus -1
        for (int i = 0; i < lineSize; i++) {
            dataGrid.append("0").append(",");
        }
        dataGrid.deleteCharAt(dataGrid.length()-1);//delete last comma

        dataGrid.append("],\n");
        ///


        for (Calendar calendar : timeSeries.keySet()) {
            final Set<AggregatedProfiling> entries = timeSeries.get(calendar);

            dataGrid.append("[new Date(\"").append(dateFormat.format(calendar.getTime())).append("\"),");

            final String[] values = new String[groups.size()*(customFields.length+1)];
            for (AggregatedProfiling entry : entries) {
                final AggregatedProfilingId id = entry.getId();
                final String idLabel = id.getLabel(false);
                final Integer idx = (Integer)groups.get(idLabel);
                final int startIdx = idx.intValue()*(customFields.length+1);
                values[startIdx] = decimalFormat.format(entry.getAvgMs()/scale);//millis to seconds
                values[startIdx + 1] = ""+entry.getCount();
                values[startIdx + 2] = decimalFormat.format(entry.getMinMs()/scale);
                values[startIdx + 3] = decimalFormat.format(entry.getMaxMs()/scale);
                values[startIdx + 4] = decimalFormat.format(entry.getMillis()/scale);
                values[startIdx + 5] = decimalFormat.format(entry.getStdDevMs());
                //NRet:
                values[startIdx + 6] = decimalFormat.format(entry.getNRet());
                values[startIdx + 7] = decimalFormat.format(entry.getMinRet());
                values[startIdx + 8] = decimalFormat.format(entry.getMaxRet());
                values[startIdx + 9] = decimalFormat.format(entry.getAvgRet());
                values[startIdx + 10] = decimalFormat.format(entry.getStdDevRet());
                //Perf:
                values[startIdx + 11] = ""+entry.getKeys();
                values[startIdx + 12] = ""+entry.getDocs();
                values[startIdx + 13] = ""+(entry.getDel() + entry.getIns() + entry.getMod());
                values[startIdx + 14] = entry.hasSortStage()?"1":"0";

            }
            for (int i = 0; i < values.length; i++) {
              dataGrid.append(values[i]==null?"0":values[i]).append(",");
            }
            dataGrid.deleteCharAt(dataGrid.length()-1);//delete last comma

            dataGrid.append("],\n");
        }
        dataGrid.deleteCharAt(dataGrid.length()-1);//delete last comma

        dataGrid.append("];\n");

        final boolean[] visibilityValues = new boolean[groups.size()*(customFields.length+1)];
        int c=0;
        for (int i = 0; i < groups.size(); i++) {
            visibilityValues[c++] = true;
            for (int j = 0; j < customFields.length; j++) {
                visibilityValues[c++] = false;
            }
        }

        //LOG.debug(dataGrid.toString());


        result.setLabels(labels);
        result.setDataGrid(dataGrid);
        result.setVisibilityValues(visibilityValues);
        result.setLabelSeries(labelSeries);
            

        
        return result;

    }
    
    
   public static void main(String[] args) {
       //Grapher g = new Grapher();
       //g.aggregateSlowQueries();
       /*
       HashSet<String> a = new HashSet<String>();
       a.add("a");
       a.add("b");
       
       HashSet<String> b = new HashSet<String>();
       b.add("b");
       b.add("a");
       
       HashSet<HashSet<String>> fields = new HashSet<HashSet<String>>();
       fields.add(a);
       fields.add(b);
       
       for (HashSet<String> strings : fields) {
           for (String string : strings) {
               System.out.println(string);
           }
           
       }
       */
       //boolean[] b = new boolean[] {true, false};
       Boolean[] b = new Boolean[] {true, false};
       System.out.println(""+Arrays.toString(b));
       
   }

     
    
}
