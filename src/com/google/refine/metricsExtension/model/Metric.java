
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

public class Metric implements Jsonizable {

    private String name;
    private String description;
    private float measure;
    private String columnName;
    private String dataType;

    private List<Evaluable> evaluables;
    private Map<Integer, List<Boolean>> dirtyIndices;

    public Metric(String name, String description) {
        this.name = name;
        this.description = description;
        this.measure = 0f;
    }

    public Metric() {
	}

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();

        writer.key("name").value(name);
        writer.key("measure").value(Float.toString(measure));
        writer.key("columnName").value(columnName);
        writer.key("datatype").value(dataType);
        writer.key("dirtyIndices");
        writer.array();
        for(Entry<Integer, List<Boolean>> d : dirtyIndices.entrySet()) {
        	writer.object().key("index").value(d.getKey());
        	writer.key("dirty").array();
        	for (Boolean dirtyBool : d.getValue()) {
        		writer.value(dirtyBool);
        	}
        	writer.endArray().endObject();
        }
        writer.endArray();

        writer.endObject();
    }

	public static Metric load(JSONObject o) {
        try {
        	Metric m = new Metric();
            m.setName(o.getString("name"));
            m.setDescription(o.getString("description"));
            m.setMeasure(new Float(o.getString("measure")));
            m.setColumnName(o.getString("columnName"));
            m.setDataType(o.getString("datatype"));
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
            // find a way how to determine the computation m.setComputation();
            return m;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
	
	public void addEvaluable(Evaluable evaluable) {
		this.evaluables.add(evaluable);
	}
    
    public void addDirtyIndex(int index, List<Boolean> dirty) {
    	dirtyIndices.put(index, dirty);
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

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Evaluable> getEvaluables() {
    	return evaluables;
    }

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

}
