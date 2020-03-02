
package com.google.refine.metricsExtension.expr.metrics.singleColumn;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;


public class Completeness extends SingleColumnMetricFunction {

	private static final List<String> defaultParams = Arrays.asList(new String[] {});
	
	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length >= 1) {
			Object o1 = args[0];
			if(o1 == null) {
				return false;
			}
			if (o1 instanceof Long || o1 instanceof Double || o1 instanceof Float) {
				return o1 != null;
			} else if (o1 instanceof String) {
				boolean incomplete = ((String) o1).isEmpty();
				if(args.length == 2) {
					Object nullValue = args[1];
					if (!incomplete) {
						incomplete = o1.equals(nullValue);
					}
				}
				return !incomplete; // return the inverted value since the completeness returns an error if the value is empty
			}
        }
        
//		Object value = bindings.get("value");
//		acceptZeroes = (boolean) bindings.get("acceptZeroes");
		
        //TODO: add function to check date
		return new EvalError("ToString accepts an object and an optional second argument containing a date format string");
	}

	@Override
	public String getDefaultParams() {
		return "value";
	}

	@Override
	public String getDescription() {
		return "Evaluates if an entry is empty";
	}

	@Override
	public String getEvaluable(String[] params) throws ParsingException {
		String eval = "completeness(value";
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
		return eval;
	}

	@Override
	public String getParams() {
		return "object o, missing value indicator (optional)";
	}

}
