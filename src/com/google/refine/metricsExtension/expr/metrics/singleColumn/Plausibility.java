package com.google.refine.metricsExtension.expr.metrics.singleColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.util.StatisticsUtils;
import com.google.refine.model.Project;

public class Plausibility implements SingleColumnMetricFunction {

	private static final List<String> defaultParams = Arrays.asList(new String[] {"global", "robust"});
	private String comparisonMode = "global";
	private String evalMode = "robust";
	
	@SuppressWarnings("unchecked")
	@Override
	public Object call(Properties bindings, Object[] args) {
		DescriptiveStatistics stats;
		Double median;
		Double sIQR;
		if(args.length >= 1) {
			Object val = args[0];
			if(args.length >= 3) {
				String comparisonParsed = (String) args[1];
				if(!Arrays.asList(new String[]{"global", "progressive"}).contains(comparisonParsed)) {
					return new EvalError("Unknown comparison mode " + comparisonParsed);
				}
				comparisonMode = comparisonParsed;
				String evalParsed = (String) args[2];
				if(!Arrays.asList(new String[]{"robust", "standard"}).contains(evalParsed)) {
					return new EvalError("Unknown evaluation mode " + evalParsed);
				}
				evalMode = evalParsed;
			}
			if (!bindings.containsKey("stats") || comparisonMode.equals("progressive")) {
				Project project = (Project) bindings.get("project");
				String column = (String) bindings.get("columnName");
				int cellIndex = project.columnModel.getColumnIndexByName(column);
				Engine engine = new Engine(project);
				FilteredRows filteredRows = engine.getAllFilteredRows();
				stats = new DescriptiveStatistics();
				List<Float> values = new ArrayList<Float>();
				try {
					if(comparisonMode.equals("progressive")) {
						int endIdx = (int) bindings.get("rowIndex")-1;
						if(endIdx < 9) {
							endIdx = 9;
						}
						int startIdx = endIdx - 9;
						filteredRows.accept(project, StatisticsUtils.createAggregateRowVisitor(project, cellIndex, stats, values, startIdx, endIdx));
					} else {
						filteredRows.accept(project, StatisticsUtils.createAggregateRowVisitor(project, cellIndex, stats, values));
						bindings.put("stats", stats);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

//				DescriptiveStatistics madStats = new DescriptiveStatistics();
//				for (double entry : stats.getValues()) {
//					madStats.addValue(Math.abs(entry - median));
//				}
				// statsColsList.add(stats);
				// Double mad = madStats.apply(new Median());
				// Double iqr = stats.getPercentile(75) -
				// stats.getPercentile(25);
				// Double sIQR = iqr/1.35f;
				// madColsList.add(mad);
				// iqrColsList.add(iqr);
				// sIQRColsList.add(sIQR);

			} else {
				stats = (DescriptiveStatistics) bindings.get("stats");
				sIQR = (Double) bindings.get("siqr");
			}
			
			float fVal = 0f;
			if (val instanceof Long) {
				fVal = ((Long) val).floatValue();
			} else if (val instanceof Double) {
				fVal = ((Double) val).floatValue();
			} else if (val instanceof Float) {
				fVal = (Float) val;
			} else {
				return new EvalError("No numeric column");
			}
			
			if("robust".equals(evalMode)) {
				median = stats.apply(new Median());

				DescriptiveStatistics madStats = new DescriptiveStatistics();
				for(double entry : stats.getValues()) {
					madStats.addValue(Math.abs(entry - median));
				}
				Double mad = madStats.apply(new Median());
				Double iqr = stats.getPercentile(75) - stats.getPercentile(25);
				Double smad = mad/0.675;
				sIQR = iqr/1.35f;
				
				if (fVal > (median + 2 * sIQR) || 
						(float) fVal < (median - 2 * sIQR)) {
					return false;
				} else {
					return true;
				}
			} else {
				if (fVal < (stats.getMean() + 2 * stats.getStandardDeviation()) && 
						(float) fVal > (stats.getMean() - 2 * stats.getStandardDeviation())) {
					return true;
				} else {
					return false;
				}
			}
		} else {
			return new EvalError("Statistics could not be determined");
		}
	}
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
		writer.key("description"); writer.value(getDescription());
		writer.key("params"); writer.value(getParams());
		writer.key("returns"); writer.value("boolean that informs if value is plausible");
		writer.key("defaultParams"); writer.value("robust");
		writer.endObject();
	}

	@Override
	public String getDescription() {
		return "Perform statistical plausibilization. The plausibility can be compared progressively (based on the last 10 values), or globally. "
				+ "Plausible values can be compared to robust/standard statistics measures";
	}

	@Override
	public Evaluable getEvaluable(String[] params) throws ParsingException {
		String eval = "plausibility(value";
		Iterator<String> paramIt;
		if (params != null) {
			paramIt = Arrays.asList(params).iterator();
		} else {
			paramIt = defaultParams.iterator();
		}
		while(paramIt.hasNext()) {
			eval += ", \"" + paramIt.next() + "\"";
		}
		eval += ")";
		return MetaParser.parse(eval);
	}

	@Override
	public String getParams() {
		return "value, comparisons mode (optional, default: global), statistics type (optional, default: robust)";
	}
}
