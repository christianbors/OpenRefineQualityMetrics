package com.google.refine.metricsExtension.operations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jdk.nashorn.internal.ir.LiteralNode.ArrayLiteralNode.ArrayUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.CellTuple;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.expr.WrappedCell;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.metricsExtension.util.MetricUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;

public class EvaluateMetricsOperation extends EngineDependentOperation {

	private MetricsOverlayModel model;

	public EvaluateMetricsOperation(JSONObject engineConfig,
			MetricsOverlayModel model) {
		super(engineConfig);
		this.model = model;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
		//TODO: write results of evaluation
		writer.key("metricsOverlayModel");
		model.write(writer, options);

		writer.endObject();
	}

	@Override
	protected HistoryEntry createHistoryEntry(Project project,
			long historyEntryID) throws Exception {
		Change metricsProjectChange = new MetricsExtensionOperation.MetricsProjectChange(model);

		return new HistoryEntry(historyEntryID, project,
				getBriefDescription(project), this,
				metricsProjectChange);
	}

	@Override
	protected String getBriefDescription(Project project) {
		return "TODO";
	}

	static protected class MetricsEntry {
		final public int rowIndex;
		final public Cell cell;

		public MetricsEntry(int rowIndex, Cell cell) {
			this.rowIndex = rowIndex;
			this.cell = cell;
		}
	}

	@Override
	public Process createProcess(Project project, Properties options)
			throws Exception {
		return new EvaluateMetricsProcess(project, model, getBriefDescription(project));
	}

	private class EvaluateMetricsProcess extends LongRunningProcess implements Runnable {
		
		private Project project;
		private MetricsOverlayModel model;
		
		protected EvaluateMetricsProcess(Project project, MetricsOverlayModel model, String description) {
			super(description);
			this.project = project;
			this.model = model;
		}
		
		@Override
		public void run() {
			Properties bindings = ExpressionUtils.createBindings(project);
			Engine engine = new Engine(project);

			//TODO: metric bindings / getting certain infos into the bindings to perform a better evaluation
//			Properties bindings = MetricUtils.createBindings(project);
				
			FilteredRows filteredRows = engine.getAllFilteredRows();
            filteredRows.accept(project, new RowVisitor() {
				private Properties bindings;
				private MetricsOverlayModel model;
				private Set<Cell[]> uniquesSet = new LinkedHashSet<Cell[]>();

				public RowVisitor init(Properties bindings, MetricsOverlayModel model) {
					this.bindings = bindings;
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
							errors.add(true);
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

								for (Evaluable eval : m.getEvaluables()) {
									boolean evalResult;
									Object evaluation = eval.evaluate(bindings);
									if (evaluation.getClass() != EvalError.class) {
										evalResult = (Boolean) evaluation;
										if (!evalResult) {
											entryDirty = true;
										}
										evalResults.add(evalResult);
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
								}
								for (Evaluable eval : sm.getEvaluables()) {
									boolean evalResult;
									Object evaluation = eval.evaluate(bindings);
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
						model.getUniqueness().setMeasure(1f - MetricUtils.determineQuality(bindings, model.getUniqueness()));
					}
				}
			}.init(bindings, model));
            project.processManager.onDoneProcess(this);
		}

		@Override
		protected Runnable getRunnable() {
			return this;
		}

	}
}
