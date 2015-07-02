package com.google.refine.metricsExtension.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.facets.Facet;
import com.google.refine.model.Project;


public class MetricsFacet implements Facet {

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public RowFilter getRowFilter(Project project) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RecordFilter getRecordFilter(Project project) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initializeFromJSON(Project project, JSONObject o)
            throws JSONException {
        // TODO Auto-generated method stub
        
    }

}
