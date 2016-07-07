package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.json.JSONException;
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
	
	final static protected Logger logger = LoggerFactory.getLogger("evaluate_selected_metric");
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
		Project project = getProject(request);
		Properties bindings = ExpressionUtils.createBindings(project);
		Engine engine = new Engine(project);
		
		MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		int metricIndex = Integer.parseInt(request.getParameter("metricIndex"));
		String column = request.getParameter("column");
		String metricNameString = request.getParameter("metric[name]");
		
		Metric metric = null;
		SpanningMetric spanningMetric = null;
		if(metricNameString.equals("uniqueness")) {
			spanningMetric = overlayModel.getUniqueness();
		} else if(column == null) {
			for(SpanningMetric spanMetric : overlayModel.getSpanMetricsList()) {
				if (spanMetric.getName().equals(metricNameString)) {
					spanningMetric = spanMetric;
					break;
				}
			}
		} else {
			metric = overlayModel.getMetricsColumn(column).get(metricIndex);
		}
		FilteredRows filteredRows = engine.getAllFilteredRows();
		
		List<Float> values = new ArrayList<Float>();
		if (column != null) {
			int cellIndex = project.columnModel.getColumnIndexByName(column);
			DescriptiveStatistics stats = new DescriptiveStatistics();
			filteredRows.accept(project, createAggregateRowVisitor(project, cellIndex, stats, values));
			
			Double median = stats.apply(new Median());
			DescriptiveStatistics madStats = new DescriptiveStatistics();
			for(double entry : stats.getValues()) {
				madStats.addValue(Math.abs(entry - median));
			}
			Double mad = madStats.apply(new Median());
			Double iqr = stats.getPercentile(75) - stats.getPercentile(25);
			Double sIQR = iqr/1.35f;
			bindings.put("stats", stats);
			bindings.put("mad", mad);
			bindings.put("iqr", iqr);
			bindings.put("siqr", sIQR);
			logger.info("median {}", median);
			logger.info("mad {}", mad);
			logger.info("iqr {}", iqr);
			logger.info("sIQR {}", sIQR);
			
		}
		
        filteredRows.accept(project, createEvaluateRowVisitor(bindings, response, overlayModel, metric, spanningMetric, column));
		} catch (Exception e) {
			respondException(response, e);
		} finally {
			ProjectManager.singleton.setBusy(false);
		}
	}
	
	protected RowVisitor createAggregateRowVisitor(Project project, int cellIndex, DescriptiveStatistics stats, List<Float> values) throws Exception {
        return new RowVisitor() {
            int cellIndex;
            DescriptiveStatistics stats;
            List<Float> values;
            
            public RowVisitor init(int cellIndex, DescriptiveStatistics stats, List<Float> values) {
                this.cellIndex = cellIndex;
                this.stats = stats;
                this.values = values;
                return this;
            }
            
            @Override
            public void start(Project project) {
            	// nothing to do
            }
            
            @Override
            public void end(Project project) {
            	// nothing to do
            }
            
            public boolean visit(Project project, int rowIndex, Row row) {
                try {
                    Number val = (Number)row.getCellValue(this.cellIndex);
                    this.values.add(val.floatValue());
                    this.stats.addValue(val.floatValue());
                } catch (Exception e) {
                }

                return false;
            }
        }.init(cellIndex, stats, values);
    }
	
	protected RowVisitor createEvaluateRowVisitor(Properties bindings, HttpServletResponse response, MetricsOverlayModel model, Metric metric, SpanningMetric spanningMetric, String column) {
		return new RowVisitor() {
			private Properties bindings;
			private HttpServletResponse response;
			private Metric metric;
			private SpanningMetric spanningMetric;
			private String column;
			
			public RowVisitor init(Properties bindings, HttpServletResponse response, Metric metric, SpanningMetric spanningMetric, String column) {
				this.bindings = bindings;
				this.response = response;
				this.metric = metric;
				this.spanningMetric = spanningMetric;
				this.column = column;
				return this;
			}

			@Override
			public boolean visit(Project project, int rowIndex, Row row) {
				// evaluate metrics
				WrappedCell ct;
				if(metric != null) {
					ct = (WrappedCell) row.getCellTuple(project).getField(column, bindings);
				} else {
					ct = (WrappedCell) row.getCellTuple(project).getField(spanningMetric.getSpanningColumns().get(0), bindings);
				}
				if (ct != null) {
					Cell c = ((WrappedCell )ct).cell;
					ExpressionUtils.bind(bindings, row, rowIndex, column, c);
					if(metric != null) {
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
					} else {
						List<Boolean> evalResults = new ArrayList<Boolean>();
						boolean entryDirty = false;
						
						Object spanEvalResult = spanningMetric.getSpanningEvaluable().eval.evaluate(bindings);
						if (spanEvalResult.getClass() != EvalError.class) {
							evalResults.add((Boolean) spanEvalResult);
							if(!(boolean) spanEvalResult) {
								entryDirty = true;
							}
						}
						for (EvalTuple evalTuple : spanningMetric.getEvalTuples()) {
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
				if(metric != null) {
					metric.setMeasure(1f - MetricUtils.determineQuality(bindings, metric));
					try {
						respondJSON(response, metric);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					spanningMetric.setMeasure(1f - MetricUtils.determineQuality(bindings, spanningMetric));
					try {
						respondJSON(response, spanningMetric);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.init(bindings, response, metric, spanningMetric, column);
	}
}
