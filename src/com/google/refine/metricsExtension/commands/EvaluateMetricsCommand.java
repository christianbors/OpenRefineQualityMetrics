package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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

public class EvaluateMetricsCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Project project = getProject(request);
		MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		Properties bindings = ExpressionUtils.createBindings(project);
		Engine engine = new Engine(project);
		
		FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, new RowVisitor() {
			private Properties bindings;
			private HttpServletResponse response;
			private MetricsOverlayModel model;
			private Set<Cell[]> uniquesSet = new LinkedHashSet<Cell[]>();

			public RowVisitor init(Properties bindings, HttpServletResponse response, MetricsOverlayModel model) {
				this.bindings = bindings;
				this.response = response;
				this.model = model;
				return this;
			}

			@Override
			public boolean visit(Project project, int rowIndex, Row row) {
				// compute duplicates
				if (model.isComputeDuplicates()) {
					SpanningMetric uniqueness = model.getUniqueness();
					Cell[] cells = new Cell[uniqueness.getSpanningColumns().size()];
					for (int i = 0; i < uniqueness.getSpanningColumns().size(); ++i) {
						String columnName = uniqueness.getSpanningColumns().get(i);
						WrappedCell wc = (WrappedCell) row.getCellTuple(project).getField(columnName, bindings);
						cells[i] = wc.cell;
					}
					// evaluate here
					boolean foundDuplicate = false;
					for (Cell[] toBeChecked : uniquesSet) {
						for (int i = 0; i < toBeChecked.length; ++i) {
							if(!toBeChecked[i].value.equals(cells[i].value)) {
								break;
							}
						}
						List<Boolean> errors = new ArrayList<Boolean>();
						errors.add(false);
						model.getUniqueness().addDirtyIndex(rowIndex, new ArrayList<Boolean>(errors));
						foundDuplicate = true;
					}
					if (!foundDuplicate) {
						// after that add the data
						uniquesSet.add(cells);
					}
				}
				// evaluate metrics
				for (String columnName : model.getMetricColumnNames()) {
					WrappedCell ct = (WrappedCell) row.getCellTuple(project).getField(columnName, bindings);
					if (ct != null) {
						Cell c = ((WrappedCell )ct).cell;
						List<Metric> metrics = model
								.getMetricsForColumn(columnName);
						List<SpanningMetric> spanMetrics = model.getSpanMetricsList();

						ExpressionUtils.bind(bindings, row, rowIndex,
								columnName, c);

						for (Metric m : metrics) {
							List<Boolean> evalResults = new ArrayList<Boolean>();
							boolean entryDirty = false;

							for (EvalTuple evalTuple : m.getEvalTuples()) {
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
								m.addDirtyIndex(rowIndex, evalResults);
							}
						}
						
						for (SpanningMetric sm : spanMetrics) {
							List<Boolean> evalResults = new ArrayList<Boolean>();
							boolean entryDirty = false;
							
							Object spanEvalResult = sm.getSpanningEvaluable().evaluate(bindings);
							if (spanEvalResult.getClass() != EvalError.class) {
								evalResults.add((Boolean) spanEvalResult);
								if(!(boolean) spanEvalResult) {
									entryDirty = true;
								}
							}
							for (EvalTuple evalTuple : sm.getEvalTuples()) {
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
				for (SpanningMetric sm : model.getSpanMetricsList()) {
					sm.setMeasure(1f - MetricUtils.determineQuality(bindings, sm));
				}
				try {
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
	}
}
