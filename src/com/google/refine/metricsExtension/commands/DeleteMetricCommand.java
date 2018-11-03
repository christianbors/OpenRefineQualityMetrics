package com.google.refine.metricsExtension.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.model.Project;

public class DeleteMetricCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Project project = getProject(request);
		String[] columnNames = request.getParameterValues("column[]");
		String columnName = request.getParameter("column");
		String metricName = request.getParameter("metricName");
		
		MetricsOverlayModel metricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get(MetricsOverlayModel.OVERLAY_NAME);
		if (columnName != null) {
			metricsOverlayModel.deleteMetric(columnName, metricName);
		} else {
			metricsOverlayModel.deleteSpanningMetric(metricName, columnNames);
		}
		try {
			ProjectManager.singleton.ensureProjectSaved(project.id);
			respondJSON(response, metricsOverlayModel);
		} catch (JSONException e) {
			respondException(response, e);
		}
	}

}
