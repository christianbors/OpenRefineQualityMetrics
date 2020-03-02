package com.google.refine.metricsExtension.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.google.refine.metricsExtension.util.MetricUtils.RegisteredMetrics;
import com.google.refine.metricsExtension.util.MetricUtils.RegisteredSpanningMetrics;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import java.io.IOException;
import java.util.*;

@JsonRootName(value = "metricsOverlayModel", namespace="overlayModels")
public class MetricsOverlayModel implements OverlayModel {

	public static final String OVERLAY_NAME = "metricsOverlayModel";

	@JsonProperty("metrics")
	private Map<String, Map<String, Metric>> metricsMap;
	@JsonProperty("spanningMetrics")
	private List<SpanningMetric> spanMetricsList;

	@JsonProperty("computeDuplicated")
	private boolean computeDuplicates;
	@JsonProperty("uniquenessMetric")
	private SpanningMetric uniqueness;
	
	static public MetricsOverlayModel reconstruct(String json) throws IOException {
		return ParsingUtilities.mapper.readValue(json, MetricsOverlayModel.class);
	}

	@JsonCreator
    public MetricsOverlayModel(@JsonProperty("metrics") Map<String, Map<String, Metric>> metricsMap,
							   @JsonProperty("spanningMetrics") List<SpanningMetric> spanMetricsList) {
    	this.metricsMap = metricsMap;
    	this.computeDuplicates = false;
    	this.spanMetricsList = spanMetricsList;
	}

	@JsonCreator
	public MetricsOverlayModel(@JsonProperty("metrics") Map<String, Map<String, Metric>> metricsMap,
							   @JsonProperty("spanningMetrics")List<SpanningMetric> spanMetricsList,
							   @JsonProperty("uniquenessMetric") SpanningMetric uniqueness) {
    	this(metricsMap, spanMetricsList);
    	this.computeDuplicates = true;
    	this.uniqueness = uniqueness;
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

    public Map<String, Metric> getMetricsForColumn(String columnName) {
    	return metricsMap.get(columnName);
    }
    
    public void addMetric(String columnName, Metric metric) {
    	if(this.metricsMap.containsKey(columnName)) {
    		this.metricsMap.get(columnName).put(metric.name, metric);
    	} else {
    		this.metricsMap.put(columnName, new HashMap<String, Metric>());
    		this.metricsMap.get(columnName).put(metric.name, metric);
    	}
    }
    
    public boolean deleteMetric(String columnName, String metricName) {
    	if (this.metricsMap.get(columnName).remove(metricName) != null) {
			return true;
		} else {
			return false;
		}
    }
    
    public void addSpanningMetric(SpanningMetric spanningMetric) {
    	this.spanMetricsList.add(spanningMetric);
    }
    
    public boolean deleteSpanningMetric(String metricName, String[] colNames) {
    	for(SpanningMetric sm : this.spanMetricsList) {
    		if(sm.name.equals(metricName)) {
    			if(sm.getSpanningColumns().size() == colNames.length && sm.getSpanningColumns().containsAll(Arrays.asList(colNames))) {
    				this.spanMetricsList.remove(sm);
    				return true;    				
    			}
    		}
    	}
    	return false;
    }
    
    public void addMetrics(String columnName, Map<String, Metric> metrics) {
    	this.metricsMap.put(columnName, metrics);
    }
    
    public Map<String, Metric> getMetricsColumn(String columnName) {
    	return metricsMap.get(columnName);
    }

    @JsonProperty("metricColumnNames")
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

	@JsonProperty("availableMetrics")
    public RegisteredMetrics[] getAvailableMetrics() {
    	return RegisteredMetrics.values();
    }

	@JsonProperty("availableSpanningMetrics")
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
		computeDuplicates = true;
	}

	public List<SpanningMetric> getSpanMetricsList() {
		return spanMetricsList;
	}

	public void setSpanMetricsList(List<SpanningMetric> spanMetricsList) {
		this.spanMetricsList = spanMetricsList;
	}

//	private static Map<String, Metric> reconstructMetrics(JSONObject jsonMetricsMap) throws JSONException {
//		Map<String, Metric> metricsMap = new HashMap<String, Metric>();
//		Iterator<?> keys = jsonMetricsMap.keys();
//		while (keys.hasNext()) {
//			String key = (String) keys.next();
//			JSONObject o = (JSONObject) jsonMetricsMap.get(key);
//			metricsMap.put((String) o.get("name"), Metric.load(o));
//		}
//		return metricsMap;
//	}
//
//	private static List<SpanningMetric> reconstructSpanningMetrics(JSONArray jsonArray) throws JSONException {
//		List<SpanningMetric> spanningMetricsList = new ArrayList<SpanningMetric>();
//		for (int i = 0; i < jsonArray.length(); ++i) {
//			JSONObject o = jsonArray.getJSONObject(i);
//			spanningMetricsList.add(SpanningMetric.load(o));
//		}
//		return spanningMetricsList;
//	}

	public static MetricsOverlayModel load(Project project, String jsonObject) throws Exception {
        return reconstruct(jsonObject);
    }
}
