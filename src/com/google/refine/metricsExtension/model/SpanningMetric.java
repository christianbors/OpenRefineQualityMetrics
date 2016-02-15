package com.google.refine.metricsExtension.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.util.MetricUtils;

public class SpanningMetric extends Metric {

	private List<String> spanningColumns;
	private Evaluable spanningEvaluable;

	public SpanningMetric(String name, String description, Evaluable spanningEvaluable, List<String> columns) {
		this(name, description, 0f, "none", Concatenation.OR, spanningEvaluable, columns);
	}
	
	public SpanningMetric(String name, String description, float measure,
			String dataType, Concatenation concat, Evaluable spanningEvaluable, List<String> columns) {
		super(name, description, measure, dataType, concat);
		this.spanningColumns = columns;
		this.spanningEvaluable = spanningEvaluable;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();

        writer.key("name").value(name);
        writer.key("measure").value(Float.toString(measure));
        writer.key("datatype").value(dataType);
        writer.key("description").value(description);
		if (!dirtyIndices.isEmpty()) {
			writer.key("dirtyIndices");
			writer.array();
			for (Entry<Integer, List<Boolean>> d : dirtyIndices.entrySet()) {
				writer.object().key("index").value(d.getKey());
				writer.key("dirty").array();
				for (Boolean dirtyBool : d.getValue()) {
					writer.value(dirtyBool);
				}
				writer.endArray().endObject();
			}
			writer.endArray();
		}
        writer.key("evaluables").array();
        for (Evaluable e : evaluables) {
        	writer.value(e.toString());
        }
        writer.endArray();
        
        writer.key("spanningColumns").array();
        for (String colName : spanningColumns) {
        	writer.value(colName);
        }
        writer.endArray();
        
        writer.key("spanningEvaluable").value(spanningEvaluable);

        writer.endObject();
	}
	
	public static SpanningMetric load(JSONObject o) {
        try {
        	SpanningMetric m = new SpanningMetric(o.getString("name"), 
        			o.getString("description"), 
        			new Float(o.getString("measure")), 
        			o.getString("datatype"),
        			Concatenation.valueOf(o.getString("concat")),
        			MetaParser.parse(MetricUtils.decapitalize(o.getString("spanningEvaluable"))), 
        			new ArrayList<String>());
        	JSONArray colNameArray = o.getJSONArray("spanningColumns");
        	for (int spanningIdx = 0; spanningIdx < colNameArray.length(); ++spanningIdx) {
        		m.spanningColumns.add(colNameArray.getString(spanningIdx));
        	}
			if (o.has("dirtyIndices")) {
				JSONArray di = o.getJSONArray("dirtyIndices");
				m.dirtyIndices = new HashMap<Integer, List<Boolean>>();
				for (int i = 0; i < di.length(); ++i) {
					JSONObject entry = di.getJSONObject(i);

					List<Boolean> dirtyBools = new ArrayList<Boolean>();
					JSONArray dirty = entry.getJSONArray("dirty");
					for (int dirtyIndex = 0; dirtyIndex < dirty.length(); ++dirtyIndex) {
						dirtyBools.add(dirty.getBoolean(dirtyIndex));
					}
					m.dirtyIndices.put(entry.getInt("index"), dirtyBools);
				}
			}
			if (o.has("evaluables")) {
				JSONArray evals = o.getJSONArray("evaluables");
				for (int i = 0; i < evals.length(); ++i) {
					m.addEvaluable(MetaParser.parse(evals.getString(i)));
				}
			}
            return m;
        } catch (JSONException | NumberFormatException | ParsingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
	
	public List<String> getSpanningColumns() {
		return spanningColumns;
	}

	public void setSpanningColumns(List<String> spanningColumns) {
		this.spanningColumns = spanningColumns;
	}

	public Evaluable getSpanningEvaluable() {
		return spanningEvaluable;
	}

	public void setSpanningEvaluable(Evaluable spanningEvaluable) {
		this.spanningEvaluable = spanningEvaluable;
	}

}
