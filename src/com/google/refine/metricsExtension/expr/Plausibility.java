package com.google.refine.metricsExtension.expr;

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

public class Plausibility implements MetricFunction {

	private static final List<String> defaultParams = Arrays.asList(new String[] {"robust"});
	
	@Override
	public Object call(Properties bindings, Object[] args) {
		if(bindings.containsKey("stats")) {
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
				
				DescriptiveStatistics stats = (DescriptiveStatistics) bindings.get("stats");
				
				float fVal = 0f;
				if (val instanceof Long) {
					fVal = ((Long) val).floatValue();
				} else if (val instanceof Double) {
					fVal = ((Double) val).floatValue();
				} else {
					fVal = (Float) val;
				}
				
				if("robust".equals(evalMode)) {
					Double median = stats.apply(new Median());
					Double sIQR = (Double) bindings.get("siqr");
					
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
			}
			return false;
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
