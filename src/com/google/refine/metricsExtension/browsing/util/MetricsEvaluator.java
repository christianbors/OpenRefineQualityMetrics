package com.google.refine.metricsExtension.browsing.util;

import java.util.List;

import com.google.refine.browsing.RecordVisitor;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;

/**
 * This Evaluator collects all metrics possibilities that can be displayed
 * @author Christian Bors
 *
 */
public class MetricsEvaluator implements RowVisitor, RecordVisitor {

    /*
     * Configuration
     * e.g. which metrics should be considered
     */
    private List<Metric<?>> metricsList;
    private boolean detailedEvaluation;

    /*
     * Computed results
     */
    // MetricFacetChoices
    
    /*
     * intermediate variables
     */

    @Override
    public void start(Project project) {
        // TODO Evaluate all metric constraints
        
    }

    @Override
    public void end(Project project) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean visit(Project project, int rowIndex, Row row) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public boolean visit(Project project, Record record) {
        // TODO Auto-generated method stub
        return false;
    }

}
