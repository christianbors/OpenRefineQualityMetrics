
package com.google.refine.metricsExtension.model;

import java.util.ArrayList;
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

import com.google.refine.Jsonizable;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;

public class Metric implements Jsonizable {

    private String name;
    private String description;
    private float measure;
    private String dataType;

    private List<String> evaluables;
    private Map<Integer, List<Boolean>> dirtyIndices;

    public Metric(String name, String description) {
        this(name, description, 0f, "unknown");
    }
    
    public Metric(String name, String description, float measure, String dataType) {
    	this.name = name;
        this.description = description;
        this.measure = measure;
        this.dataType = dataType;
        this.dirtyIndices = new HashMap<Integer, List<Boolean>>();
        this.evaluables = new ArrayList<String>();
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
        for (String e : evaluables) {
        	writer.value(e.toString());
        }
        writer.endArray();

        writer.endObject();
    }

	public static Metric load(JSONObject o) {
        try {
        	Metric m = new Metric(o.getString("name"), o.getString("description"), new Float(o.getString("measure")), o.getString("datatype"));
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
					m.addEvaluable(evals.getString(i));
				}
			}
            return m;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
	
	public void addEvaluable(String toBeParsed) {
		this.evaluables.add(toBeParsed);
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getEvaluables() {
    	return evaluables;
    }

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

}
