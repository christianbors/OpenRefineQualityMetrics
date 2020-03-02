
package com.google.refine.metricsExtension.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.util.MetricUtils;
import com.google.refine.util.ParsingUtilities;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;


public class Metric {

	public enum Concatenation {
		AND, OR, XOR;
	}
	
	public class EvalTuple {
		@JsonProperty("evalString")
		public String evalString;
		@JsonProperty("column")
		public String column;
		@JsonProperty("comment")
		public String comment;
		@JsonProperty("disabled")
		public boolean disabled;

		@JsonCreator
		public EvalTuple(@JsonProperty("evalString") String evalString, @JsonProperty("column") String column,
						 @JsonProperty("comment") String comment, @JsonProperty("disabled") boolean disabled) {
			this.evalString = evalString;
			this.column = column;
			this.comment = comment;
			this.disabled = disabled;
		}

		@JsonProperty("eval")
		public Evaluable getEvaluable() {
			try {
				return MetaParser.parse(evalString);
			} catch (ParsingException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	@JsonProperty("name")
    protected String name;
	@JsonProperty("description")
    protected String description;
	@JsonProperty("measure")
    protected float measure;
	@JsonProperty("dataType")
    protected String dataType;
	@JsonProperty("concat")
    protected Concatenation concat;

	@JsonProperty("evaluables")
    protected List<EvalTuple> evaluables;
	@JsonProperty("comments")
    protected List<String> comments;
	@JsonProperty("dirtyIndices")
	@JsonInclude(JsonInclude.Include.NON_NULL)
    protected Map<Integer, List<Boolean>> dirtyIndices;

	@JsonCreator
    public Metric(String name, String description) {
        this(name, description, "unknown", Concatenation.OR);
    }

	@JsonCreator
    public Metric(String name, String description, String dataType) {
    	this(name, description, dataType, Concatenation.OR);
    }

    @JsonCreator
    public Metric(String name, String description, String dataType, Concatenation concat) {
    	this.name = name;
        this.description = description;
        this.measure = 0f;
        this.dataType = dataType;
        this.concat = concat;
        this.dirtyIndices = new HashMap<Integer, List<Boolean>>();
        this.evaluables = new ArrayList<EvalTuple>();
        this.comments = new ArrayList<String>();
    }

	public static Metric load(String json) {
        try {
//        	Metric m = new Metric(o.getString("name"),
//        			o.getString("description"),
//        			o.getString("datatype"),
//        			Concatenation.valueOf(o.getString("concat")));
//        	m.setMeasure(new Float(o.getString("measure")));
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
//				JSONArray evals = o.getJSONArray("evaluables");
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
            return ParsingUtilities.mapper.readValue(json, Metric.class);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }
	
	public void addEvalTuple(String evaluable, String column, String comment, boolean evalDisabled) {
		this.evaluables.add(new EvalTuple(evaluable, column, comment, evalDisabled));
	}
	
	public void addEvalTuple(EvalTuple evalTuple) {
		this.evaluables.add(evalTuple);
	}
	
	public EvalTuple editEvalTuple(int listIndex, String evalString, String column, String comment, boolean evalDisabled) {
		EvalTuple eval = this.evaluables.get(listIndex);
		eval.evalString = evalString;
		eval.column = column;
		eval.comment = comment;
		eval.disabled = evalDisabled;
		return eval;
	}
    
    public void addDirtyIndex(int index, List<Boolean> dirty) {
    	dirtyIndices.put(index, dirty);
    }

    public Map<Integer, List<Boolean>> getDirtyIndices() {
		return dirtyIndices;
	}

	public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public float getMeasure() {
        return measure;
    }

    public void setMeasure(float measure) {
        this.measure = measure;
    }
    
    public Concatenation getConcat() {
		return concat;
	}

	public void setConcat(Concatenation concat) {
		this.concat = concat;
	}

	public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<EvalTuple> getEvalTuples() {
    	return evaluables;
    }

	public List<String> getComments() {
		return comments;
	}

	public void addComments(String comment, int index) {
		this.comments.remove(index);
		this.comments.add(index, comment);
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

}
