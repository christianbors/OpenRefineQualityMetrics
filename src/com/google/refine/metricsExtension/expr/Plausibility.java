package com.google.refine.metricsExtension.expr;

import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;

public class Plausibility implements Function {
		
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
				} else {
					fVal = (Float) val;
				}
				
				if("robust".equals(evalMode)) {
					Double median = stats.apply(new Median());
					Double sIQR = (Double) bindings.get("siqr");
					
					if (fVal < (median + 2 * sIQR) && 
							(float) fVal > (median - 2 * sIQR)) {
						return true;
					} else {
						return false;
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
		writer.key("description"); writer.value("Perform statistical plausibilization. Plausible values can be compared to robust/standard statistics measures");
		writer.key("params"); writer.value("value, evaluation mode (optional)");
		writer.key("returns"); writer.value("boolean that informs if value is plausible");
		writer.endObject();
	}
}
