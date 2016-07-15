package com.google.refine.metricsExtension.expr.metrics.singleColumn;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;

public class Plausibility implements SingleColumnMetricFunction {

	private static final List<String> defaultParams = Arrays.asList(new String[] {"robust"});
	
	@SuppressWarnings("unchecked")
	@Override
	public Object call(Properties bindings, Object[] args) {
		if(args.length >= 1) {
			Object val = args[0];
			String evalMode = "robust";
			if(args.length >= 2) {
				String modeParsed = (String) args[1];
				if(!Arrays.asList(new String[]{"robust", "standard"}).contains(modeParsed)) {
					return new EvalError("Unknown evaluation mode " + modeParsed);
				}
				evalMode = modeParsed;
			}
			Project model = (Project) bindings.get("project");
			DescriptiveStatistics stats;
			Double sIQR;
			if(bindings.containsKey("stats")) {
				stats = (DescriptiveStatistics) bindings.get("stats");
				sIQR = (Double) bindings.get("siqr");
			} else if (bindings.containsKey("statsList")) {
				int columnIndex = model.columnModel.getColumnIndexByName((String) bindings.get("columnName"));
				stats = ((List<DescriptiveStatistics>) bindings.get("statsList")).get(columnIndex);
				sIQR = ((List<Double>) bindings.get("siqrList")).get(columnIndex);
			} else {
				return new EvalError("Statistics could not be determined");
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
				Double median = stats.apply(new Median());
				
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
		return "Perform statistical plausibilization. Plausible values can be compared to robust/standard statistics measures";
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
		return "value, evaluation mode (optional)";
	}
}
