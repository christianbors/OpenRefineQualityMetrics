package com.google.refine.metricsExtension.model;

import com.google.refine.Jsonizable;
import org.json.JSONException;
import org.json.JSONWriter;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Recommendation implements Jsonizable {

    private Map<String, List<MetricRecommendation>> recommendedMetrics;

    public Recommendation(Map<String, List<MetricRecommendation>> recommendedMetrics) {
        this.recommendedMetrics = recommendedMetrics;
    }

    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        writer.key("columns");
        writer.array();
        for(Map.Entry<String, List<MetricRecommendation>> entry : this.recommendedMetrics.entrySet()) {
            writer.object();
            writer.key("column").value(entry.getKey());
            writer.key("metrics").array();
            for (MetricRecommendation recomm : entry.getValue()) {
                recomm.write(writer, options);
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endArray();

        writer.endObject();
    }
}
