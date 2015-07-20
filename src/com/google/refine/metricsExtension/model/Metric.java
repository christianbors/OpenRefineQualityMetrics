
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
import com.google.refine.model.Project;

public class Metric<E> implements Jsonizable {

    private float measure;
    protected List<E> validItems;
    protected Map<Integer, E> spuriousItemMap;

    public Metric() {
        measure = 0f;
        validItems = new ArrayList<E>();
        spuriousItemMap = new HashMap<Integer, E>();
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();

        writer.key("elementList");
        writer.array();
        for (Iterator<Map.Entry<Integer, E>> entries = spuriousItemMap.entrySet().iterator(); entries
                .hasNext();) {
            Map.Entry<Integer, E> entry = entries.next();
            writer.object();
            writer.key(entry.getKey().toString());
            writer.value(entries);
            writer.endObject();
        }
        writer.endArray();

        writer.key("metric").value(Float.toString(measure));

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

    public Map<Integer, E> getSpuriousItemMap() {
        return spuriousItemMap;
    }

}
