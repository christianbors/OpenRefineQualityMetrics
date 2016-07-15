package com.google.refine.metricsExtension.util;

import java.util.Arrays;
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
import com.google.refine.metricsExtension.expr.metrics.spanningColumn.DateInterval;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.Metric.Concatenation;
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
		dateInterval {
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

	public static String decapitalize(String string) {
		if (string == null || string.length() == 0) {
			return string;
		}
		char c[] = string.toCharArray();
		c[0] = Character.toLowerCase(c[0]);
		return new String(c);
	}

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
				int dirtyCount = 0;
				for(Entry<Integer, List<Boolean>> resultEntry : metric.getDirtyIndices().entrySet()) {
					boolean dirty = false;
					if (metric.getConcat() == Concatenation.AND) {
						if(!resultEntry.getValue().contains(true)) {
							dirty = true;
						}
					} else if (metric.getConcat() == Concatenation.OR) {
						if(resultEntry.getValue().contains(false)) {
							dirty = true;
						}
					} else if (metric.getConcat() == Concatenation.XOR) {
						if(Arrays.asList(resultEntry.getValue()).contains(false) && 
								!Arrays.asList(resultEntry.getValue()).contains(true)) 
							dirty = true;
						if(Arrays.asList(resultEntry.getValue()).contains(true) && 
								!Arrays.asList(resultEntry.getValue()).contains(false)) 
							dirty = true;
						boolean duplicateError = false;
						for (boolean result : resultEntry.getValue()) {
							if(!result && !duplicateError) {
								duplicateError = true;
							} else if (!result && duplicateError) {
								dirty = true;
								break;
							}
						}
					}
					if (dirty) {
						++dirtyCount;
					}
				}
				measure = (rowSize - dirtyCount) / rowSize;
			}
			return measure;
		} else {
			return 1f;
		}
	}
}
