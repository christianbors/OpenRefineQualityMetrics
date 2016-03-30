package com.google.refine.metricsExtension.expr;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.grel.Function;

public class DateInterval implements Function {

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
        writer.object();
        writer.key("description"); writer.value("Determine if an interval is negative");
        writer.key("params"); writer.value("date start, date end, int value, string timeunit");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
	}
	
	@Override
	public Object call(Properties bindings, Object[] args) {
		if (args.length >= 2) {
			Object valFrom = args[0];
			Object valTo = args[1];
			if (valFrom.toString().isEmpty() || valFrom.toString().isEmpty()) {
				return false;
			}
			int isAfter;
			if (valFrom instanceof Date && valTo instanceof Date) {
				isAfter = ((Date) valFrom).compareTo((Date) valTo);
			}
			else if (valFrom instanceof Calendar && valTo instanceof Calendar) {
				isAfter = ((Calendar) valFrom).compareTo((Calendar) valTo);
			} else {
				return false;
			}
			return isAfter < 0;
		}
		return false;
	}
}
