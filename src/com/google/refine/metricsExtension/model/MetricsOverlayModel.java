package com.google.refine.metricsExtension.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.metricsExtension.util.MetricUtils.RegisteredMetrics;
import com.google.refine.metricsExtension.util.MetricUtils.RegisteredSpanningMetrics;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;


public class MetricsOverlayModel implements OverlayModel {

	private Map<String, List<Metric>> metricsMap;
	private List<SpanningMetric> spanMetricsList;
	
	private boolean computeDuplicates;
	private SpanningMetric uniqueness;
	
	public static MetricsOverlayModel reconstruct(JSONObject metricsOverlayModel) throws Exception {
		
		if (metricsOverlayModel != null) {
			Map<String, List<Metric>> reconstructMap = new HashMap<String, List<Metric>>();
			MetricsOverlayModel overlayModel;
			JSONArray metricColumns = metricsOverlayModel.getJSONArray("metricColumns");
			for (int i = 0; i < metricColumns.length(); ++i) {
				JSONObject obj = metricColumns.getJSONObject(i);
				reconstructMap.put(obj.getString("columnName"), reconstructMetrics(obj.getJSONArray("metrics")));
			}
			List<SpanningMetric> spanMetrics;
			if (metricsOverlayModel.has("spanningMetrics")) {
				spanMetrics = reconstructSpanningMetrics(metricsOverlayModel
						.getJSONArray("spanningMetrics"));
			} else {
				spanMetrics = new ArrayList<SpanningMetric>();
			}
			
			boolean computeDuplicates = metricsOverlayModel.getBoolean("computeDuplicates");
			if (!computeDuplicates) {
				overlayModel = new MetricsOverlayModel(reconstructMap, spanMetrics);
			} else {
				SpanningMetric uniqueness = SpanningMetric.load(metricsOverlayModel.getJSONObject("uniqueness"));
				overlayModel = new MetricsOverlayModel(reconstructMap, spanMetrics, uniqueness);
			}
			return overlayModel;
		}
		return null;
	}
	
    public MetricsOverlayModel(Map<String, List<Metric>> metricsMap, List<SpanningMetric> spanMetricsList) {
    	this.metricsMap = metricsMap;
    	this.computeDuplicates = false;
    	this.spanMetricsList = spanMetricsList;
	}
    
    public MetricsOverlayModel(Map<String, List<Metric>> metricsMap, List<SpanningMetric> spanMetricsList, SpanningMetric uniqueness) {
    	this(metricsMap, spanMetricsList);
    	this.computeDuplicates = true;
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
        
        writer.key("availableMetrics");
        writer.array();
        for (RegisteredMetrics rm : getAvailableMetrics()) {
        	rm.write(writer, options);
        }
        writer.endArray();

        writer.key("availableSpanningMetrics");
        writer.array();
        for (RegisteredSpanningMetrics rm : getAvailableSpanningMetrics()) {
        	rm.write(writer, options);
        }
        writer.endArray();
        
		if (this.spanMetricsList.size() > 0) {
			writer.key("spanningMetrics").array();
			for (SpanningMetric sm : this.spanMetricsList) {
				sm.write(writer, options);
			}
			writer.endArray();
		}
        
        writer.key("computeDuplicates").value(computeDuplicates);
        
		if (computeDuplicates) {
			writer.key("uniqueness");
			uniqueness.write(writer, options);
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
    
    public void addMetric(String columnName, Metric metric) {
    	if(this.metricsMap.containsKey(columnName)) {
    		this.metricsMap.get(columnName).add(metric);
    	} else {
    		this.metricsMap.put(columnName, new ArrayList<Metric>(Arrays.asList(metric)));    		
    	}
    }
    
    public boolean deleteMetric(String columnName, String metricName) {
    	for(Metric m : this.metricsMap.get(columnName)) {
    		if(m.name.equals(metricName)) {
    			this.metricsMap.get(columnName).remove(m);
    			return true;
    		}
    	}
    	return false;
    }
    
    public void addSpanningMetric(SpanningMetric spanningMetric) {
    	this.spanMetricsList.add(spanningMetric);
    }
    
    public boolean deleteSpanningMetric(String metricName, String[] colNames) {
    	for(SpanningMetric sm : this.spanMetricsList) {
			if(!sm.getSpanningColumns().containsAll(Arrays.asList(colNames))) {
				return false;
			}
    		if(sm.name.equals(metricName)) {
    			this.spanMetricsList.remove(sm);
    			return true;
    		}
    	}
    	return false;
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
    
//    public List<String> getAvailableMetrics() {
//    	List<String> availableMetrics = new LinkedList<String>();
//    	for (Entry<String, List<Metric>> entry : metricsMap.entrySet()) {
//    		for (Metric m : entry.getValue())
//				if (!availableMetrics.contains(m.getName())) {
//					availableMetrics.add(m.getName());
//				}
//    	}
//    	return availableMetrics;
//    }
    
    public RegisteredMetrics[] getAvailableMetrics() {
    	return RegisteredMetrics.values();
    }
    
    public RegisteredSpanningMetrics[] getAvailableSpanningMetrics() {
    	return RegisteredSpanningMetrics.values();
    }
    
    public boolean isComputeDuplicates() {
    	return computeDuplicates;
    }
    
    public void setComputeDuplicates(boolean computeDuplicates) {
    	this.computeDuplicates = computeDuplicates;
    }
    
	public SpanningMetric getUniqueness() {
		return uniqueness;
	}

	public void setUniqueness(SpanningMetric uniqueness) {
		this.uniqueness = uniqueness;
	}

	public List<SpanningMetric> getSpanMetricsList() {
		return spanMetricsList;
	}

	public void setSpanMetricsList(List<SpanningMetric> spanMetricsList) {
		this.spanMetricsList = spanMetricsList;
	}

	private static List<Metric> reconstructMetrics(JSONArray jsonArray) throws JSONException {
		List<Metric> metricsList = new ArrayList<Metric>();
		for (int i = 0; i < jsonArray.length(); ++i) {
			JSONObject o = jsonArray.getJSONObject(i);
			metricsList.add(Metric.load(o));
		}
		return metricsList;
	}
	
	private static List<SpanningMetric> reconstructSpanningMetrics(JSONArray jsonArray) throws JSONException {
		List<SpanningMetric> spanningMetricsList = new ArrayList<SpanningMetric>();
		for (int i = 0; i < jsonArray.length(); ++i) {
			JSONObject o = jsonArray.getJSONObject(i);
			spanningMetricsList.add(SpanningMetric.load(o));
		}
		return spanningMetricsList;
	}

	public static MetricsOverlayModel load(Project project, JSONObject jsonObject) throws Exception {
        return reconstruct(jsonObject);
    }
}
