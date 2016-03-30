package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.model.Metric;
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
		String[] columnNames = request.getParameterValues("columns[]");
		String metricName = request.getParameter("metric");
		String description = request.getParameter("description");
		String dataType = request.getParameter("dataType");
		Project project = getProject(request);
		
		MetricsOverlayModel metricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		try {
			if (columnNames.length == 1) {
				Metric m = new Metric(metricName, description, dataType);
				m.addEvalTuple(RegisteredMetrics.valueOf(metricName).evaluable(), "", false);
				metricsOverlayModel.addMetric(columnNames[0], m);
			} else if (columnNames.length == 2) {
					SpanningMetric newSpanningMetric = new SpanningMetric(
							metricName,
							description, 
							Arrays.asList(columnNames));
					newSpanningMetric.addSpanningEvalTuple(RegisteredSpanningMetrics.valueOf(MetricUtils.decapitalize(metricName)).evaluable(columnNames[0], columnNames[1]), "", false);
					metricsOverlayModel.addSpanningMetric(newSpanningMetric);
			}
		} catch (ParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ProjectManager.singleton.ensureProjectSaved(project.id);
	}

}
