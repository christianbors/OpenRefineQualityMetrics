package com.google.refine.metricsExtension.commands;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.operations.EvaluateMetricsOperation;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;

public class EvaluateMetricsCommand extends EngineDependentCommand {

	@Override
	protected AbstractOperation createOperation(Project project,
			HttpServletRequest request, JSONObject engineConfig)
			throws Exception {
		
		String columnName = request.getParameter("columnName");
		MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		
		return new EvaluateMetricsOperation(engineConfig, overlayModel, columnName);
	}

}
