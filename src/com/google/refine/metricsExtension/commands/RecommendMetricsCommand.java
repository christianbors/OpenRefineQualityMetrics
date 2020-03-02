package com.google.refine.metricsExtension.commands;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.commands.Command;
import com.google.refine.expr.CellTuple;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.metricsExtension.model.MetricRecommendation;
import com.google.refine.metricsExtension.model.Recommendation;
import com.google.refine.metricsExtension.util.RecommendMetricsRowVisitor;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.JSONException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class RecommendMetricsCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Project project = getProject(request);
        Properties bindings = ExpressionUtils.createBindings(project);
        Engine engine = new Engine(project);

        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, new RecommendMetricsRowVisitor() {

            @Override public void returnMessage(Map<String, List<MetricRecommendation>> recommendationMap) {
                try {
                    respondJSON(response, new Recommendation(recommendationMap));
                } catch (IOException e1) {
                    try {
                        respondException(response, e1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ServletException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.init(project.columnModel.columns));
    }

    protected RowVisitor createRowVisitor(Properties bindings, HttpServletResponse response) {
        return new RowVisitor() {
            private Properties bindings;
            private HttpServletResponse response;
            private Map<String, DescriptiveStatistics> descriptiveStatistics;
            private Map<String, AtomicLong> stringCount;
            private Map<String, AtomicLong> dateCount;

            public RowVisitor init(Properties bindings, HttpServletResponse response) {
                this.bindings = bindings;
                this.response = response;
                this.descriptiveStatistics = new HashMap<>();
                this.stringCount = new HashMap<>();
                this.dateCount = new HashMap<>();
                return this;
            }

            @Override
            public void start(Project project) {
                for(Column c : project.columnModel.columns) {
                    this.descriptiveStatistics.put(c.getName(), new DescriptiveStatistics());
                    this.stringCount.put(c.getName(), new AtomicLong(0));
                    this.dateCount.put(c.getName(), new AtomicLong(0));
                }
            }

            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                try {
                    CellTuple tuple = row.getCellTuple(project);
                    for (int cellIdx = 0; cellIdx < tuple.row.cells.size(); ++cellIdx) {
                        Cell c = tuple.row.getCell(cellIdx);
                        if(c != null) {
                            DescriptiveStatistics statsForCol = descriptiveStatistics.get(project.columnModel.getColumnByCellIndex(cellIdx).getName());
                            try {
                                double d = Double.parseDouble(c.value.toString());
                                statsForCol.addValue(d);
                            } catch (NumberFormatException nfe) {
                                if (c.value instanceof Date) {
                                    this.dateCount.get(project.columnModel.getColumnByCellIndex(cellIdx).getName()).incrementAndGet();
                                } else {
                                    this.stringCount.get(project.columnModel.getColumnByCellIndex(cellIdx).getName()).incrementAndGet();
                                }
                            }
//                            if() {
//                                statsForCol.addValue((double) c.value);
//                            } else
                        }
                    }
                } catch (NullPointerException e) {
                    return false;
                }
                return false;
            }

            @Override
            public void end(Project project) {
//                System.out.println("counts");
//                for (AtomicLong al : this.stringCount.values()) {
//                    System.out.println(al);
//                }
                try {
                    //TODO: determine recommended metrics for each column
                    Map<String, List<MetricRecommendation>> recommendationMap = new HashMap<>(project.columnModel.columns.size());
                    for (Column colName : project.columnModel.columns)
                        recommendationMap.put(colName.getName(), new ArrayList<>());

                    for (Map.Entry<String, DescriptiveStatistics> entry: this.descriptiveStatistics.entrySet()) {
                        recommendationMap.get(entry.getKey()).add(new MetricRecommendation("completeness", "value, none", ControlFunctionRegistry.getFunction("completeness")));
                        if(entry.getKey().equalsIgnoreCase("id")) {
                            recommendationMap.get(entry.getKey()).add(new MetricRecommendation("uniqueness", "ID", ControlFunctionRegistry.getFunction("uniqueness")));
                        }
                        if(entry.getValue().getValues().length > this.dateCount.get(entry.getKey()).intValue() &&
                                entry.getValue().getValues().length > this.dateCount.get(entry.getKey()).intValue()) {
                            recommendationMap.get(entry.getKey()).add(new MetricRecommendation("plausibility", "value", ControlFunctionRegistry.getFunction("plausibility")));
                            recommendationMap.get(entry.getKey()).add(new MetricRecommendation("validity", "value, number", ControlFunctionRegistry.getFunction("validity")));
                        }
                        if (this.stringCount.get(entry.getKey()).intValue() > this.dateCount.get(entry.getKey()).intValue() &&
                                this.stringCount.get(entry.getKey()).intValue() > entry.getValue().getValues().length) {
                            recommendationMap.get(entry.getKey()).add(new MetricRecommendation("validity", "value, string", ControlFunctionRegistry.getFunction("validity")));
                        }
                        if (this.dateCount.get(entry.getKey()).intValue() > this.stringCount.get(entry.getKey()).intValue() &&
                                this.dateCount.get(entry.getKey()).intValue() > entry.getValue().getValues().length){
                            recommendationMap.get(entry.getKey()).add(new MetricRecommendation("validity", "value, datetime", ControlFunctionRegistry.getFunction("validity")));
                        }
                    }
                    respondJSON(response, new Recommendation(recommendationMap));
                } catch (JSONException e) {
                    try {
                        respondException(response, e);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } catch (ServletException e1) {
                        e1.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.init(bindings, response);
    }


}
