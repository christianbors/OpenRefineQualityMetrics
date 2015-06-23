package com.google.refine.metricsExtension.metrics.column;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.model.Project;


public class Completeness extends ColumnMetric<Object, String> {

    @Override
    protected void writeSpecificProperty(JSONWriter writer, Properties options) throws JSONException {
        writer.key("test").value("value");
    }

    @Override
    protected boolean checkSpurious(Object val) {
        return (String.valueOf(val).isEmpty());
    }

    @Override
    protected void endVisit(Project project, List<Object> values, Map<Integer, String> spuriousValues, float metric) {
        System.out.println("visited, completeness: " + Float.toString(metric));
    }

    @Override
    protected void startVisit(Project project) {
        // TODO Auto-generated method stub
        
    }

}
