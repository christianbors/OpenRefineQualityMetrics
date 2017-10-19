package com.google.refine.metricsExtension.commands;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.expr.metrics.singleColumn.SingleColumnMetricFunction;
import com.google.refine.metricsExtension.expr.metrics.spanningColumn.SpanningColumnMetricFunction;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.metricsExtension.operations.MetricsExtensionOperation;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.process.Process;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class MetricsExtensionCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// create Metrics from command
		try {
			Project project = getProject(request);
			
			MetricsOverlayModel metricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
			if (metricsOverlayModel == null) {
				Map<String, Map<String, Metric>> metricsMap = new HashMap<String, Map<String, Metric>>();
				List<String> colList = new ArrayList<String>();
				for (Column col : project.columnModel.columns) {
					Map<String, Metric> columnMetricsMap = new HashMap<String, Metric>();
					for (Entry<String, Function> entry : ControlFunctionRegistry.getFunctionMapping()) {
						if (entry.getValue() instanceof SingleColumnMetricFunction) {
							SingleColumnMetricFunction mf = (SingleColumnMetricFunction) entry.getValue();
							Metric m = new Metric(entry.getKey(), mf.getDescription());
							m.addEvalTuple(mf.getEvaluable(null), col.getName(), "", false);
							columnMetricsMap.put(entry.getKey(), m);
						}
						metricsMap.put(col.getName(), columnMetricsMap);
					}
					colList.add(col.getName());
				}
				List<SpanningMetric> spanningMetrics = new ArrayList<SpanningMetric>();
				//TODO: insert spanning metrics
				
				if (request.getParameter("computeDuplicates") != null) {
					boolean computeDuplicates = Boolean.parseBoolean(request.getParameter("computeDuplicates"));
					if (computeDuplicates) {
						SpanningColumnMetricFunction uniquenessFn = (SpanningColumnMetricFunction) ControlFunctionRegistry.getFunction("uniqueness");
						SpanningMetric uniqueness = new SpanningMetric("uniqueness",
								uniquenessFn.getDescription(), colList);
						uniqueness.addSpanningEvalTuple(uniquenessFn.getEvaluable(null, null), colList.get(0), "", false);
						metricsOverlayModel = new MetricsOverlayModel(
								metricsMap, spanningMetrics, uniqueness);
								
					} else {
						metricsOverlayModel = new MetricsOverlayModel(metricsMap, spanningMetrics);
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
