package com.google.refine.metricsExtension.expr;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;

public class Uniqueness implements SpanningMetricFunction {

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
	public Evaluable getEvaluable() throws ParsingException {
		return MetaParser.parse("uniqueness(\"ID\")");
	}

	@Override
	public String getParams() {
		return "";
	}

}
