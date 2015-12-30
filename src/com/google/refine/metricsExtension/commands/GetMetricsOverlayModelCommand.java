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

public class GetMetricsOverlayModelCommand extends Command {
	@Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project;
            try {
                project = getProject(request);
            } catch (ServletException e) {
                respond(response, "error", e.getLocalizedMessage());
                return;
            }
            MetricsOverlayModel model = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
			if (model != null) {
				respondJSON(response, model);
			}
        } catch (JSONException e) {
            respondException(response, e);
        }
    }
}
