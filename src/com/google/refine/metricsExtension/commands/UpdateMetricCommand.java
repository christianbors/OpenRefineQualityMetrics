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
		
		// {metric[name]=completeness, metric[datatype]=unknown, metric[evalTuples][0][disabled]=false, metric[description]=Evaluates if an entry is empty, 
		// metric[concat]=AND, project=2374114907925, metric[evalTuples][0][comment]=, metric[evalTuples][0][evaluable]=completeness(value), 
		// metric[columnName]=Longitude, metric[measure]=0.10425717}
		String metricNameString = request.getParameter("metric[name]");
		String metricDescriptionString = request.getParameter("metric[description]");
		String metricDatatypeString = request.getParameter("metric[datatype]");
		String metricConcatenation = request.getParameter("metric[concat]");
		String column = request.getParameter("column");
		int evaluableCount = Integer.parseInt(request.getParameter("metricEvalCount"));
		
		Metric toBeEdited;
		if (column != null) {
			Map<String, Metric> columnMetrics = model.getMetricsForColumn(column);
			toBeEdited = columnMetrics.get(metricNameString);
			columnMetrics.remove(metricNameString);
		} else {
			if(metricNameString.equals("uniqueness")) {
				toBeEdited = model.getUniqueness();
			} else {
				toBeEdited = null;
				for(SpanningMetric spanMetric : model.getSpanMetricsList()) {
					if (spanMetric.getName().equals(metricNameString)) {
						toBeEdited = spanMetric;
						String comment = request.getParameter("metric[spanningEvaluable][comment]");
						String evaluable = request.getParameter("metric[spanningEvaluable][evaluable]");
						boolean disabled = Boolean.parseBoolean(request.getParameter("metric[spanningEvaluable][disabled]"));
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
			String comment = request.getParameter("metric[evalTuples][" + i + "][comment]");
			String evaluable = request.getParameter("metric[evalTuples][" + i + "][evaluable]");
			boolean disabled = Boolean.parseBoolean(request.getParameter("metric[evalTuples][" + i + "][disabled]"));
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
			Map<String, Metric> columnMetrics = model.getMetricsForColumn(column);
			columnMetrics.put(metricNameString, toBeEdited);
		}
		try {
			respondJSON(response, toBeEdited);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
