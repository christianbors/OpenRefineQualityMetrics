
package com.google.refine.metricsExtension.expr;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricEvaluable;
import com.google.refine.util.StringUtils;


public class Completeness implements Function {

	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length >= 1) {
            Object o1 = args[0];
            if (o1 instanceof Long 
                    || o1 instanceof Double
                    || o1 instanceof Float) {
                    return o1 != null;
            } else if (o1 instanceof String) {
                return !((String) o1).isEmpty();
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
        writer.key("description"); writer.value("Evaluates if a value is null");
        writer.key("params"); writer.value("o");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
	}

}
