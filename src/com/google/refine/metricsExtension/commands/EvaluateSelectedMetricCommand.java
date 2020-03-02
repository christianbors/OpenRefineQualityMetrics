package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.metricsExtension.model.MetricList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.google.refine.metricsExtension.util.MetricUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class EvaluateSelectedMetricCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			Project project = getProject(request);
			Properties bindings = ExpressionUtils.createBindings(project);
			Engine engine = new Engine(project);

			MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels.get(MetricsOverlayModel.OVERLAY_NAME);
			JSONArray selection = new JSONArray(request.getParameter("selection"));

			List<MetricSelection> metricSelections = new ArrayList<>();

			for (int selIdx = 0; selIdx < selection.length(); ++selIdx) {
				JSONObject selectionObj = selection.getJSONObject(selIdx);
				JSONObject metricJSON = new JSONObject(selectionObj.getString("metric"));
				if(metricJSON.has("spanningEvaluable")) {
					JSONArray colsJSON = new JSONArray(selectionObj.getString("columns"));
					ArrayList<String> cols = new ArrayList<String>();
					if (colsJSON != null) {
						int len = colsJSON.length();
						for (int i=0;i<len;i++){
							cols.add(colsJSON.get(i).toString());
						}
					}

					for (SpanningMetric currentSpanMetric : overlayModel.getSpanMetricsList()) {
						if (currentSpanMetric.getName().equals(metricJSON.getString("name"))
								&& currentSpanMetric.getSpanningColumns().size() == cols.size()
								&& currentSpanMetric.getSpanningColumns().containsAll(cols)) {
							metricSelections.add(new MetricSelection(currentSpanMetric, null, cols.toArray(new String[0])));
							break;
						}
					}
				} else {
					String column = selectionObj.getString("column");
					metricSelections.add(new MetricSelection(overlayModel.getMetricsColumn(column).get(metricJSON.getString("name")), column, null));
				}
			}

			FilteredRows filteredRows = engine.getAllFilteredRows();
	        filteredRows.accept(project, createEvaluateRowVisitor(bindings, response, overlayModel, metricSelections));
		} catch (Exception e) {
			respondException(response, e);
		} finally {
			ProjectManager.singleton.setBusy(false);
		}
	}
	
	protected RowVisitor createEvaluateRowVisitor(Properties bindings, HttpServletResponse response, MetricsOverlayModel model, List<MetricSelection> metricsSelection) {
		return new RowVisitor() {
			private Properties bindings;
			private HttpServletResponse response;
			private List<MetricSelection> metricsSelection;

			public RowVisitor init(Properties bindings, HttpServletResponse response, List<MetricSelection> metricsSelection) {
				this.bindings = bindings;
				this.response = response;
				this.metricsSelection = metricsSelection;
				return this;
			}

			@Override
			public boolean visit(Project project, int rowIndex, Row row) {
				// evaluate metrics
				for (MetricSelection selection : metricsSelection) {
					WrappedCell ct = (WrappedCell) row.getCellTuple(project).getField(selection.column, bindings);

					List<SpanningMetric> spanMetrics = model.getSpanMetricsList();

					List<Boolean> evalResults = new ArrayList<Boolean>();

					if(!(selection.metric instanceof SpanningMetric)) {
						for (EvalTuple evalTuple : selection.metric.getEvalTuples()) {
							if (!evalTuple.disabled) {
								if (ct != null) {
									Cell c = ((WrappedCell) ct).cell;
									ExpressionUtils.bind(bindings, row, rowIndex, evalTuple.column, c);
								} else {
									ExpressionUtils.bind(bindings, row, rowIndex, evalTuple.column, null);
								}
								bindings.setProperty("columnName", evalTuple.column);
								evalResults.add(this.evaluateTuple(evalTuple, bindings));
							}
						}

						if (evalResults.contains(false)) {
							selection.metric.addDirtyIndex(rowIndex, evalResults);
						}
					}
					else {
						SpanningMetric spanningMetric = (SpanningMetric) selection.metric;
						if(spanningMetric.getSpanningEvaluable() != null) {
							if (ct != null) {
								Cell c = ((WrappedCell) ct).cell;
								ExpressionUtils.bind(bindings, row, rowIndex, "", c);
							} else {
								ExpressionUtils.bind(bindings, row, rowIndex, "", null);
							}
							evalResults.add(this.evaluateTuple(spanningMetric.getSpanningEvaluable(), bindings));
						}
						for (EvalTuple evalTuple : spanningMetric.getEvalTuples()) {
							bindings.setProperty("columnName", evalTuple.column);
							evalResults.add(this.evaluateTuple(evalTuple, bindings));
						}

						if (evalResults.contains(false)) {
							spanningMetric.addDirtyIndex(rowIndex, evalResults);
						}
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
				List<Metric> metricsList = new ArrayList<>();
				for (MetricSelection sel : this.metricsSelection) {
					sel.metric.setMeasure(1f - MetricUtils.determineQuality(bindings, sel.metric));
					metricsList.add(sel.metric);
				}
				try {
					respondJSON(response, new MetricList(metricsList));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			private boolean evaluateTuple(EvalTuple evalTuple, Properties bindings) {
				boolean evalResult;
				Object evaluation = evalTuple.getEvaluable().evaluate(bindings);
				if (evaluation.getClass() != EvalError.class) {
					return (Boolean) evaluation;
				} else {
					return false;
				}
			}

		}.init(bindings, response, metricsSelection);
	}

	private class MetricSelection {
		Metric metric;
		String column;
		String[] columns;

		public MetricSelection(Metric metric, String column, String[] columns) {
			this.metric = metric;
			this.column = column;
			this.columns = columns;
		}
	}
}
