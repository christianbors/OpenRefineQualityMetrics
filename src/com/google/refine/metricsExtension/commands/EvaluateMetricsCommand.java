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
		MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		Properties bindings = ExpressionUtils.createBindings(project);
		Engine engine = new Engine(project);
		
		FilteredRows filteredRows = engine.getAllFilteredRows();		
        filteredRows.accept(project, new RowVisitor() {
			private Properties bindings;
			private HttpServletResponse response;
			private MetricsOverlayModel model;
			private Set<Cell[]> uniquesSet = new LinkedHashSet<Cell[]>();
			private SpanningMetric uniqueness;

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
							if(toBeChecked[i].value instanceof Date && cells[i].value instanceof Date) {
								if(((Date) toBeChecked[i].value).getTime() != ((Date) cells[i].value).getTime()) {
									foundDuplicate = false;
									break;
								}
							} else {
								if(!toBeChecked[i].value.equals(cells[i].value)) {
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
				// evaluate metrics
				for (String columnName : model.getMetricColumnNames()) {
					WrappedCell ct = (WrappedCell) row.getCellTuple(project).getField(columnName, bindings);
					
					Map<String, Metric> metrics = model
							.getMetricsForColumn(columnName);
					List<SpanningMetric> spanMetrics = model.getSpanMetricsList();

					for (Map.Entry<String, Metric> metricEntry : metrics.entrySet()) {
						List<Boolean> evalResults = new ArrayList<Boolean>();
						boolean entryDirty = false;

						for (EvalTuple evalTuple : metricEntry.getValue().getEvalTuples()) {
							if (!evalTuple.disabled) {
								if (ct != null) {
									Cell c = ((WrappedCell )ct).cell;
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
						
						if(sm.getSpanningEvaluable() != null) {
							Object spanEvalResult = sm.getSpanningEvaluable().eval.evaluate(bindings);
							if (spanEvalResult.getClass() != EvalError.class) {
								evalResults.add((Boolean) spanEvalResult);
								if (!(boolean) spanEvalResult) {
									entryDirty = true;
								}
							}
						}
						for (EvalTuple evalTuple : sm.getEvalTuples()) {
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

			@Override
			public void start(Project project) {
				if (model.getUniqueness() != null) {
					uniqueness = model.getUniqueness();
					uniqueness.getDirtyIndices().clear();
				}
			}

			@Override
			public void end(Project project) {
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
	}
}
