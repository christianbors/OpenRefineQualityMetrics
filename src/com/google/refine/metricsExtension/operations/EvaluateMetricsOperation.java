package com.google.refine.metricsExtension.operations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.history.HistoryEntry;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricsColumn;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.changes.MetricChange;
import com.google.refine.metricsExtension.util.MetricUtils;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.operations.EngineDependentMassCellOperation;
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
		return super.createHistoryEntry(project, historyEntryID);
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

				public RowVisitor init(Properties bindings, MetricsOverlayModel model) {
					this.bindings = bindings;
					this.model = model;
					return this;
				}

				@Override
				public boolean visit(Project project, int rowIndex, Row row) {
					for (String columnName : model.getMetricsColumns()) {
						int colIndex = project.columnModel.getColumnIndexByName(columnName);
						Cell c = row.cells.get(colIndex);
						List<Metric> metrics = model.getMetrics(columnName);
						
						ExpressionUtils.bind(bindings, row, rowIndex, columnName, c);
	
						for (Metric m : metrics) {
							List<Boolean> evalResults = new ArrayList<Boolean>();
							boolean entryDirty = false;
	
							for (String eval : m.getEvaluables()) {
								boolean evalResult;
								try {
									evalResult = (Boolean) MetaParser.parse(eval).evaluate(bindings);
									if (!evalResult) {
										entryDirty = true;
									}
									evalResults.add(evalResult);
								} catch (ParsingException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
	
							if (entryDirty) {
								m.addDirtyIndex(rowIndex, evalResults);
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
					for (String columnName : model.getMetricsColumns()) {
						for (Metric m : model.getMetrics(columnName)) {
							float q = MetricUtils.determineQuality(bindings, m);
							m.setMeasure(q);
						}
						
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
