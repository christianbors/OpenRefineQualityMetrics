package com.google.refine.metricsExtension.expr.metrics.singleColumn;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;

public class Validity implements SingleColumnMetricFunction {
	
	private static final List<String> defaultParams = Arrays.asList(new String[] {"string"});
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
        writer.object();
        writer.key("description"); writer.value(getDescription());
        writer.key("params"); writer.value(getParams());
        writer.key("returns"); writer.value("boolean");
        writer.key("defaultParams"); writer.value("string");
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

	@Override
	public String getDescription() {
		return "Evaluate validity of a value with respect to the column data type";
	}

	@Override
	public Evaluable getEvaluable(String[] params) throws ParsingException {
		String eval = "validity(value";
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
		return "string s, string type";
	}

}
