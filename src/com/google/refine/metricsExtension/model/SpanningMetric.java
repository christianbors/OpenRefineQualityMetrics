package com.google.refine.metricsExtension.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.util.ParsingUtilities;
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

	@JsonProperty("spanningColumns")
	private List<String> spanningColumns;
	@JsonProperty("spanningEvaluable")
	private EvalTuple spanningEvaluable;

	@JsonCreator
	public SpanningMetric(@JsonProperty("name") String name, @JsonProperty("description") String description,
						  @JsonProperty("spanningColumns") List<String> columns) {
		this(name, description, "none", Concatenation.OR, columns);
	}

	@JsonCreator
	public SpanningMetric(@JsonProperty("name") String name, @JsonProperty("description") String description,
						  @JsonProperty("dataType") String dataType, @JsonProperty("concat") Concatenation concat,
						  @JsonProperty("spanningColumns") List<String> columns) {
		super(name, description, dataType, concat);
		this.spanningColumns = columns;
	}

	public static SpanningMetric load(String jsonObject) {
        try {
			return ParsingUtilities.mapper.readValue(jsonObject, SpanningMetric.class);
//        	SpanningMetric m = new SpanningMetric(o.getString("name"),
//        			o.getString("description"),
//        			o.getString("datatype"),
//        			Concatenation.valueOf(o.getString("concat")),
//        			new ArrayList<String>());
//        	m.setMeasure(new Float(o.getString("measure")));
//        	JSONArray colNameArray = o.getJSONArray("spanningColumns");
//        	for (int spanningIdx = 0; spanningIdx < colNameArray.length(); ++spanningIdx) {
//        		m.spanningColumns.add(colNameArray.getString(spanningIdx));
//        	}
//        	JSONObject spanningEval = o.getJSONObject("spanningEvaluable");
//        	m.addSpanningEvalTuple(MetaParser.parse(MetricUtils.decapitalize(spanningEval.getString("evaluable"))),
//        			spanningEval.getString("column"),
//        			spanningEval.getString("comment"),
//        			spanningEval.getBoolean("disabled"));
//			if (o.has("dirtyIndices")) {
//				JSONArray di = o.getJSONArray("dirtyIndices");
//				m.dirtyIndices = new HashMap<Integer, List<Boolean>>();
//				for (int i = 0; i < di.length(); ++i) {
//					JSONObject entry = di.getJSONObject(i);
//
//					List<Boolean> dirtyBools = new ArrayList<Boolean>();
//					JSONArray dirty = entry.getJSONArray("dirty");
//					for (int dirtyIndex = 0; dirtyIndex < dirty.length(); ++dirtyIndex) {
//						dirtyBools.add(dirty.getBoolean(dirtyIndex));
//					}
//					m.dirtyIndices.put(entry.getInt("index"), dirtyBools);
//				}
//			}
//			if (o.has("evaluables")) {
//				JSONArray evals = o.getJSONArray("evalTuples");
//				for (int i = 0; i < evals.length(); ++i) {
//					try {
//						JSONObject evalTuple = evals.getJSONObject(i);
//						m.addEvalTuple(MetaParser.parse(MetricUtils.decapitalize(evalTuple.getString("evaluable"))),
//								evalTuple.getString("column"),
//								evalTuple.getString("comment"),
//								evalTuple.getBoolean("disabled"));
//					} catch (ParsingException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
//            return m;
        } catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }

	public void addSpanningEvalTuple(String evaluable, String columnName, String comment, boolean evalDisabled) {
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
