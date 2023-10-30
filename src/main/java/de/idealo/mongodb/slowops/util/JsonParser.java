package de.idealo.mongodb.slowops.util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class JsonParser {
    private static final Logger LOG = LoggerFactory.getLogger(ProfilingReaderCreator.class);

    public static String generateJSONString(List<String> headers, List<List<Object>> dataList) {
        JSONArray jsonArray = new JSONArray();
        for (List<Object> data : dataList) {
            JSONObject jsonObject = new JSONObject();
            for (int i = 0; i < headers.size(); i++) {
                if (i < data.size()) {
                    Object raw_data = data.get(i);
                    // FIX: Hotfix for json serializer issue
                    raw_data = raw_data != null ? raw_data.toString().replaceAll("\"", "'") : null;
                    jsonObject.put(headers.get(i), raw_data);
                }
            }
            jsonArray.add(jsonObject);
        }
        return jsonArray.toJSONString();
    }
}
