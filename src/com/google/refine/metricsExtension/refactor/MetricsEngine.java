package com.google.refine.metricsExtension.refactor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.facets.Facet;
import com.google.refine.browsing.facets.ListFacet;
import com.google.refine.browsing.facets.RangeFacet;
import com.google.refine.browsing.facets.ScatterplotFacet;
import com.google.refine.browsing.facets.TextSearchFacet;
import com.google.refine.browsing.facets.TimeRangeFacet;
import com.google.refine.metricsExtension.browsing.facets.MetricsFacet;
import com.google.refine.model.Project;


public class MetricsEngine extends Engine {

    public MetricsEngine(Project project) {
        super(project);
    }

    @Override
    public void initializeFromJSON(JSONObject o)
            throws JSONException {
        if (o == null) {
            return;
        }
        
        if (o.has("facets") && !o.isNull("facets")) {
            JSONArray a = o.getJSONArray("facets");
            int length = a.length();

            for (int i = 0; i < length; i++) {
                JSONObject fo = a.getJSONObject(i);
                String type = fo.has("type") ? fo.getString("type") : "list";

                Facet facet = null;
                if ("list".equals(type)) {
                    facet = new ListFacet();
                } else if ("range".equals(type)) {
                    facet = new RangeFacet();
                } else if ("timerange".equals(type)) {
                    facet = new TimeRangeFacet();
                } else if ("scatterplot".equals(type)) {
                    facet = new ScatterplotFacet();
                } else if ("text".equals(type)) {
                    facet = new TextSearchFacet();
                } else if ("metrics".equals(type)) {
                    facet = new MetricsFacet();
                } 
                if (facet != null) {
                    facet.initializeFromJSON(_project, fo);
                    _facets.add(facet);
                }
            }
        }

        // for backward compatibility
        if (o.has(INCLUDE_DEPENDENT) && !o.isNull(INCLUDE_DEPENDENT)) {
            _mode = o.getBoolean(INCLUDE_DEPENDENT) ? Mode.RecordBased : Mode.RowBased;
        }

        if (o.has(MODE) && !o.isNull(MODE)) {
            _mode = MODE_ROW_BASED.equals(o.getString(MODE)) ? Mode.RowBased : Mode.RecordBased;
        }
    }

}
