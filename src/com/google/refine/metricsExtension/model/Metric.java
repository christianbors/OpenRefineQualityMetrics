
package com.google.refine.metricsExtension.model;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.model.Project;

public abstract class Metric<E> implements Jsonizable {

    protected String name;
    protected String description;
    protected float measure;
    private String columnName;

    // TODO: put constraints here
    private String constraints;

    public Metric(String name, String description) {
        this.name = name;
        this.description = description;
        this.measure = 0f;
    }

    public Metric<E> initializeFromJSON(Project project, JSONObject o) {
        try {
            name = o.getString("name");
            description = o.getString("description");
            measure = new Float(o.getString("measure"));
            columnName = o.getString("columnName");
            constraints = o.getString("constraints");
            return this;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();

        writer.key("metric").value(Float.toString(measure));
        writer.key("columnName").value(columnName);
        writer.key("constraints").value(constraints);

        writer.endObject();
    }

    public abstract boolean evaluateValue(Object value);

    public abstract boolean evaluateValue(Properties bindings);

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

    public String getConstraints() {
        return constraints;
    }

    public void setConstraints(String constraints) {
        this.constraints = constraints;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
