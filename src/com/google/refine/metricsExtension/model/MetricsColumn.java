package com.google.refine.metricsExtension.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.model.Column;
import com.google.refine.util.ParsingUtilities;


public class MetricsColumn {

//    private List<Metric> metrics;
    private Column column;
    private Map<Metric, List<Integer>> evalValues;

    public MetricsColumn(Column column, List<Metric> metrics) throws Exception {
    	this.column = column;
    	this.evalValues = new HashMap<Metric, List<Integer>>(metrics.size());
    	for (Metric m : metrics) {
    		evalValues.put(m, new ArrayList<Integer>());
    	}
    }
    
    public MetricsColumn(Column column) {
    	this.column = column;
    	this.evalValues = new HashMap<Metric, List<Integer>>();
    }

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object().key("column");
		column.write(writer, options);
		
		writer.key("evalValues").array();
		for(Map.Entry<Metric, List<Integer>> entry : evalValues.entrySet()) {
			writer.object().key("metric");
			entry.getKey().write(writer, options);
			
			writer.key("metricValues");
			writer.array();
			for (Integer val : entry.getValue()) {
				writer.value(val);
			}
			writer.endArray();
			writer.endObject();
		}
		writer.endArray();
		writer.endObject();
	}

	public static MetricsColumn load(String s) throws Exception {
		JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(s);
		
		MetricsColumn column = new MetricsColumn(Column.load(s));
		JSONArray metricsArray = obj.getJSONArray("evalValues");
		for (int i = 0; i < metricsArray.length(); ++i) {
			JSONObject evalValsObj = metricsArray.getJSONObject(i);
			Metric m = Metric.load(evalValsObj.getJSONObject("metric"));
			column.getMetrics().add(m);
		}
		
		return column;
	}

	public Set<Metric> getMetrics() {
		return evalValues.keySet();
	}

//	public void setMetrics(List<Metric> metrics) {
//		this.metrics = metrics;
//	}
	public void addMetric(Metric metric) {
		evalValues.put(metric, new LinkedList<Integer>());
	}

	public Column getColumn() {
		return column;
	}

	public void setColumn(Column column) {
		this.column = column;
	}
}
