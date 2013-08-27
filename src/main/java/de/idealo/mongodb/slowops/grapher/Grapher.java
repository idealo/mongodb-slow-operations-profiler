/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.grapher;

import java.text.*;
import java.util.*;

import org.jongo.*;
import org.slf4j.*;

import com.google.common.collect.*;
import com.mongodb.*;

import de.idealo.mongodb.slowops.dto.SlowOpsDto;
import de.idealo.mongodb.slowops.util.Util;


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
        final String scaleStr = Util.getProperty(Util.Y_AXIS_SCALE, Util.Y_AXIS_SECONDS);

        if(scaleStr.equals(Util.Y_AXIS_MILLISECONDS)){
            scale = 1;
        }else {
            scale = 1000;//default
        }
        LOG.info("scaleStr: " + scaleStr + " scale: " + scale);
    }
    
    private MongoCollection getProfilingCollection() {
        final String servers = Util.getProperty(Util.COLLECTOR_SERVER_ADDRESSES, "");
        final String dbName = Util.getProperty(Util.COLLECTOR_DATABASE, null);
        final String collName = Util.getProperty(Util.COLLECTOR_COLLECTION, null);
        try {
            final List<ServerAddress> addresses = Util.getServerAddresses(servers);
            
            final Mongo mongo = new Mongo(addresses);
            final DB db = mongo.getDB(dbName);
            final String login = Util.getProperty(Util.COLLECTOR_LOGIN, null);
            final String pw = Util.getProperty(Util.COLLECTOR_PASSWORD, null);
            if(login!= null && pw != null) {
                boolean ok = db.authenticate(login, pw.toCharArray());
                LOG.info("auth on " + addresses + " ok: " + ok);
            }
            
            db.setReadPreference(ReadPreference.secondaryPreferred());
            final Jongo jongo = new Jongo(db);
            return jongo.getCollection(collName);
        } catch (MongoException e) {
            LOG.error("Exception while connecting to: " + servers, e);
        }
        return null;
    }
    

    public SlowOpsDto aggregateSlowQueries(StringBuffer filter, Object[] params, StringBuffer groupExp, StringBuffer groupTime) {
        final SlowOpsDto result = new SlowOpsDto();
        final String[] customFields = new String[] {"count", "minSec", "maxSec"};
        List<AggregatedProfiling> queryResult = null;
        
        LOG.debug("filter: " + filter);
        LOG.debug("groupExp: " + groupExp);
        LOG.debug("groupTime: " + groupTime);
        
        try {
            final long begin = System.currentTimeMillis();
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
                    "avgRet : { $avg : '$nret' }," +
                    "minRet : { $min : '$nret' }," +
                    "maxRet : { $max : '$nret' }," +
                    "firstts : { $first : '$ts' }" +
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
            LOG.debug("Duration in ms: " + (System.currentTimeMillis() - begin));
        }catch(MongoException e) {
            LOG.warn("MongoException while aggreating.", e);
            result.setErrorMessage(e.getMessage());
            return result;
        }catch(IllegalArgumentException e) {
            LOG.warn("MongoException while aggreating.", e);
            result.setErrorMessage("Please enter only valid values!");
            return result;
        }
        
        
        final HashMap<Calendar, Set<AggregatedProfiling>> timeSeries = new HashMap<Calendar, Set<AggregatedProfiling>>();
        final HashBiMap<String, Integer> groups = HashBiMap.create();
        int index=0;
        Calendar minCalendar = new GregorianCalendar();
        double maxAvgMs=0;
        
        for (AggregatedProfiling entry : queryResult) {
            final AggregatedProfilingId id = entry.getId();
            final String idLabel = id.getLabel();
            
            if(!groups.containsKey(idLabel)) {
                groups.put(idLabel, Integer.valueOf(index++));
            }
            
            final Calendar calendar = id.getCalendar();
            
            Set<AggregatedProfiling> serie = timeSeries.get(calendar);
            
            if(serie == null) {
                serie = new HashSet<AggregatedProfiling>();
                timeSeries.put(calendar, serie);
            }
            
            serie.add(entry);
            
            if(minCalendar.after(calendar)) {
                minCalendar = calendar;
            }
            if(maxAvgMs < entry.getAvgMs()) {
                maxAvgMs = entry.getAvgMs();
            }
        }
        
        final StringBuffer dataGrid = new StringBuffer("\"Date,");
        
        final BiMap<Integer, String> inversedGroups = groups.inverse();
        for (int i = 0; i < index; i++) {
            dataGrid.append(inversedGroups.get(Integer.valueOf(i))).append(",");
            for (int n = 0; n < customFields.length; n++) {
                dataGrid.append(customFields[n]).append(",");
            }
        }
        
        for (String group : groups.keySet()) {
            dataGrid.append(group).append(",");
            for (int i = 0; i < customFields.length; i++) {
                dataGrid.append(customFields[i]).append(",");
            }
        }
        if(dataGrid.length() > 0) {
            dataGrid.deleteCharAt(dataGrid.length()-1);//remove last char
        }
        dataGrid.append("\\n\" + \n");
        
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        final DecimalFormat decimalFormat = new DecimalFormat("#.##", symbols); 
        
        //write one line to expand the y-axis to the negated max value so zooming-in values near 0 becomes easier  
        dataGrid.append("\"").append(dateFormat.format(minCalendar.getTime())).append(",");
        final int lineSize = groups.size()*(customFields.length+1);
        dataGrid.append(decimalFormat.format(maxAvgMs/-scale)).append(",");
        for (int i = 0; i < lineSize-1; i++) {
          dataGrid.append("0").append(",");
        }
        dataGrid.deleteCharAt(dataGrid.length()-1);//delete last comma
        
        dataGrid.append("\\n\" +\n");
        ///
        
        for (Calendar calendar : timeSeries.keySet()) {
            final Set<AggregatedProfiling> entries = timeSeries.get(calendar);
            
            dataGrid.append("\"").append(dateFormat.format(calendar.getTime())).append(",");
            
            final String[] values = new String[groups.size()*(customFields.length+1)];
            for (AggregatedProfiling entry : entries) {
                final AggregatedProfilingId id = entry.getId();
                final String idLabel = id.getLabel();
                final Integer idx = (Integer)groups.get(idLabel);
                final int startIdx = idx.intValue()*(customFields.length+1); 
                values[startIdx] = decimalFormat.format(entry.getAvgMs()/scale);//millis to seconds
                values[startIdx + 1] = ""+entry.getCount();
                values[startIdx + 2] = decimalFormat.format(entry.getMinMs()/scale);
                values[startIdx + 3] = decimalFormat.format(entry.getMaxMs()/scale);
            }
            for (int i = 0; i < values.length; i++) {
              dataGrid.append(values[i]==null?"0":values[i]).append(",");
            }
            dataGrid.deleteCharAt(dataGrid.length()-1);//delete last comma
            
            dataGrid.append("\\n\" +\n");
        }
        
        dataGrid.append("\"\",");
        
        final boolean[] visibilityValues = new boolean[groups.size()*(customFields.length+1)];
        int c=0;
        for (int i = 0; i < groups.size(); i++) {
            visibilityValues[c++] = true;
            for (int j = 0; j < customFields.length; j++) {
                visibilityValues[c++] = false;
            }
        }
        
        //LOG.debug(dataGrid.toString());
        
        result.setDataGrid(dataGrid);
        result.setVisibilityValues(visibilityValues);

        
        return result;
    }
    
}
