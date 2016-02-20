
package com.google.refine.metricsExtension.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.util.MetricUtils;

public class Metric implements Jsonizable {

	public enum Concatenation {
		AND, OR, XOR;
	}
	
	public class EvalTuple {
		public Evaluable eval;
		public String comment;
		public boolean disabled;
		
		public EvalTuple(Evaluable eval, String comment, boolean disabled) {
			this.eval = eval;
			this.comment = comment;
			this.disabled = disabled;
		}
	}
	
    protected String name;
    protected String description;
    protected float measure;
    protected String dataType;
    protected Concatenation concat;

    protected List<EvalTuple> evaluables;
    protected List<String> comments;
    protected Map<Integer, List<Boolean>> dirtyIndices;

    public Metric(String name, String description) {
        this(name, description, "unknown", Concatenation.OR);
    }
    
    public Metric(String name, String description, String dataType) {
    	this(name, description, dataType, Concatenation.OR);
    }
    
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
        	writer.key("comment").value(e.comment);
        	writer.key("disabled").value(e.disabled);
        	writer.endObject();
        }
        writer.endArray();

        writer.endObject();
    }

	public static Metric load(JSONObject o) {
        try {
        	Metric m = new Metric(o.getString("name"), 
        			o.getString("description"),  
        			o.getString("datatype"), 
        			Concatenation.valueOf(o.getString("concat")));
        	m.setMeasure(new Float(o.getString("measure")));
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
			if (o.has("evalTuples")) {
				JSONArray evals = o.getJSONArray("evalTuples");
				for (int i = 0; i < evals.length(); ++i) {
					try {
						JSONObject evalTuple = evals.getJSONObject(i);
						m.addEvalTuple(MetaParser.parse(MetricUtils.decapitalize(evalTuple.getString("evaluable"))), 
								evalTuple.getString("comment"), 
								evalTuple.getBoolean("disabled"));
					} catch (ParsingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
            return m;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
	
	public void addEvalTuple(Evaluable evaluable, String comment, boolean evalDisabled) {
		this.evaluables.add(new EvalTuple(evaluable, comment, evalDisabled));
	}
	
	public void addEvalTuple(EvalTuple evalTuple) {
		this.evaluables.add(evalTuple);
	}
	
	public EvalTuple editEvalTuple(int listIndex, Evaluable evaluable, String comment, boolean evalDisabled) {
		EvalTuple eval = this.evaluables.get(listIndex);
		eval.eval = evaluable;
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
