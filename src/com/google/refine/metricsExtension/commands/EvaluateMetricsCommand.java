package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.json.JSONException;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.commands.Command;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.WrappedCell;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.metricsExtension.util.EvaluateMetricsRowVisitor;
import com.google.refine.metricsExtension.util.MetricUtils;
import com.google.refine.metricsExtension.util.StatisticsUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class EvaluateMetricsCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Project project = getProject(request);
		MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels.get(MetricsOverlayModel.OVERLAY_NAME);
		Properties bindings = ExpressionUtils.createBindings(project);
		Engine engine = new Engine(project);

		if(overlayModel != null) {
            FilteredRows filteredRows = engine.getAllFilteredRows();
            filteredRows.accept(project, new EvaluateMetricsRowVisitor() {

                @Override public void end(Project project) {
                    // TODO: add AND/OR/XOR
                    if (model.getUniqueness() != null) {
                        Metric uniqueness = model.getUniqueness();
                        uniqueness.setMeasure(1f - MetricUtils.determineQuality(bindings, uniqueness));
                    }
                    for (String columnName : model.getMetricColumnNames()) {
                        for (Map.Entry<String, Metric> metricEntry : model.getMetricsForColumn(columnName).entrySet()) {
                            float q = 1f - MetricUtils.determineQuality(bindings, metricEntry.getValue());
                            metricEntry.getValue().setMeasure(q);
                        }
                    }
                    for (SpanningMetric sm : model.getSpanMetricsList()) {
                        sm.setMeasure(1f - MetricUtils.determineQuality(bindings, sm));
                    }
                    try {
                        ProjectManager.singleton.ensureProjectSaved(project.id);
                        respondJSON(response, model);
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }.init(bindings, response, overlayModel));
        } else {
            try {
                respondJSON(response, null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
	}
}
