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

	protected EvaluateMetricsOperation(JSONObject engineConfig,
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

	public class CalculateMetricsProcess extends LongRunningProcess implements
			Runnable {

		private Project project;
		private AbstractOperation parentOperation;
		private JSONObject engineConfig;
		private long historyEntryId;
		private MetricsOverlayModel model;

		public CalculateMetricsProcess(Project project,
				MetricsOverlayModel metricsOverlayModel, Properties options,
				AbstractOperation parentOperation, String briefDescription,
				JSONObject engineConfig) {
			super(briefDescription);
			this.project = project;
			this.parentOperation = parentOperation;
			this.engineConfig = engineConfig;
			this.historyEntryId = HistoryEntry.allocateID();
			this.model = metricsOverlayModel;
		}

		protected void iterateColumns() {
			// Column column = project.columnModel.getColumnByName(model.);
			Engine engine = new Engine(project);
			// engine.initializeFromJSON(_engineConfig);
			//
			// Column column =
			// project.columnModel.getColumnByName(_baseColumnName);
			// if (column == null) {
			// throw new Exception("No column named " + _baseColumnName);
			// }
			//

			FilteredRows filteredRows = engine.getAllFilteredRows();
			filteredRows.accept(project, new RowVisitor() {

				public RowVisitor init() {
					return this;
				}

				@Override
				public boolean visit(Project project, int rowIndex, Row row) {
					for (MetricsColumn metricsColumn : model
							.getMetricsColumnList()) {
						int cellIndex = metricsColumn.getColumn()
								.getCellIndex();
						Cell cell = row.getCell(cellIndex);
						for (Metric m : metricsColumn.getMetrics()) {
							// evaluate metrics, you can obtain them from
							m.getEvaluables();
						}
					}
					return false;
				}

				@Override
				public void start(Project project) {
					// TODO Auto-generated method stub

				}

				@Override
				public void end(Project project) {
					// TODO Auto-generated method stub

				}
			}.init());
		}

		@Override
		public void run() {
			// TODO
			// iterate columns, get metrics to calculate
			// iterate rows, evaluate metrics
			// create list of dirty entries for each metric
			// calculate metric value
			if (!_canceled) {
				project.history.addEntry(new HistoryEntry(historyEntryId,
						project, _description, parentOperation,
						new MetricChange()));
				project.processManager.onDoneProcess(this);
				// TODO add History
			}
		}

		@Override
		protected Runnable getRunnable() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@Override
	protected RowVisitor createRowVisitor(Project project,
			List<CellChange> cellChanges, long historyEntryID) throws Exception {
		Column column = project.columnModel.getColumnByName(_columnName);

		// Evaluable eval = MetaParser.parse(_expression);
		Properties bindings = ExpressionUtils.createBindings(project);
		//TODO: metric bindings / getting certain infos into the bindings to perform a better evaluation
//		Properties bindings = MetricUtils.createBindings(project);

//		Map<String, Serializable> fromTo = new HashMap<String, Serializable>();
//		Serializable fromBlankTo = null;
//		Serializable fromErrorTo = null;

		return new RowVisitor() {
			private int cellIndex;
			private long historyEntryID;
			private Properties bindings;

			public RowVisitor init(int cellIndex, Properties bindings,
					long historyEntryID) {
				this.cellIndex = cellIndex;
				this.bindings = bindings;
				this.historyEntryID = historyEntryID;
				return this;
			}

			@Override
			public boolean visit(Project project, int rowIndex, Row row) {
				Cell cell = row.getCell(cellIndex);

				MetricsColumn col = model.getMetricsColumnList().get(cellIndex);

				for (Metric m : col.getMetrics()) {
					List<Evaluable> metricEvaluables = m.getEvaluables();

					ExpressionUtils.bind(bindings, row, rowIndex, _columnName,
							cell);

					List<Boolean> evalResults = new ArrayList<Boolean>(
							metricEvaluables.size());
					boolean entryDirty = false;

					for (int evalIndex = 0; evalIndex < metricEvaluables.size(); ++evalIndex) {
						Evaluable eval = metricEvaluables.get(evalIndex);
						boolean evalResult = (Boolean) eval.evaluate(bindings);
						if (!evalResult) {
							entryDirty = true;
						}
						evalResults.set(evalIndex, evalResult);
					}

					if (entryDirty) {
						m.addDirtyIndex(rowIndex, evalResults);
					}
				}
				return false;
			}

			@Override
			public void start(Project project) {
				// TODO Auto-generated method stub

			}

			@Override
			public void end(Project project) {
				// TODO: update OverlayModel with new metric
				project.history.addEntry(new HistoryEntry(historyEntryID, project, getBriefDescription(project),
						EvaluateMetricsOperation.this, new MetricChange()));
			}
		}.init(column.getCellIndex(), bindings, historyEntryID);
	}

	@Override
	protected String createDescription(Column column,
			List<CellChange> cellChanges) {
		// TODO Auto-generated method stub
		return null;
	}
}
