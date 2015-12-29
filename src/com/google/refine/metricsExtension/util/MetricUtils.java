package com.google.refine.metricsExtension.util;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.google.refine.expr.Binder;
import com.google.refine.expr.CellTuple;
import com.google.refine.expr.WrappedRow;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class MetricUtils {

	static final protected Set<Binder> s_binders = new HashSet<Binder>();

	static public void registerBinder(Binder binder) {
		s_binders.add(binder);
	}

	static public void bind(Properties bindings, Row row, int rowIndex, String columnName, Cell cell, List<Metric> metrics) {
		Project project = (Project) bindings.get("project");

		bindings.put("rowIndex", rowIndex);
		bindings.put("row", new WrappedRow(project, rowIndex, row));
		bindings.put("cells", new CellTuple(project, row));

		if (columnName != null) {
			bindings.put("columnName", columnName);
		}

		for (Binder binder : s_binders) {
			binder.bind(bindings, row, rowIndex, columnName, cell);
		}
	}

	static public float determineQuality(Properties bindings, Metric metric) {
		Project project = (Project) bindings.get("project");
		long rowSize = project.rows.size();
		if (!metric.getDirtyIndices().isEmpty()) {
			return rowSize <= 0 ? 0 : rowSize / metric.getDirtyIndices().size();
		} else {
			return 1;
		}
	}
}
