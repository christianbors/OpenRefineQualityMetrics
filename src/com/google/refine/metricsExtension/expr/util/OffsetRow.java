package com.google.refine.metricsExtension.expr.util;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.grel.Function;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class OffsetRow implements Function {

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
		writer.key("description"); writer.value("Offset row by one");
		writer.key("params"); writer.value("");
		writer.key("returns"); writer.value("Cell value from next row");
		writer.endObject();
	}

	@Override
	public Object call(Properties bindings, Object[] args) {
		Project project = (Project) bindings.get("project");
		
		int index = (int) bindings.get("rowIndex");
		String columnName = (String) bindings.get("columnName");
		Row next = project.rows.get(index+1);
		
		return next.getCellValue(project.columnModel.getColumnIndexByName(columnName));
	}

}
