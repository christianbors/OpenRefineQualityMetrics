package com.google.refine.metricsExtension.expr;

import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;

public class DateInterval implements Function {

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
        writer.object();
        writer.key("description"); writer.value("Determine if an interval is negative");
        writer.key("params"); writer.value("date start, date end");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
	}
	
	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length >= 2) {
			Object valFrom = args[0];
			Object valTo = args[1];
			if ((valFrom.toString().isEmpty() || valFrom.toString().isEmpty()) ||
					(!(valFrom instanceof Date) || !(valTo instanceof Date))) {
				return false;
			}
			int isAfter = ((Date) valFrom).compareTo((Date) valTo);
			return isAfter < 0;
		}
		return false;
	}
}
