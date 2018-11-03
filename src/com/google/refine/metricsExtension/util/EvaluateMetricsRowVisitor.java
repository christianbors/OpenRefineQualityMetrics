package com.google.refine.metricsExtension.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.WrappedCell;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public abstract class EvaluateMetricsRowVisitor implements RowVisitor {

    private Properties bindings;
    private HttpServletResponse response;
    protected MetricsOverlayModel model;
    private Set<Cell[]> uniquesSet = new LinkedHashSet<Cell[]>();
    private SpanningMetric uniqueness;

    public EvaluateMetricsRowVisitor init(Properties bindings, HttpServletResponse response, MetricsOverlayModel model) {
        this.bindings = bindings;
        this.response = response;
        this.model = model;
        return this;
    }

    @Override public void start(Project project) {
        if (model.getUniqueness() != null) {
            uniqueness = model.getUniqueness();
            uniqueness.getDirtyIndices().clear();
        }
        for (String columnName : model.getMetricColumnNames()) {
            Map<String, Metric> metrics = model
                .getMetricsForColumn(columnName);
            for (Metric metric : metrics.values()) {
                metric.getDirtyIndices().clear();
            }
        }
        for (SpanningMetric sm : model.getSpanMetricsList()) {
            sm.getDirtyIndices().clear();
        }
    }

    @Override public boolean visit(Project project, int rowIndex, Row row) {
        // compute duplicates
        if (model.getUniqueness() != null && model.isComputeDuplicates()) {
            Cell[] cells = new Cell[uniqueness.getSpanningColumns().size()];
            for (int i = 0; i < uniqueness.getSpanningColumns().size(); ++i) {
                String columnName = uniqueness.getSpanningColumns().get(i);
                WrappedCell wc = (WrappedCell) row.getCellTuple(project).getField(columnName, bindings);
                cells[i] = wc.cell;
            }
            // evaluate here
            boolean foundDuplicate = false;
            for (Cell[] toBeChecked : uniquesSet) {
                foundDuplicate = true;
                for (int i = 0; i < toBeChecked.length; ++i) {
                    if (toBeChecked[i].value instanceof Date && cells[i].value instanceof Date) {
                        if (((Date) toBeChecked[i].value).getTime() != ((Date) cells[i].value).getTime()) {
                            foundDuplicate = false;
                            break;
                        }
                    } else {
                        if (!toBeChecked[i].value.equals(cells[i].value)) {
                            foundDuplicate = false;
                            break;
                        }
                    }
                }
                if (foundDuplicate) {
                    List<Boolean> errors = new ArrayList<Boolean>();
                    errors.add(false);
                    uniqueness.addDirtyIndex(rowIndex, new ArrayList<Boolean>(errors));
                }
            }
            if (!foundDuplicate) {
                // after that add the data
                uniquesSet.add(cells);
            }
        }
        List<SpanningMetric> spanMetrics = model.getSpanMetricsList();
        // evaluate metrics
        for (String columnName : model.getMetricColumnNames()) {
            WrappedCell ct = (WrappedCell) row.getCellTuple(project).getField(columnName, bindings);

            Map<String, Metric> metrics = model
                .getMetricsForColumn(columnName);

            for (Map.Entry<String, Metric> metricEntry : metrics.entrySet()) {
                List<Boolean> evalResults = new ArrayList<Boolean>();
                boolean entryDirty = false;

                for (Metric.EvalTuple evalTuple : metricEntry.getValue().getEvalTuples()) { 
                    if (!evalTuple.disabled) {
                        if (ct != null) {
                            Cell c = ((WrappedCell) ct).cell;
                            ExpressionUtils.bind(bindings, row, rowIndex, evalTuple.column, c);
                        } else {
                            ExpressionUtils.bind(bindings, row, rowIndex, evalTuple.column, null);
                        }
                        bindings.setProperty("columnName", evalTuple.column);

                        boolean evalResult;
                        Object evaluation = evalTuple.eval
                            .evaluate(bindings);
                        if (evaluation.getClass() != EvalError.class) {
                            evalResult = (Boolean) evaluation;
                            if (!evalResult) {
                                entryDirty = true;
                            }
                            evalResults.add(evalResult);
                        }
                    }
                }

                if (entryDirty) {
                    metricEntry.getValue().addDirtyIndex(rowIndex, evalResults);
                }
            }

            for (SpanningMetric sm : spanMetrics) {
                List<Boolean> evalResults = new ArrayList<Boolean>();
                boolean entryDirty = false;

                if (sm.getSpanningEvaluable() != null) {
                    Object spanEvalResult = sm.getSpanningEvaluable().eval.evaluate(bindings);
                    if (spanEvalResult.getClass() != EvalError.class) {
                        evalResults.add((Boolean) spanEvalResult);
                        if (!(boolean) spanEvalResult) {
                            entryDirty = true;
                        }
                    }
                }
                for (Metric.EvalTuple evalTuple : sm.getEvalTuples()) {
                    bindings.setProperty("columnName", evalTuple.column);

                    boolean evalResult;
                    Object evaluation = evalTuple.eval.evaluate(bindings);
                    if (evaluation.getClass() != EvalError.class) {
                        evalResult = (Boolean) evaluation;
                        if (!evalResult) {
                            entryDirty = true;
                        }
                        evalResults.add(evalResult);
                    }
                }

                if (entryDirty) {
                    sm.addDirtyIndex(rowIndex, evalResults);
                }
            }
        }
        return false;
    }

}
