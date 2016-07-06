package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.Metric.Concatenation;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.model.Project;

public class UpdateMetricCommand extends Command {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		internalRespond(request, response);
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		internalRespond(request, response);
	}
	
	protected void internalRespond(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		Project project = getProject(request);
		MetricsOverlayModel model = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		
		String metricNameString = request.getParameter("metricName");
		String metricDescriptionString = request.getParameter("metricDescription");
		String metricDatatypeString = request.getParameter("metricDatatype");
		String metricConcatenation = request.getParameter("concat");
		String column = request.getParameter("column");
		int metricIndex = Integer.parseInt(request.getParameter("metricIndex"));
		int evaluableCount = Integer.parseInt(request.getParameter("metricEvalCount"));
		
		Metric toBeEdited;
		if (column != null) {
			List<Metric> columnMetrics = model.getMetricsForColumn(column);
			toBeEdited = columnMetrics.get(metricIndex);
			columnMetrics.remove(metricIndex);
		} else {
			if(metricNameString.equals("uniqueness")) {
				toBeEdited = model.getUniqueness();
			} else {
				toBeEdited = null;
				for(SpanningMetric spanMetric : model.getSpanMetricsList()) {
					if (spanMetric.getName().equals(metricNameString)) {
						toBeEdited = spanMetric;
						String comment = request.getParameter("metricSpanningEvalTuple[comment]");
						String evaluable = request.getParameter("metricSpanningEvalTuple[evaluable]");
						boolean disabled = Boolean.parseBoolean(request.getParameter("metricSpanningEvalTuple[disabled]"));
						try {
							((SpanningMetric) toBeEdited).addSpanningEvalTuple(MetaParser.parse(evaluable), comment, disabled);
						} catch (ParsingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					}
				}
			}
		}
		
		toBeEdited.setDataType(metricDatatypeString);
		toBeEdited.setName(metricNameString);
		if (!metricDescriptionString.isEmpty()) {
			toBeEdited.setDescription(metricDescriptionString);
		}
		toBeEdited.setConcat(Concatenation.valueOf(metricConcatenation));
		toBeEdited.getEvalTuples().clear();
		for(int i = 0; i < evaluableCount; i++) {
			String comment = request.getParameter("metricEvalTuples[" + i + "][comment]");
			String evaluable = request.getParameter("metricEvalTuples[" + i + "][evaluable]");
			boolean disabled = Boolean.parseBoolean(request.getParameter("metricEvalTuples[" + i + "][disabled]"));
			try {
				toBeEdited.addEvalTuple(MetaParser.parse(evaluable), comment, disabled);
			} catch (ParsingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		toBeEdited.getDirtyIndices().clear();
		toBeEdited.setMeasure(0f);
		
		if (column != null) {
			List<Metric> columnMetrics = model.getMetricsForColumn(column);
			columnMetrics.add(metricIndex, toBeEdited);
		}
		try {
			respondJSON(response, toBeEdited);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
