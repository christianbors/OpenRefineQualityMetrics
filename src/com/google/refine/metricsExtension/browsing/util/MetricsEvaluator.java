
package com.google.refine.metricsExtension.browsing.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.refine.browsing.RecordVisitor;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.metricsComputation.Computation;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Record;
import com.google.refine.model.Row;

/**
 * This Evaluator collects all metrics possibilities that can be displayed
 * 
 * @author Christian Bors
 *
 */
public class MetricsEvaluator implements RowVisitor, RecordVisitor {
    
    /*
     * Configuration e.g. which metrics should be considered
     */
    private Metric[] metrics;
    private String colName;
    private int cellIndex;

    private boolean detailedEvaluation;

    /*
     * Computed results
     */
    Map<String, MetricFacetChoice> choices = new HashMap<String, MetricFacetChoice>();

    /*
     * intermediate variables
     */
    Map<Metric, Computation> metricsCalculation = new HashMap<Metric, Computation>();

    public MetricsEvaluator(Metric[] metrics, String columnName, int cellIndex) {
        this.metrics = metrics;
        this.colName = columnName;
        this.cellIndex = cellIndex;
    }

    @Override
    public void start(Project project) {
        // TODO Evaluate all metric constraints
        // initialize all containers for counting here
    }

    @Override
    public void end(Project project) {
        // TODO Auto-generated method stub
        for (Metric m : metrics) {
            Computation calc = metricsCalculation.get(m);
            float measure = ((float) calc.valid / (float) (calc.valid + calc.spurious));
            m.setMeasure(measure);
        }
    }

    @Override
    public boolean visit(Project project, int rowIndex, Row row) {
        Properties bindings = ExpressionUtils.createBindings(project);

        processRow(project, rowIndex, row, bindings);

        return false;
    }

    @Override
    public boolean visit(Project project, Record record) {
        // TODO Auto-generated method stub
        return false;
    }

    private void processRow(Project project, int rowIndex, Row row, Properties bindings) {
        // TODO Auto-generated method stub
        for (Metric m : metrics) {
            if(!metricsCalculation.containsKey(m)) {
                metricsCalculation.put(m, new MetricsComputation());
            }
            
            if(m.evaluateValue(bindings)) {
                metricsCalculation.get(m).spurious++;
            } else {
                metricsCalculation.get(m).valid++;
            }
        }
    }
}
