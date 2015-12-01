package com.google.refine.metricsExtension.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.expr.Evaluable;
import com.google.refine.model.Column;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;


public class MetricsOverlayModel implements OverlayModel {

	private List<MetricsColumn> metricsColumnList;
	
	public static MetricsOverlayModel reconstruct(JSONObject jsonObject) throws Exception {
		MetricsOverlayModel overlayModel = new MetricsOverlayModel();
		
		JSONArray metricsColumns = jsonObject.getJSONArray("metricsColumns");
		for (int i = 0; i < metricsColumns.length(); ++i) {
			JSONObject obj = metricsColumns.getJSONObject(i);
			overlayModel.getMetricsColumnList().add(reconstructColumn(obj));
		}
		
		return overlayModel;
	}
	
    public MetricsOverlayModel(List<MetricsColumn> metricsColumn) {
    	this.metricsColumnList = metricsColumn;
	}

	public MetricsOverlayModel() {
		this.metricsColumnList = new LinkedList<MetricsColumn>();
	}

	@Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        // TODO Auto-generated method stub
        
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
    
    public List<MetricsColumn> getMetricsColumnList() {
    	return metricsColumnList;
    }
    
    public Map<Integer, List<String>> getMetricColumns() {
    	Map<Integer, List<String>> eligibleColumns = new HashMap<Integer, List<String>>();
    	
    	return eligibleColumns;
    }
    
    protected static MetricsColumn reconstructColumn(JSONObject colObject) throws Exception {
		// TODO iterate through all possible metrics
    	// we also need to determine which computations are to be added to each metric
    	//String originalName, List<Metric> metrics
    	JSONArray metricsJson = colObject.getJSONArray("metrics");
    	List<Metric> metrics = new LinkedList<Metric>();
		if (metricsJson != null) {
			metrics.addAll(reconstructMetrics(colObject.getJSONArray("metrics")));
		}
		MetricsColumn metricsCol = new MetricsColumn(Column.load(colObject.getString("column")), metrics);
		
		return metricsCol;
	}

    private static Metric reconstructMetric(JSONObject o) throws JSONException {
    	Metric m = new Metric(o.getString("name"), o.getString("description"), o.getDouble("measure"));
    	JSONArray funJSON = o.getJSONArray("functions");
		for (int i = 0; i < funJSON.length(); i++) {
			m.addEvaluable(funJSON.getString(i));
		}
    	return m;
    }

	private static List<Metric> reconstructMetrics(JSONArray jsonArray) throws JSONException {
		List<Metric> metricsList = new ArrayList<Metric>();
		for (int i = 0; i < jsonArray.length(); ++i) {
			JSONObject o = jsonArray.getJSONObject(i);
			metricsList.add(reconstructMetric(o));
		}
		return metricsList;
	}

	public static MetricsOverlayModel load(Project project, JSONObject jsonObject) throws Exception {
        return reconstruct(jsonObject);
    }
}
