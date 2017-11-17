package com.google.refine.metricsExtension.commands;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.expr.metrics.singleColumn.SingleColumnMetricFunction;
import com.google.refine.metricsExtension.expr.metrics.spanningColumn.SpanningColumnMetricFunction;
import com.google.refine.metricsExtension.expr.metrics.spanningColumn.Uniqueness;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricRecommendation;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.metricsExtension.operations.MetricsExtensionOperation;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import org.json.JSONArray;
import org.json.JSONObject;

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
				if (request.getParameter("metricsConfigList") == null) {
					Map<String, Map<String, Metric>> metricsMap = new HashMap<String, Map<String, Metric>>();
					List<String> colList = new ArrayList<String>();
					for (Column col : project.columnModel.columns) {
						Map<String, Metric> columnMetricsMap = new HashMap<String, Metric>();
						// check if metric functions have been submitted by the user
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
				} else {
					Map<String, Map<String, Metric>> metricColumnsMap = new HashMap<>();
					List<SpanningMetric> spanningMetrics = new ArrayList<>();
					SpanningMetric uniqueness = null;

					JSONArray metricsColumnConfigList = ParsingUtilities.evaluateJsonStringToArray(request.getParameter("metricsConfigList"));
					for (int listIdx = 0; listIdx < metricsColumnConfigList.length(); ++listIdx) {
						JSONObject colConfigMetricsEntry = metricsColumnConfigList.getJSONObject(listIdx);
						if (colConfigMetricsEntry != null) {
							String colName = colConfigMetricsEntry.getString("column");
							Map<String, Metric> metricsMap = new HashMap<String, Metric>();

							JSONArray metricsRecommendations = colConfigMetricsEntry.getJSONArray("metrics");
							for (int metricIdx = 0; metricIdx < metricsRecommendations.length(); ++metricIdx) {
								JSONObject metricRecomm = metricsRecommendations.getJSONObject(metricIdx);

								String metricName = metricRecomm.getString("name");
								if (ControlFunctionRegistry.getFunction(metricName).getClass().equals(Uniqueness.class)) {
									SpanningColumnMetricFunction fn = (SpanningColumnMetricFunction) ControlFunctionRegistry.getFunction(metricName);
									List<String> columns = new ArrayList<String>();
									uniqueness = new SpanningMetric(metricName, fn.getDescription(), Arrays.asList(metricRecomm.getString("parameters").split(", ")));
									uniqueness.addSpanningEvalTuple(fn.getEvaluable(metricRecomm.getString("parameters").split(", "), metricRecomm.getString("parameters").split(", ")), colName, "", false);
								} else {
									SingleColumnMetricFunction fn = (SingleColumnMetricFunction) ControlFunctionRegistry.getFunction(metricName);
									Metric m = new Metric(metricName, fn.getDescription());
									String[] params = metricRecomm.getString("parameters").split(", ");
									if (params[0].equalsIgnoreCase("value"))
										params = Arrays.copyOfRange(params, 1, params.length);
									m.addEvalTuple(fn.getEvaluable(params), colName, "", false);
									metricsMap.put(metricRecomm.getString("name"), m);
								}
							}
							metricColumnsMap.put(colName, metricsMap);
						}
					}
					metricsOverlayModel = new MetricsOverlayModel(
							metricColumnsMap, spanningMetrics, uniqueness);
					metricsOverlayModel.setComputeDuplicates(uniqueness != null);
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
