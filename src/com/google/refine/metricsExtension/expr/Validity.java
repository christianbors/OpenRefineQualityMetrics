package com.google.refine.metricsExtension.expr;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.grel.Function;

public class Validity implements Function {

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
        writer.object();
        writer.key("description"); writer.value("Evaluate validity of a value with respect to the column data type");
        writer.key("params"); writer.value("string s, string type");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
	}

	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length >= 2) {
			Object o1 = args[0];
			Object type = args[1];
			if (o1.toString().isEmpty()) {
				return true;
			}
			if (type.toString().equals("string")) {
				return o1 instanceof String;
			} else if (type.toString().equals("number")) {
				return (o1 instanceof Long 
	                    || o1 instanceof Double
	                    || o1 instanceof Float); 
			} else if (type.toString().equals("date")) {
				return o1 instanceof Date || o1 instanceof Calendar;
			}
		}
		return true;
	}

}
