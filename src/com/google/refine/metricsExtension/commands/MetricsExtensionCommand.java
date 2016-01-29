package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.operations.MetricsExtensionOperation;
import com.google.refine.metricsExtension.util.MetricUtils;
import com.google.refine.metricsExtension.util.MetricUtils.RegisteredMetrics;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

public class MetricsExtensionCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// create Metrics from command
		try {
			Project project = getProject(request);
			
			MetricsOverlayModel metricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
			if (metricsOverlayModel == null) {
				Map<String, List<Metric>> metricsMap = new HashMap<String, List<Metric>>();
				for (Column col : project.columnModel.columns) {
					List<Metric> metricList = new ArrayList<Metric>();
					for (RegisteredMetrics rm : MetricUtils.RegisteredMetrics.values()) {
						if(!rm.equals(RegisteredMetrics.uniqueness)) {
							metricList.add(new Metric(rm.toString(), rm.description(), rm.datatype()));
						}
					}
					metricsMap.put(col.getName(), metricList);
				}
				if (request.getParameter("computeDuplicates") != null) {
					boolean computeDuplicates = Boolean.parseBoolean(request.getParameter("computeDuplicates"));
					if (computeDuplicates) {
						String[] duplicateDependencies = request.getParameterValues("duplicateDependencies");
						metricsOverlayModel = new MetricsOverlayModel(
								metricsMap, 
								new ArrayList<String>(Arrays.asList(duplicateDependencies)), 
								new Metric(RegisteredMetrics.uniqueness.toString(), 
										RegisteredMetrics.uniqueness.description(), 
										RegisteredMetrics.uniqueness.datatype())
								);
					} else {
						metricsOverlayModel = new MetricsOverlayModel(metricsMap);
					}
				}
			}
			
			AbstractOperation op = new MetricsExtensionOperation(metricsOverlayModel);
			Process process = op.createProcess(project, new Properties());
			
			ProjectManager.singleton.ensureProjectSaved(project.id);
			
			performProcessAndRespond(request, response, project, process);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
