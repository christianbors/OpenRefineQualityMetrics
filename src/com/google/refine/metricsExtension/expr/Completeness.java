
package com.google.refine.metricsExtension.expr;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;


public class Completeness implements MetricFunction {

	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length >= 1) {
			Object o1 = args[0];
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
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
        writer.object();
        writer.key("description"); writer.value("Evaluates if an entry is empty");
        writer.key("params"); writer.value("object o, missing value indicator (optional)");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
	}

}
