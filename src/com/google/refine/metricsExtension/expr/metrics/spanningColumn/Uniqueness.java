package com.google.refine.metricsExtension.expr.metrics.spanningColumn;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;

public class Uniqueness extends SpanningColumnMetricFunction {

	@Override
	public Object call(Properties bindings, Object[] args) {
		return true;
	}

	@Override
	public String getDefaultParams() {
		return "";
	}

	@Override
	public String getDescription() {
		return "Determine duplicate rows based on the specified key columns that need to be different.";
	}

	@Override
	public String getEvaluable(String[] columns, String[] params) throws ParsingException {
		String eval = "uniqueness(";
		Iterator<String> it;
		if (columns != null) {
			it = Arrays.asList(columns).iterator();
		} else {
			it = Arrays.asList("ID").iterator();
		}
		while(it.hasNext()) {
			eval += "\"" + it.next() + "\"";
			if(it.hasNext()) {
				eval += ", ";
			}
		}
		if (params != null) {
			eval += ", ";
			it = Arrays.asList(params).iterator();
			while (it.hasNext()) {
				eval += "\"" + it.next() + "\"";
				if (it.hasNext()) {
					eval += ", ";
				}
			}
		}
		eval += ")";
		return eval;
	}

	@Override
	public String getParams() {
		return "list of column names";
	}

}
