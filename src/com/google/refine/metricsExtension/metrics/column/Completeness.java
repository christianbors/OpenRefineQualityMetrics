
package com.google.refine.metricsExtension.metrics.column;

import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.model.Project;

public class Completeness extends ColumnMetric<Object> {

    @Override
    protected boolean checkSpurious(Object val) {
        if (val instanceof Long 
                || val instanceof Double
                || val instanceof Float) {
                return val == null;
        } else if (val instanceof String) {
            return ((String) val).isEmpty();
        }
        //TODO: add function to check date
        return true;
    }

    @Override
    protected void endVisit(Project project, Metric<Object> metric) {
        // System.out.println("visited, completeness: " +
        // Float.toString(metric.getMeasure()));
    }

    @Override
    protected void startVisit(Project project) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {
        return "completeness";
    }

}
