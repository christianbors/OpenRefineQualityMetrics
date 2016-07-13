package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.expr.MetricFunction;
import com.google.refine.metricsExtension.expr.SpanningMetricFunction;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.Metric.Concatenation;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.metricsExtension.util.MetricUtils;
import com.google.refine.metricsExtension.util.MetricUtils.RegisteredMetrics;
import com.google.refine.metricsExtension.util.MetricUtils.RegisteredSpanningMetrics;
import com.google.refine.model.Project;

public class CreateMetricCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String[] columnNames = request.getParameterValues("columns[]");
		String metricName = request.getParameter("metric");
		String[] parameters = request.getParameterValues("parameters[]");
		String dataType = request.getParameter("dataType");
		Project project = getProject(request);
		
		MetricsOverlayModel metricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		logger.info("spanning metrics {}", metricsOverlayModel.getSpanMetricsList().size());
		try {
			if (columnNames.length == 1) {
				MetricFunction metricFun = (MetricFunction) ControlFunctionRegistry.getFunction(metricName);
				Metric m = new Metric(metricName, metricFun.getDescription(), dataType);
				m.addEvalTuple(metricFun.getEvaluable(parameters), "", false);
				
				metricsOverlayModel.addMetric(columnNames[0], m);
			} else if (columnNames.length >= 2) {
				SpanningMetricFunction metricFun = (SpanningMetricFunction) ControlFunctionRegistry.getFunction(metricName);
					SpanningMetric newSpanningMetric = new SpanningMetric(
							metricName,
							metricFun.getDescription(),
							dataType,
							Concatenation.OR,
							Arrays.asList(columnNames));
					newSpanningMetric.addSpanningEvalTuple(metricFun.getEvaluable(columnNames, parameters), "", false);
					
					metricsOverlayModel.addSpanningMetric(newSpanningMetric);
					logger.info("spanning metrics {}", metricsOverlayModel.getSpanMetricsList().size());
			}
		} catch (ParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ProjectManager.singleton.ensureProjectSaved(project.id);
		try {
			respondJSON(response, metricsOverlayModel);
		} catch (JSONException e) {
			respondException(response, e);
		}
	}
}
