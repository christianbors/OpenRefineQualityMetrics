package com.google.refine.metricsExtension.expr;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.grel.Function;

public class Uniqueness implements Function {

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.key("description"); writer.value("Returns evaluation of column row duplicates");
        writer.key("params"); writer.value("array a");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
	}

	@Override
	public Object call(Properties bindings, Object[] args) {
		// TODO Auto-generated method stub
		return null;
	}

}
