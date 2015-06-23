
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
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public abstract class ColumnMetric<E, F> extends Command implements Jsonizable {

    protected int cellIndex;
    protected List<E> values;
    protected Map<Integer, F> spuriousValues;
    protected float metric;

    protected RowVisitor createRowVisitor(Project project, int cellIndex, List<E> values,
            Map<Integer, F> spuriousValues, float metric) {
        return new RowVisitor() {

            int cellIndex;
            List<E> values;
            Map<Integer, F> spuriousValues;
            float metric;

            public RowVisitor init(int cellIndex, List<E> values, Map<Integer, F> spuriousValues, float metric) {
                this.cellIndex = cellIndex;
                this.values = values;
                this.spuriousValues = spuriousValues;
                this.metric = metric;
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
                        values.add(val);
                    } else {
                        throw new MetricsException("Value spurious, " + val);
                    }
                    this.values.add(val);
                } catch (Exception e) {
                    spuriousValues.put(rowIndex, (F) row.getCellValue(this.cellIndex));
                }

                return false;
            }

            @Override
            public void end(Project project) {
                metric = ((float) values.size() / (float) (values.size() + spuriousValues.size()));
                endVisit(project, values, spuriousValues, metric);
            }
        }.init(cellIndex, values, spuriousValues, metric);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            spuriousValues = new HashMap<Integer, F>();
            values = new ArrayList<E>();
            metric = 0f;

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
            filteredRows.accept(project, createRowVisitor(project, cellIndex, values, spuriousValues, metric));
        } catch (Exception e) {
            respondException(response, e);
        } finally {
            ProjectManager.singleton.setBusy(false);
            try {
                write(new JSONWriter(response.getWriter()), new Properties());
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();

        writer.key("elementList");
        writer.array();
        for (Iterator<Map.Entry<Integer, F>> entries = spuriousValues.entrySet().iterator(); entries.hasNext();) {
            Map.Entry<Integer, F> entry = entries.next();
            writer.object();
            writer.key(entry.getKey().toString());
            writer.value(entries);
            writer.endObject();
        }
        writer.endArray();

        writer.key("metric").value(Float.toString(metric));

        writeSpecificProperty(writer, options);

        writer.endObject();
    }

    protected abstract boolean checkSpurious(E val);

    protected abstract void endVisit(Project project, List<E> values, Map<Integer, F> spuriousValues, float metric);

    protected abstract void startVisit(Project project);

    protected abstract void writeSpecificProperty(JSONWriter writer, Properties options)
            throws JSONException;
}
