package com.google.refine.metricsExtension.util;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.Binder;
import com.google.refine.expr.CellTuple;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.expr.WrappedRow;
import com.google.refine.metricsExtension.expr.DateInterval;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class MetricUtils {

	static final protected Set<Binder> s_binders = new HashSet<Binder>();

	static public void registerBinder(Binder binder) {
		s_binders.add(binder);
	}
	
	public enum RegisteredMetrics {
		completeness("string") {
			public String description() {
				return "Detects missing entries for the specified column";
			}

			@Override
			public Evaluable evaluable() throws ParsingException {
				return MetaParser.parse("completeness(value)");
			}
		},
		validity("number") {
			public String description() {
				return "Validates an entry towards a specific data type";
			}

			@Override
			public Evaluable evaluable() throws ParsingException {
				return MetaParser.parse("validity(value, '" + datatype() + "')");
			}
		};
		
		private String datatype;
		
		private RegisteredMetrics(String datatype) {
			this.datatype = datatype;
		}
		
		public String datatype() {
			return datatype;
		}
		
		public void write(JSONWriter writer, Properties options)
	            throws JSONException {
	        writer.object();
	        writer.key("name").value(name());
	        writer.key("description").value(description());
	        writer.key("datatype").value(datatype());
	        writer.endObject();
	    }
		
		abstract public String description();
		abstract public Evaluable evaluable() throws ParsingException;
	}
	
	public enum RegisteredSpanningMetrics {	
		dateinterval {
			public String description() {
				return "Determine if an interval is negative";
			}

			@Override
			public Evaluable evaluable(String colFrom, String colTo) throws ParsingException {
				return MetaParser.parse("dateInterval(cells." + colFrom + ".value, cells." + colTo + ".value)");
			}
		};
		
		abstract public String description();
		abstract public Evaluable evaluable(String colFrom, String colTo) throws ParsingException;
		
		public void write(JSONWriter writer, Properties options)
	            throws JSONException {
	        writer.object();
	        writer.key("name").value(name());
	        writer.key("description").value(description());
	        writer.endObject();
	    }
	}
	/*
	 * ,
		dateInterval("date") {
			public String description() {
				return "Determine if an interval is negative";
			}

			@Override
			public String evaluable() {
				return "dateInterval()";
			}
		}
	 */

	static public void bind(Properties bindings, Row row, int rowIndex,
			String columnName, Cell cell, List<Metric> metrics) {
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
		float rowSize = project.rows.size();
		if (!metric.getDirtyIndices().isEmpty()) {
			float measure;
			if (rowSize <= 0) {
				measure = 0f;
			} else {
				measure = (rowSize - metric.getDirtyIndices().size()) / rowSize;
			}
			return measure;
		} else {
			return 1f;
		}
	}
}
