package com.google.refine.metricsExtension.expr.metrics.singleColumn;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;

import java.time.OffsetDateTime;
import java.util.*;

public class Validity extends SingleColumnMetricFunction {

	@JsonProperty
	private static final List<String> defaultParams = Arrays.asList(new String[] {"string"});

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
				return o1 instanceof Date || o1 instanceof Calendar || o1 instanceof OffsetDateTime;
			} else {
				return new EvalError("Error at parameter 2: Type string not recognized \"" + type + "\"");
			}
		}
		return true;
	}

	@Override
	public String getDefaultParams() {
		return defaultParams.get(0);
	}

	@Override
	public String getDescription() {
		return "Evaluate validity of a value with respect to the column data type";
	}

	@Override
	public String getEvaluable(String[] params) throws ParsingException {
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
		return eval;
	}

	@Override
	public String getParams() {
		return "string s, string type";
	}

}
