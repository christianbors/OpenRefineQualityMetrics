package com.google.refine.metricsExtension.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.google.refine.commands.Command;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.model.Project;

public class DeleteMetricCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Project project = getProject(request);
		String[] columnNames = request.getParameterValues("columnNames[]");
		String columnName = request.getParameter("column");
		String metricName = request.getParameter("metricName");
		
		MetricsOverlayModel metricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		if (columnName != null) {
			metricsOverlayModel.deleteMetric(columnName, metricName);
			try {
				respond(response, "200 OK", "Metric " + metricName + " for column " + columnName + " deleted");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			metricsOverlayModel.deleteSpanningMetric(metricName, columnNames);
			try {
				String cols = "";
				for(int i = 0; i < columnNames.length; ++i) {
					cols += columnNames[i];
					if(i+1 < columnNames.length) cols += ", ";
				}
				respond(response, "200 OK", "Metric " + metricName + " for columns " + cols + " deleted");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}
