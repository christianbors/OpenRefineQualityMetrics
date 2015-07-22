
package com.google.refine.metricsExtension.model;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;

public abstract class Metric<E> implements Jsonizable {

    protected String name;
    protected String description;
    protected float measure;
    private String columnName; 
    
    //TODO: put constraints here
    private String constraints;

    public Metric(String name, String description) {
        this.name = name;
        this.description = description;
        this.measure = 0f;
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

    public abstract boolean evaluateValue(Object value);

}
