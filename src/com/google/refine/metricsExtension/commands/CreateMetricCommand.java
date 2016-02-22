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
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.metricsExtension.util.MetricUtils;
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
		if (columnNames.length == 1) {
			metricsOverlayModel.addMetric(columnNames[0], new Metric(metricName, description, dataType));
		} else if (columnNames.length == 2) {
			try {
				metricsOverlayModel.addSpanningMetric(new SpanningMetric(
						metricName,
						description,
						RegisteredSpanningMetrics.valueOf(MetricUtils.decapitalize(metricName)).evaluable(columnNames[0], columnNames[1]), 
						Arrays.asList(columnNames)));
			} catch (ParsingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ProjectManager.singleton.ensureProjectSaved(project.id);
	}

}
