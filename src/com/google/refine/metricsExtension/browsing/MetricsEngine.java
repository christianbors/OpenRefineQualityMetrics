package com.google.refine.metricsExtension.browsing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.facets.Facet;
import com.google.refine.metricsExtension.browsing.facets.MetricsFacet;
import com.google.refine.model.Project;


public class MetricsEngine extends Engine {

    public MetricsEngine(Project project) {
        super(project);
    }

    @Override
    public void initializeFromJSON(JSONObject o)
            throws JSONException {
        super.initializeFromJSON(o);
        if (o.has("facets") && !o.isNull("facets")) {
            JSONArray a = o.getJSONArray("facets");
            int length = a.length();

            for (int i = 0; i < length; i++) {
                JSONObject fo = a.getJSONObject(i);
                String type = fo.has("type") ? fo.getString("type") : "list";

                Facet facet = null;
                if ("metrics".equals(type)) {
                    facet = new MetricsFacet();
                } 
                if (facet != null) {
                    facet.initializeFromJSON(_project, fo);
                    _facets.add(facet);
                }
            }
        }
    }

}
