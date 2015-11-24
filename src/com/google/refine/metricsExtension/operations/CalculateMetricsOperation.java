package com.google.refine.metricsExtension.operations;

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
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;

public class CalculateMetricsOperation extends EngineDependentOperation {

	private MetricsOverlayModel model;
	private String columnName;

	protected CalculateMetricsOperation(JSONObject engineConfig,
			MetricsOverlayModel model, String columnName) {
		super(engineConfig);
		this.model = model;
		this.columnName = columnName;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		throw new JSONException("TODO");
	}

	@Override
	public Process createProcess(Project project, Properties options)
			throws Exception {
		return new CalculateMetricsProcess(project, model, options, this,
				getBriefDescription(project), getEngineConfig());
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
//			Column column = project.columnModel.getColumnByName(_baseColumnName);
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
					for (MetricsColumn metricsColumn : model.getMetricsColumnList()) {
						int cellIndex = metricsColumn.getColumn().getCellIndex();
						Cell cell = row.getCell(cellIndex);
						for (Metric m : metricsColumn.getMetrics()) {
							//evaluate metrics, you can obtain them from
							m.compute(comp)
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
}
