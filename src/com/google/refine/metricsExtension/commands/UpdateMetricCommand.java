package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.model.Project;

public class UpdateMetricCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Project project = getProject(request);
		MetricsOverlayModel model = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		
		String metricNameString = request.getParameter("metricName");
		String metricDescriptionString = request.getParameter("metricDescription");
		String metricDatatypeString = request.getParameter("metricDatatype");
		String[] metricEvaluableString = request.getParameterValues("metricEvaluables[]");
		String column = request.getParameter("column");
		int metricIndex = Integer.parseInt(request.getParameter("metricIndex"));
		
		List<Metric> columnMetrics = model.getMetricsForColumn(column);
		Metric toBeEdited = columnMetrics.get(metricIndex);
		columnMetrics.remove(metricIndex);
		
		toBeEdited.setDataType(metricDatatypeString);
		toBeEdited.setName(metricNameString);
		if (!metricDescriptionString.isEmpty()) {
			toBeEdited.setDescription(metricDescriptionString);
		}
		toBeEdited.getEvaluables().clear();
		for (int i = 0; i < metricEvaluableString.length; ++i) {
			toBeEdited.addEvaluable(metricEvaluableString[i]);
		}
		toBeEdited.getDirtyIndices().clear();
		toBeEdited.setMeasure(0f);
		
		columnMetrics.add(metricIndex, toBeEdited);
	}

}
