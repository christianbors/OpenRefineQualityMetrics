
package com.google.refine.metricsExtension.metrics.column;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.commands.Command;
import com.google.refine.metricsExtension.MetricsException;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public abstract class ColumnMetric<E> extends Command {

    protected int cellIndex;
    protected Metric<E> metric;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            metric = new Metric<E>();

            ProjectManager.singleton.setBusy(true);
            Project project = getProject(request);

            ColumnModel columnModel = project.columnModel;
            Column column = columnModel.getColumnByName(request.getParameter("column_name"));
            int cellIndex = column.getCellIndex();

            Engine engine = new Engine(project);
            JSONObject engineConfig = null;

            engineConfig = ParsingUtilities.evaluateJsonStringToObject(request.getParameter("engine"));
            engine.initializeFromJSON(engineConfig);

            FilteredRows filteredRows = engine.getAllRows();

            RowVisitor rw = new RowVisitor() {

                int cellIndex;

                public RowVisitor init(int cellIndex) {
                    this.cellIndex = cellIndex;
                    return this;
                }

                @Override
                public void start(Project project) {
                    startVisit(project);
                }

                @SuppressWarnings("unchecked")
                @Override
                public boolean visit(Project project, int rowIndex, Row row) {
                    try {
                        E val = (E) row.getCellValue(this.cellIndex);
                        if (!checkSpurious(val)) {
                            metric.getValidItems().add(val);
                        } else {
                            throw new MetricsException("Value spurious, " + val);
                        }
                    } catch (Exception e) {
                        metric.getSpuriousItemMap().put(rowIndex, (E) row.getCellValue(this.cellIndex));
                    }

                    return false;
                }

                @Override
                public void end(Project project) {
                    metric.setMeasure(((float) metric.getValidItems().size() / (float) (metric.getValidItems().size() + metric.getSpuriousItemMap().size())));
                    endVisit(project, metric);
                }
            }.init(cellIndex);

            filteredRows.accept(project, rw);
        } catch (Exception e) {
            respondException(response, e);
        } finally {
            ProjectManager.singleton.setBusy(false);
            try {
                respondJSON(response, metric);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    protected abstract boolean checkSpurious(E val);

    protected abstract void endVisit(Project project, Metric<E> metric);

    protected abstract void startVisit(Project project);

    protected abstract void writeSpecificProperty(JSONWriter writer, Properties options)
            throws JSONException;
}
