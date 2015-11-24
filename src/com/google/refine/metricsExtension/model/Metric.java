
package com.google.refine.metricsExtension.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.metricsExtension.operations.evaluate.EvaluateCell;

public class Metric implements Jsonizable {

    private String name;
    private String description;
    private float measure;
    private String columnName;
    private String dataType;

    private List<EvaluateCell> evaluations;
    private List<Integer> dirtyIndices;

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
        for(Integer d : dirtyIndices) {
        	writer.value(d);
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
            m.dirtyIndices = new LinkedList<Integer>();
            for (int i = 0; i < di.length(); ++i) {
            	m.dirtyIndices.add(di.getInt(i));
            }
            // find a way how to determine the computation m.setComputation();
            return m;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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

    public List<Integer> compute() {
    	//TODO compute something
    	return dirtyIndices;
    }

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

}
