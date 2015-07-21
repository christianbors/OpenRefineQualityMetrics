
package com.google.refine.metricsExtension.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.expr.Evaluable;
import com.google.refine.model.Project;

public class Metric<E> implements Jsonizable {

    private float measure;
    private List<E> validItems;
    private String columnName; 
    
    //TODO: put constraints here
    private String constraints;

    public Metric() {
        measure = 0f;
        validItems = new ArrayList<E>();
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();

        writer.key("elementList");

        writer.key("metric").value(Float.toString(measure));
        writer.key("columnName").value(columnName);
        writer.key("constraints").value(constraints);

        writer.endObject();
    }

    public float getMeasure() {
        return measure;
    }

    public void setMeasure(float measure) {
        this.measure = measure;
    }

    public List<E> getValidItems() {
        return validItems;
    }
}
