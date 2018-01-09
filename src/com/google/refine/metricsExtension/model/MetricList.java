package com.google.refine.metricsExtension.model;

import com.google.refine.Jsonizable;
import org.json.JSONException;
import org.json.JSONWriter;

import java.util.List;
import java.util.Properties;

public class MetricList implements Jsonizable {
    private List<Metric> metricList;

    public MetricList(List<Metric> metricList) {
        this.metricList = metricList;
    }

    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object().key("metricList").array();
        for(Metric m : metricList) {
            m.write(writer, options);
        }
        writer.endArray().endObject();
    }
}
