package com.google.refine.metricsExtension.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.model.Project;

public class MergeMetricCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Project project = getProject(request);
		String[] columnNames = request.getParameterValues("columnNames[]");
		String[] metricName = request.getParameterValues("metricNames[]");
	}

}
