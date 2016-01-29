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
	private boolean computeDuplicates;
	private List<String> duplicateDependencies;
	private Metric uniqueness;
	
	public static MetricsOverlayModel reconstruct(JSONObject metricsOverlayModel) throws Exception {
		
		Map<String, List<Metric>> reconstructMap = new HashMap<String, List<Metric>>();
		if (metricsOverlayModel != null) {
			JSONArray metricColumns = metricsOverlayModel.getJSONArray("metricColumns");
			for (int i = 0; i < metricColumns.length(); ++i) {
				JSONObject obj = metricColumns.getJSONObject(i);
				reconstructMap.put(obj.getString("columnName"), reconstructMetrics(obj.getJSONArray("metrics")));
			}
		}
		boolean computeDuplicates = metricsOverlayModel.getBoolean("computeDuplicates");
		MetricsOverlayModel overlayModel;
		if (!computeDuplicates) {
			overlayModel = new MetricsOverlayModel(reconstructMap);
		} else {
			Metric uniqueness = Metric.load(metricsOverlayModel.getJSONObject("uniqueness"));
			List<String> duplicateDepList = new ArrayList<String>();
			JSONArray colNameArray = metricsOverlayModel.getJSONArray("duplicateDependencies");
			for (int i = 0; i < colNameArray.length(); ++i) {
				duplicateDepList.add(colNameArray.getString(i));
			}
			overlayModel = new MetricsOverlayModel(reconstructMap, duplicateDepList, uniqueness);
		}
		
		return overlayModel;
	}
	
    public MetricsOverlayModel(Map<String, List<Metric>> metricsMap) {
    	this.metricsMap = metricsMap;
    	this.computeDuplicates = false;
	}
    
    public MetricsOverlayModel(Map<String, List<Metric>> metricsMap, List<String> duplicateDependencies, Metric uniqueness) {
    	this(metricsMap);
    	this.computeDuplicates = true;
    	this.duplicateDependencies = duplicateDependencies;
    	this.uniqueness = uniqueness;
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
        
        writer.key("computeDuplicates").value(computeDuplicates);
        
		if (computeDuplicates) {
			writer.key("uniqueness");
			uniqueness.write(writer, options);
			
			writer.key("duplicateDependencies").array();
			for (String colName : duplicateDependencies) {
				writer.value(colName);
			}
			writer.endArray();
		}
        
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
    	List<String> availableMetrics = new LinkedList<String>();
    	for (Entry<String, List<Metric>> entry : metricsMap.entrySet()) {
    		for (Metric m : entry.getValue())
				if (!availableMetrics.contains(m.getName())) {
					availableMetrics.add(m.getName());
				}
    	}
    	return availableMetrics;
    }
    
    public boolean isComputeDuplicates() {
    	return computeDuplicates;
    }
    
    public void setComputeDuplicates(boolean computeDuplicates) {
    	this.computeDuplicates = computeDuplicates;
    }
    
    public List<String> getDuplicateDependencies() {
    	return duplicateDependencies;
    }
    
    public void setDuplicateDependencies(List<String> duplicateDependencies) {
    	this.duplicateDependencies = duplicateDependencies;
    }

	public Metric getUniqueness() {
		return uniqueness;
	}

	public void setUniqueness(Metric uniqueness) {
		this.uniqueness = uniqueness;
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
