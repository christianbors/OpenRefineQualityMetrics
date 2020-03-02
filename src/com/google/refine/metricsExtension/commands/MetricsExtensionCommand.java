package com.google.refine.metricsExtension.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.expr.Evaluable;
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
import org.apache.jena.atlas.json.JsonObject;
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
			
			MetricsOverlayModel metricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get(MetricsOverlayModel.OVERLAY_NAME);
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
                            m.addEvalTuple(entry.getKey(), col.getName(), "", false);
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
                        uniqueness.addSpanningEvalTuple("uniqueness", colList.get(0), "", false);

                        if(metricsOverlayModel == null) {
                            metricsOverlayModel = new MetricsOverlayModel(
                                    metricsMap, spanningMetrics, uniqueness);
                        }
                    } else {
                        if(metricsOverlayModel == null) {
                            metricsOverlayModel = new MetricsOverlayModel(metricsMap, spanningMetrics);
                        }
                    }
                }
            } else {
                Map<String, Map<String, Metric>> metricColumnsMap = new HashMap<>();
                List<SpanningMetric> spanningMetrics = new ArrayList<>();
                SpanningMetric uniqueness = null;

                ObjectNode metricsColumnConfigList = ParsingUtilities.evaluateJsonStringToObjectNode(request.getParameter("metricsConfigList"));
//                for (int listIdx = 0; listIdx < size(); ++listIdx) {
                for (Iterator<String> it = metricsColumnConfigList.fieldNames(); it.hasNext(); ) {
                    String colName = it.next();
                    JsonNode colConfigMetricsEntry = metricsColumnConfigList.get(colName);
                    if (colConfigMetricsEntry != null) {
//                        String colName = colConfigMetricsEntry.get("column").toString();
                        Map<String, Metric> metricsMap = new HashMap<String, Metric>();

//                        JsonNode metricsRecommendations = colConfigMetricsEntry.get("metrics");
                        for (Iterator<JsonNode> metricIt = colConfigMetricsEntry.elements(); metricIt.hasNext(); ) {
                            JsonNode metricRecomm = metricIt.next();
//                        }
//                        for (int metricIdx = 0; metricIdx < colConfigMetricsEntry.size(); ++metricIdx) {
//                            JsonNode metricRecomm = metricsRecommendations.get(metricIdx);
//                            MetricRecommendation recommendation = ParsingUtilities.mapper.readValue(metricRecomm, MetricRecommendation.class);

                            String metricName = metricRecomm.get("name").asText();
                            if (ControlFunctionRegistry.getFunction(metricName).getClass().equals(Uniqueness.class)) {
                                SpanningColumnMetricFunction fn = (SpanningColumnMetricFunction) ControlFunctionRegistry.getFunction(metricName);
                                List<String> columns = new ArrayList<String>();

                                uniqueness = new SpanningMetric(metricName, fn.getDescription(), Arrays.asList(metricRecomm.get("parameters").asText().split(", ")));
                                uniqueness.addSpanningEvalTuple(fn.getEvaluable(columns.toArray(new String[0]), fn.getParams().split(",")).toString(), colName, "", false);
                            } else {
                                SingleColumnMetricFunction fn = (SingleColumnMetricFunction) ControlFunctionRegistry.getFunction(metricName);
                                Metric m = new Metric(metricName, fn.getDescription());
                                String[] params = metricRecomm.get("parameters").asText().split(", ");
                                if (params[0].equalsIgnoreCase("value"))
                                    params = Arrays.copyOfRange(params, 1, params.length);
                                m.addEvalTuple(buildEvalString(metricName, metricRecomm.get("parameters").asText(null)), colName, "", false);
                                metricsMap.put(metricRecomm.get("name").asText(), m);
                            }
                        }
                        metricColumnsMap.put(colName, metricsMap);
                    }
                }
                metricsOverlayModel = new MetricsOverlayModel(
                        metricColumnsMap, spanningMetrics, uniqueness);
                metricsOverlayModel.setComputeDuplicates(uniqueness != null);
            }
            project.overlayModels.put(MetricsOverlayModel.OVERLAY_NAME, metricsOverlayModel);
            logger.info("new provenance instance for project {}", project.id);

//			AbstractOperation op = new MetricsExtensionOperation(metricsOverlayModel);
//			Process process = op.createProcess(project, new Properties());
            project.getMetadata().setCustomMetadata("metricsProject", true);

			ProjectManager.singleton.ensureProjectSaved(project.id);
            response.setStatus(HttpServletResponse.SC_OK);
            respond(response, String.valueOf(project.id));
//			performProcessAndRespond(request, response, project, process);
		} catch (Exception e) {
			respondException(response, e);
			e.printStackTrace();
		}
	}

	private String buildEvalString(String metricName, String params) {
	    if (params == null)
	        return metricName + "(value)";
	    else {
//	        String paramString = String.join(", ", params);
            return metricName + "(" + params + ")";
        }
    }
}
