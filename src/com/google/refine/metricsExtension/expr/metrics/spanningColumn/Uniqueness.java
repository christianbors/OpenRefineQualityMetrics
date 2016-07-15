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

public class Uniqueness implements SpanningColumnMetricFunction {

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
        writer.key("description"); writer.value(getDescription());
        writer.key("params"); writer.value(getParams());
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
	}

	@Override
	public Object call(Properties bindings, Object[] args) {
		return true;
	}

	@Override
	public String getDescription() {
		return "uniqueness placeholder";
	}

	@Override
	public Evaluable getEvaluable(String[] columns, String[] params) throws ParsingException {
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
		return MetaParser.parse(eval);
	}

	@Override
	public String getParams() {
		return "";
	}

}
