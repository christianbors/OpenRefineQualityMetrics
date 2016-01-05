package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.ArrayList;
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

import com.google.refine.commands.Command;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.operations.MetricsExtensionOperation;
import com.google.refine.model.AbstractOperation;
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
			
			String[] metricNameString = request.getParameterValues("metricName[]");
			String[][] metricFunctionString = new String[metricNameString.length][];
			for (int i = 0; i < metricNameString.length; ++i) {
				metricFunctionString[i] = request.getParameterValues("metricFunction[" + i + "][]");
			}
			String baseColumnString = request.getParameter("baseColumnName");
			MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
//			JSONObject column = ParsingUtilities.evaluateJsonStringToObject(baseColumnString);
			
			List<Metric> metrics = new ArrayList<Metric>(); 
			for (int i = 0; i < metricNameString.length; ++i) {
				metrics.add(new Metric(metricNameString[i], ""));
//				Metric.load(metricsString[i]);
			}
			for (int i = 0; i < metricNameString.length; ++i) {
				for (int j = 0; j < metricFunctionString[i].length; ++j) {
					metrics.get(i).addEvaluable(metricFunctionString[i][j]);
				}
			}
			
//			String colName = column.getString("baseColumnName");
			
			if (overlayModel != null) {
				overlayModel.addMetrics(baseColumnString, metrics);
			} else {
				Map<String, List<Metric>> metricsMap = new HashMap<String, List<Metric>>();
				metricsMap.put(baseColumnString, metrics);
				overlayModel = new MetricsOverlayModel(metricsMap);
			}
			
			AbstractOperation op = new MetricsExtensionOperation(overlayModel);
			Process process = op.createProcess(project, new Properties());
			
			performProcessAndRespond(request, response, project, process);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
