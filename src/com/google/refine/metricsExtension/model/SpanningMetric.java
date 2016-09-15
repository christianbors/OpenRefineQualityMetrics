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
import com.google.refine.metricsExtension.model.Metric.EvalTuple;
import com.google.refine.metricsExtension.util.MetricUtils;

public class SpanningMetric extends Metric {

	private List<String> spanningColumns;
	private EvalTuple spanningEvaluable;

	public SpanningMetric(String name, String description, List<String> columns) {
		this(name, description, "none", Concatenation.OR, columns);
	}
	
	public SpanningMetric(String name, String description, String dataType, Concatenation concat, 
			List<String> columns) {
		super(name, description, dataType, concat);
		this.spanningColumns = columns;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();

        writer.key("name").value(name);
        writer.key("measure").value(Float.toString(measure));
        writer.key("datatype").value(dataType);
        writer.key("description").value(description);
        writer.key("concat").value(concat.toString());
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
		writer.key("evalTuples").array();
        for (EvalTuple e : evaluables) {
        	writer.object();
        	char c[] = e.eval.toString().toCharArray();
        	c[0] = Character.toLowerCase(c[0]);
        	String evalString = new String(c);
        	writer.key("evaluable").value(evalString);
        	writer.key("column").value(e.column);
        	writer.key("comment").value(e.comment);
        	writer.key("disabled").value(e.disabled);
        	writer.endObject();
        }
        writer.endArray();
        
        writer.key("comments").array();
        for (String s : comments) {
        	writer.value(s);
        }
        writer.endArray();
        
        writer.key("spanningColumns").array();
        for (String colName : spanningColumns) {
        	writer.value(colName);
        }
        writer.endArray();
        
        if(spanningEvaluable != null) {
	        char c[] = spanningEvaluable.eval.toString().toCharArray();
	    	c[0] = Character.toLowerCase(c[0]);
	    	String evalString = new String(c);
	        writer.key("spanningEvaluable").object();
	        writer.key("evaluable").value(evalString);
	        writer.key("column").value("");
	    	writer.key("comment").value(spanningEvaluable.comment);
	    	writer.key("disabled").value(spanningEvaluable.disabled);
	    	writer.endObject();
        }

        writer.endObject();
	}
	
	public static SpanningMetric load(JSONObject o) {
        try {
			
        	SpanningMetric m = new SpanningMetric(o.getString("name"), 
        			o.getString("description"), 
        			o.getString("datatype"),
        			Concatenation.valueOf(o.getString("concat")),
        			new ArrayList<String>());
        	m.setMeasure(new Float(o.getString("measure")));
        	JSONArray colNameArray = o.getJSONArray("spanningColumns");
        	for (int spanningIdx = 0; spanningIdx < colNameArray.length(); ++spanningIdx) {
        		m.spanningColumns.add(colNameArray.getString(spanningIdx));
        	}
        	JSONObject spanningEval = o.getJSONObject("spanningEvaluable");
        	m.addSpanningEvalTuple(MetaParser.parse(MetricUtils.decapitalize(spanningEval.getString("evaluable"))),
        			spanningEval.getString("column"),
        			spanningEval.getString("comment"), 
        			spanningEval.getBoolean("disabled"));
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
				JSONArray evals = o.getJSONArray("evalTuples");
				for (int i = 0; i < evals.length(); ++i) {
					try {
						JSONObject evalTuple = evals.getJSONObject(i);
						m.addEvalTuple(MetaParser.parse(MetricUtils.decapitalize(evalTuple.getString("evaluable"))),
								evalTuple.getString("column"),
								evalTuple.getString("comment"), 
								evalTuple.getBoolean("disabled"));
					} catch (ParsingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
            return m;
        } catch (JSONException | NumberFormatException | ParsingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
	
	public void addSpanningEvalTuple(Evaluable evaluable, String columnName, String comment, boolean evalDisabled) {
		this.spanningEvaluable = new EvalTuple(evaluable, columnName, comment, evalDisabled);
	}
	
	public List<String> getSpanningColumns() {
		return spanningColumns;
	}

	public void setSpanningColumns(List<String> spanningColumns) {
		this.spanningColumns = spanningColumns;
	}

	public EvalTuple getSpanningEvaluable() {
		return spanningEvaluable;
	}

	public void setSpanningEvaluable(EvalTuple spanningEvaluable) {
		this.spanningEvaluable = spanningEvaluable;
	}

}
