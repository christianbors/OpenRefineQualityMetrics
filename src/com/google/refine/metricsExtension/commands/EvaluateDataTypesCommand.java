package com.google.refine.metricsExtension.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.commands.Command;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.WrappedCell;
import com.google.refine.expr.functions.ToDate;
import com.google.refine.expr.functions.ToNumber;
import com.google.refine.expr.functions.ToString;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import org.json.JSONException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class EvaluateDataTypesCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final Project project = getProject(request);
			List<String> columns = Arrays.asList(request.getParameterValues("columns[]"));
			Properties bindings = ExpressionUtils.createBindings(project);
			Engine engine = new Engine(project);

			FilteredRows filteredRows = engine.getAllFilteredRows();
			filteredRows.accept(project, new RowVisitor() {
				private Properties bindings;
				private List<String> columns;
				private HttpServletResponse response;
				private DataTypesDist types;

				public RowVisitor init(Properties bindings, HttpServletResponse response, List<String> columns) {
					this.bindings = bindings;
					this.response = response;
					this.columns = columns;
					this.types = new DataTypesDist(columns.size(), project.rows.size());
					return this;
				}

				@Override
				public void start(Project project) {
				}

				@Override
				public boolean visit(Project project, int rowIndex, Row row) {
					for (String columnName : columns) {
						int colIdx = columns.indexOf(columnName);
						WrappedCell ct = (WrappedCell) row
								.getCellTuple(project).getField(columnName,
										bindings);
						Object[] argsDate = 
							{	"dd.MM.yyyy HH:mm:ss",
								"yyyy-MM-dd HH:mm:ss" };
//						if (ct.cell != null) {
//							argsDate[argsDate.length+1] = ct.cell.value;
//						}
						if (ct != null && ct.cell != null) {
							Object[] argsDefault = { ct.cell.value };
							Object dateValue = new ToDate()
									.call(bindings, argsDate);
							Object numericValue = new ToNumber().call(bindings,
									argsDefault);
							Object stringValue = new ToString().call(bindings,
									argsDefault);
							if ((dateValue instanceof Date)
									|| (dateValue instanceof Calendar)) {
								int[] counts = types.dataTypes.get("date/time");
								counts[colIdx]++;
								types.dataTypes.put("date/time", counts);
							} else if (numericValue instanceof Long
									|| numericValue instanceof Float
									|| numericValue instanceof Integer) {
								int[] counts = types.dataTypes.get("numeric");
								counts[colIdx]++;
								types.dataTypes.put("numeric", counts);
							} else if (stringValue instanceof String) {
								int[] counts = types.dataTypes.get("string");
								counts[colIdx]++;
								types.dataTypes.put("string", counts);
							} else {
								int[] counts = types.dataTypes.get("unknown");
								counts[colIdx]++;
								types.dataTypes.put("unknown", counts);
							}
						}
					}
					return false;
				}

				@Override
				public void end(Project project) {
					try {
						respondJSON(response, types);
					} catch (IOException | JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}.init(bindings, response, columns));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private class DataTypesDist {

		@JsonProperty("dataTypes")
		private Map<String, int[]> dataTypes;
		@JsonProperty("rowCount")
		private int rowCount;
		
		DataTypesDist(int columnSize, int rowCount) {
			this.dataTypes = new HashMap<String, int[]>();
			this.rowCount = rowCount;
			dataTypes.put("date/time", new int[columnSize]);
			dataTypes.put("numeric", new int[columnSize]);
			dataTypes.put("string", new int[columnSize]);
			dataTypes.put("unknown", new int[columnSize]);
		}
		
//		@Override
//		public void write(JSONWriter writer, Properties options)
//				throws JSONException {
//			writer.array();
//			for (Map.Entry<String, int[]> entry : dataTypes.entrySet()) {
//				writer.object();
//				writer.key("type").value(entry.getKey());
//				writer.key("val").array();
//				for(int count : entry.getValue()) {
//					writer.value(count);
//				}
//				writer.endArray().endObject();
//			}
//			writer.endArray();
//		}
	}
}
