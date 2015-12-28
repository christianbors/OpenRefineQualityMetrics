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

public class EvaluateMetricsOperation extends EngineDependentMassCellOperation {

	private MetricsOverlayModel model;
	private String columnName;

	public EvaluateMetricsOperation(JSONObject engineConfig,
			MetricsOverlayModel model, String columnName) {
		super(engineConfig, columnName, true);
		this.model = model;
		this.columnName = columnName;
	}

	static public AbstractOperation reconstruct(Project project, JSONObject obj)
			throws Exception {
		JSONObject engineConfig = obj.has("engineConfig")
				&& !obj.isNull("engineConfig") ? obj
				.getJSONObject("engineConfig") : null;

		return new EvaluateMetricsOperation(engineConfig,
				(MetricsOverlayModel) obj.get("metricsOverlayModel"),
				obj.getString("columnName"));
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
		writer.key("columnName").value(columnName);

		writer.key("metricsOverlayModel");
		model.write(writer, options);

		writer.endObject();
	}

	@Override
	protected HistoryEntry createHistoryEntry(Project project,
			long historyEntryID) throws Exception {
		// TODO Auto-generated method stub
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
	protected RowVisitor createRowVisitor(Project project,
			List<CellChange> cellChanges, long historyEntryID) throws Exception {
		Column column = project.columnModel.getColumnByName(_columnName);

		Properties bindings = ExpressionUtils.createBindings(project);

		Evaluable eval = MetaParser.parse("Test");
		//TODO: metric bindings / getting certain infos into the bindings to perform a better evaluation
//		Properties bindings = MetricUtils.createBindings(project);
		List<Metric> metrics = model.getMetrics(column.getCellIndex());

		return new RowVisitor() {
			private int cellIndex;
			private long historyEntryID;
			private Properties bindings;
			private List<Metric> metrics;

			public RowVisitor init(int cellIndex, Properties bindings,
					long historyEntryID, List<Metric> metrics) {
				this.cellIndex = cellIndex;
				this.bindings = bindings;
				this.historyEntryID = historyEntryID;
				this.metrics = metrics;
				return this;
			}

			@Override
			public boolean visit(Project project, int rowIndex, Row row) {
				Cell cell = row.getCell(cellIndex);
				ExpressionUtils.bind(bindings, row, rowIndex, _columnName,
						cell);

				for (Metric m : metrics) {
					List<Boolean> evalResults = new ArrayList<Boolean>();
					boolean entryDirty = false;

					for (Evaluable eval : m.getEvaluables()) {
						boolean evalResult = (Boolean) eval.evaluate(bindings);
						if (!evalResult) {
							entryDirty = true;
						}
						evalResults.add(evalResult);
					}

					if (entryDirty) {
						m.addDirtyIndex(rowIndex, evalResults);
					}
				}
				return false;
			}

			@Override
			public void start(Project project) {
			}

			@Override
			public void end(Project project) {
				// TODO: update OverlayModel with new metric
				for (Metric m : metrics) {
					m.setMeasure(MetricUtils.determineQuality(bindings, m));
				}
				project.history.addEntry(new HistoryEntry(historyEntryID, project, getBriefDescription(project),
						EvaluateMetricsOperation.this, new MetricChange()));
			}
		}.init(column.getCellIndex(), bindings, historyEntryID, metrics);
	}

	@Override
	protected String createDescription(Column column,
			List<CellChange> cellChanges) {
		// TODO Auto-generated method stub
		return null;
	}
}
