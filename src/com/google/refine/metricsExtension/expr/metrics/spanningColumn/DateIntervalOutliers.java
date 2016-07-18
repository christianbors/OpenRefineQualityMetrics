package com.google.refine.metricsExtension.expr.metrics.spanningColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.expr.CellTuple;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.expr.WrappedCell;
import com.google.refine.metricsExtension.util.StatisticsUtils;
import com.google.refine.model.Project;

public class DateIntervalOutliers implements SpanningColumnMetricFunction {

	private static final List<String> defaultParams = Arrays.asList(new String[] {"robust", "seconds"});
	
	@SuppressWarnings("unchecked")
	@Override
	public Object call(Properties bindings, Object[] args) {
		String evalMode = "standard";
		String unit = "seconds";
		int rowIndex = (int) bindings.get("rowIndex");
		DescriptiveStatistics stats;
		List<Long> values;
		
		if (args.length >= 2) {
			WrappedCell valFrom = (WrappedCell) ((CellTuple) bindings.get("cells")).getField((String) args[0], bindings);
			WrappedCell valTo = (WrappedCell) ((CellTuple) bindings.get("cells")).getField((String) args[1], bindings);
			
			if (args.length >= 3) {
				String evalParsed = (String) args[2];
				if (!Arrays.asList(new String[] { "robust", "standard" }).contains(evalParsed)) {
					return new EvalError("Unknown evaluation mode " + evalParsed);
				}
				evalMode = evalParsed;
			}
			if (args.length >= 4) {
				unit = (String) args[3];
			}
			if (!bindings.containsKey("intervalStats")) {
				Project project = (Project) bindings.get("project");
				Engine engine = new Engine(project);
				FilteredRows filteredRows = engine.getAllFilteredRows();
				stats = new DescriptiveStatistics();
				int from = project.columnModel.getColumnIndexByName((String) args[0]);
				int to = project.columnModel.getColumnIndexByName((String) args[1]);
				values = new ArrayList<Long>();
				try {
					filteredRows.accept(project, StatisticsUtils.createAggregateSpanningRowVisitor(project, stats, values, from, to, unit));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bindings.put("intervalStats", stats);
				bindings.put("intervalValues", values);
			} else {
				stats = (DescriptiveStatistics) bindings.get("intervalStats");
				values = (List<Long>) bindings.get("intervalValues");
			}
			if (evalMode.equals("robust")) {
				Double median = stats.apply(new Median());
				Double iqr = stats.getPercentile(75) - stats.getPercentile(25);
				Double sIQR = iqr/1.35f;
				
				try {
					long value = StatisticsUtils.getIntervalValue(((Date) valFrom.cell.value).getTime(), ((Date) valTo.cell.value).getTime(), unit);
					if (value > (median + 2 * sIQR) || 
							(float) value < (median - 2 * sIQR)) {
						return false;
					} else {
						return true;
					}
				} catch (Exception e) {
					return false;
				}
			}
		}
		return new EvalError("invalid number of parameters");
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
        writer.key("description"); writer.value(getDescription());
        writer.key("params"); writer.value(getParams());
        writer.key("returns"); writer.value("boolean");
        writer.key("defaultParams"); writer.value(defaultParams.toString());
        writer.endObject();
	}

	@Override
	public String getDescription() {
		return "Determine interval length outliers";
	}

	@Override
	public Evaluable getEvaluable(String[] columns, String[] params)
			throws ParsingException {
		String eval = "dateIntervalOutliers(";
		Iterator<String> it = Arrays.asList(columns).iterator();
		while(it.hasNext()) {
			eval += "\"" + it.next() + "\"";
			if(it.hasNext()) {
				eval += ", ";
			}
		}
		if (params != null) {
			it = Arrays.asList(params).iterator();
		} else {
			it = defaultParams.iterator();
		}
		eval += ", ";
		while (it.hasNext()) {
			eval += "\"" + it.next() + "\"";
			if (it.hasNext()) {
				eval += ", ";
			}
		}
		eval += ")";
		return MetaParser.parse(eval);
	}

	@Override
	public String getParams() {
		return "start column, end column, robustness (optional, default: robust)";
	}

}
