package com.google.refine.metricsExtension.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;


public class MetricsOverlayModel implements OverlayModel {

	private Map<String, List<Metric>> metricsMap;
	private List<String> availableMetrics;
	
	public static MetricsOverlayModel reconstruct(JSONObject metricsOverlayModel) throws Exception {
		
		Map<String, List<Metric>> reconstructMap = new HashMap<String, List<Metric>>();
		if (metricsOverlayModel != null) {
			JSONArray metricColumns = metricsOverlayModel.getJSONArray("metricColumns");
			for (int i = 0; i < metricColumns.length(); ++i) {
				JSONObject obj = metricColumns.getJSONObject(i);
				reconstructMap.put(obj.getString("columnName"), reconstructMetrics(obj.getJSONArray("metrics")));
			}
		}
		MetricsOverlayModel overlayModel = new MetricsOverlayModel(reconstructMap);
		
		return overlayModel;
	}
	
    public MetricsOverlayModel(Map<String, List<Metric>> metricsMap) {
    	this.metricsMap = metricsMap;
    	availableMetrics = new LinkedList<String>();
    	for (Entry<String, List<Metric>> entry : metricsMap.entrySet()) {
    		for (Metric m : entry.getValue())
				if (!availableMetrics.contains(m.getName())) {
					availableMetrics.add(m.getName());
				}
    	}
	}

	@Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("metricColumns");
        writer.array();
        for (Entry<String, List<Metric>> e : metricsMap.entrySet()) {
        	writer.object().key("columnName").value(e.getKey());
        	writer.key("metrics").array();
        	for (Metric m : e.getValue()) {
        		m.write(writer, options);
        	}
        	writer.endArray();
        	writer.endObject();
        }
        writer.endArray();
        
        writer.key("availableMetrics");
        writer.array();
        for (String m : availableMetrics) {
        	writer.value(m);
        }
        writer.endArray();
        
        writer.endObject();
    }

    @Override
    public void onBeforeSave(Project project) {        
    }

    @Override
    public void onAfterSave(Project project) {        
    }

    @Override
    public void dispose(Project project) {        
    }

    public List<Metric> getMetricsForColumn(String columnName) {
    	return metricsMap.get(columnName);
    }
    
    public void addMetrics(String columnName, List<Metric> metrics) {
    	this.metricsMap.put(columnName, metrics);
    	for (Metric m : metrics) {
    		if(!availableMetrics.contains(m.getName())) {
    			availableMetrics.add(m.getName());
    		}
    	}
    }
    
    public List<Metric> getMetricsColumn(String columnName) {
    	return metricsMap.get(columnName);
    }
    
    public List<String> getMetricColumnNames() {
    	List<String> columnList = new LinkedList<String>();
    	columnList.addAll(metricsMap.keySet());
    	return columnList;
    }
    
    public List<String> getAvailableMetrics() {
    	return availableMetrics;
    }

	private static List<Metric> reconstructMetrics(JSONArray jsonArray) throws JSONException {
		List<Metric> metricsList = new ArrayList<Metric>();
		for (int i = 0; i < jsonArray.length(); ++i) {
			JSONObject o = jsonArray.getJSONObject(i);
			metricsList.add(Metric.load(o));
		}
		return metricsList;
	}

	public static MetricsOverlayModel load(Project project, JSONObject jsonObject) throws Exception {
        return reconstruct(jsonObject);
    }
}
