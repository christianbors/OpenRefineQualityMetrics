package com.google.refine.metricsExtension.commands.browsing;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.refine.browsing.Engine;
import com.google.refine.commands.browsing.ComputeFacetsCommand;
import com.google.refine.metricsExtension.browsing.MetricsEngine;
import com.google.refine.model.Project;


public class CustomComputeFacetsCommand extends ComputeFacetsCommand {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Project project = getProject(request);
            Engine engine = getCustomEngine(request, project);
            
            engine.computeFacets();
            
            respondJSON(response, engine);
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    private Engine getCustomEngine(HttpServletRequest request, Project project)
    throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        if (project == null) {
            throw new IllegalArgumentException("parameter 'project' should not be null");
        }

        Engine engine = new MetricsEngine(project);
        JSONObject o = getEngineConfig(request);
        if (o != null) {
            engine.initializeFromJSON(o);
        }
        return engine;
    }
}
