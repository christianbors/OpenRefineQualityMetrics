package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;
import com.google.refine.model.Project;

public class DuplicateMetricCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Project project = getProject(request);
		MetricsOverlayModel model = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		String column = request.getParameter("column");
		String targetCol = request.getParameter("targetColumn");
		String metricName = request.getParameter("metricName");
		
		Map<String, Metric> columnMetrics = model.getMetricsForColumn(column);
		Metric toBeDuplicated = columnMetrics.get(metricName);
		Metric dupMetric = new Metric(toBeDuplicated.getName(), 
				toBeDuplicated.getDescription(), 
				toBeDuplicated.getDataType(), 
				toBeDuplicated.getConcat());
		for(EvalTuple et : toBeDuplicated.getEvalTuples()) {
			dupMetric.addEvalTuple(et);
		}
		model.addMetric(targetCol, dupMetric);
	}

}
