package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

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
import com.google.refine.metricsExtension.util.MetricUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class EvaluateSelectedMetricCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Project project = getProject(request);
		MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		int metricIndex = Integer.parseInt(request.getParameter("metricIndex"));
		String columnName = request.getParameter("columnName");
		Metric metric = overlayModel.getMetricsColumn(columnName).get(metricIndex);
		Properties bindings = ExpressionUtils.createBindings(project);
		Engine engine = new Engine(project);
		
		FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, new RowVisitor() {
			private Properties bindings;
			private HttpServletResponse response;
			private MetricsOverlayModel model;
			private Metric metric;
			private String columnName;
			
			public RowVisitor init(Properties bindings, HttpServletResponse response, MetricsOverlayModel model, Metric metric, String columnName) {
				this.bindings = bindings;
				this.response = response;
				this.model = model;
				this.metric = metric;
				this.columnName = columnName;
				return this;
			}

			@Override
			public boolean visit(Project project, int rowIndex, Row row) {
				// evaluate metrics
					WrappedCell ct = (WrappedCell) row.getCellTuple(project).getField(columnName, bindings);
					if (ct != null) {
						Cell c = ((WrappedCell )ct).cell;

						ExpressionUtils.bind(bindings, row, rowIndex,
								columnName, c);

						List<Boolean> evalResults = new ArrayList<Boolean>();
						boolean entryDirty = false;

						for (EvalTuple evalTuple : metric.getEvalTuples()) {
							if (!evalTuple.disabled) {
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
							metric.addDirtyIndex(rowIndex, evalResults);
						}
					}
				return false;
			}

			@Override
			public void start(Project project) {
			}

			@Override
			public void end(Project project) {
				// TODO: add AND/OR/XOR
				for (String columnName : model.getMetricColumnNames()) {
					for (Metric m : model.getMetricsForColumn(columnName)) {
						float q = 1f - MetricUtils.determineQuality(bindings, m);
						m.setMeasure(q);
					}
					Metric uniqueness = model.getUniqueness();
					uniqueness.setMeasure(1f - MetricUtils.determineQuality(bindings, uniqueness));
				}
				try {
					respondJSON(response, metric);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.init(bindings, response, overlayModel, metric, columnName));
	}
}
