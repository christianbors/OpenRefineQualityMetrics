package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.operations.PersistMetricsOperation;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;

public class PersistMetricsCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			Project project = getProject(request);
			MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels
					.get(MetricsOverlayModel.OVERLAY_NAME);

			AbstractOperation op = new PersistMetricsOperation(overlayModel);
			Process process = op.createProcess(project, new Properties());
			
			performProcessAndRespond(request, response, project, process);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
