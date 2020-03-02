package com.google.refine.metricsExtension.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.JSONException;

import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.CellTuple;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.metricsExtension.model.MetricRecommendation;
import com.google.refine.metricsExtension.model.Recommendation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public abstract class RecommendMetricsRowVisitor implements RowVisitor {

    private Map<String, DescriptiveStatistics> descriptiveStatistics;
    private Map<String, AtomicLong> stringCount;
    private Map<String, AtomicLong> dateCount;
    private List<Column> columnsSelected;

    private Recommendation recommendation;

    public RowVisitor init(List<Column> columnsSelected) {
        this.descriptiveStatistics = new HashMap<>();
        this.stringCount = new HashMap<>();
        this.dateCount = new HashMap<>();
        this.columnsSelected = columnsSelected;
        return this;
    }

    public Recommendation getRecommendation() {
        return this.recommendation;
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
//            for (Column col : columnsSelected) {
//            int cellIdx = project.columnModel.getColumnIndexByName(col.getName());
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
                }
            }
        } catch (NullPointerException e) {
            return false;
        }
        return false;
    }

    @Override
    public void end(Project project) {
        try {
            //TODO: determine recommended metrics for each column
            Map<String, List<MetricRecommendation>> recommendationMap = new HashMap<>(columnsSelected.size());
            for (Column colName : columnsSelected)
                recommendationMap.put(colName.getName(), new ArrayList<>());

            for (Map.Entry<String, DescriptiveStatistics> entry: this.descriptiveStatistics.entrySet()) {
                if(recommendationMap.containsKey(entry.getKey())) {
                    MetricRecommendation mr = new MetricRecommendation("completeness", "value, none",
                        ControlFunctionRegistry.getFunction("completeness"));
                    recommendationMap.get(entry.getKey()).add(mr);
                    if (entry.getKey().equalsIgnoreCase("id")) {
                        recommendationMap.get(entry.getKey()).add(new MetricRecommendation("uniqueness", "ID",
                            ControlFunctionRegistry.getFunction("uniqueness")));
                    }
                    if (entry.getValue().getValues().length > this.dateCount.get(entry.getKey()).intValue()
                        && entry.getValue().getValues().length > this.dateCount.get(entry.getKey()).intValue()) {
                        recommendationMap.get(entry.getKey()).add(new MetricRecommendation("plausibility", "value",
                            ControlFunctionRegistry.getFunction("plausibility")));
                        recommendationMap.get(entry.getKey()).add(new MetricRecommendation("validity", "value, number",
                            ControlFunctionRegistry.getFunction("validity")));
                    }
                    if (this.stringCount.get(entry.getKey()).intValue() > this.dateCount.get(entry.getKey()).intValue()
                        && this.stringCount.get(entry.getKey()).intValue() > entry.getValue().getValues().length) {
                        recommendationMap.get(entry.getKey()).add(new MetricRecommendation("validity", "value, string",
                            ControlFunctionRegistry.getFunction("validity")));
                    }
                    if (this.dateCount.get(entry.getKey()).intValue() > this.stringCount.get(entry.getKey()).intValue()
                        && this.dateCount.get(entry.getKey()).intValue() > entry.getValue().getValues().length) {
                        recommendationMap.get(entry.getKey()).add(
                            new MetricRecommendation("validity", "value, datetime", ControlFunctionRegistry.getFunction("validity")));
                    }
                }
            }
            this.recommendation = new Recommendation(recommendationMap);
            returnMessage(recommendationMap);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public abstract void returnMessage(Map<String, List<MetricRecommendation>> recommendationMap);
}
