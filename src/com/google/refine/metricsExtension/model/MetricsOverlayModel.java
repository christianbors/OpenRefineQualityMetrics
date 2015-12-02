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

	private Map<Integer, List<Metric>> metricsMap;
	
	public static MetricsOverlayModel reconstruct(JSONObject jsonObject) throws Exception {
		
		JSONArray metricsColumns = jsonObject.getJSONArray("metricsColumns");
		Map<Integer, List<Metric>> reconstructMap = new HashMap<Integer, List<Metric>>();
		for (int i = 0; i < metricsColumns.length(); ++i) {
			JSONObject obj = metricsColumns.getJSONObject(i);
			reconstructMap.put(obj.getInt("colIndex"), reconstructMetrics(obj.getJSONArray("metrics")));
		}
		MetricsOverlayModel overlayModel = new MetricsOverlayModel(reconstructMap);
		
		return overlayModel;
	}
	
    public MetricsOverlayModel(Map<Integer, List<Metric>> metricsMap) {
    	this.metricsMap = metricsMap;
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
    
    public List<Metric> getMetrics(int columnIndex) {
    	return metricsMap.get(columnIndex);
    }

    private static Metric reconstructMetric(JSONObject o) throws JSONException {
    	Metric m = new Metric(o.getString("name"), 
    			o.getString("description"), 
    			new Float(o.getString("measure")), 
    			o.getString("type"));
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
