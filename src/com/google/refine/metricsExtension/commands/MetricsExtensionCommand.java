package com.google.refine.metricsExtension.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;

public class MetricsExtensionCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// create Metrics from command
		try {
			String jsonString = request.getParameter("metrics");
			JSONObject json = ParsingUtilities.evaluateJsonStringToObject(jsonString);
			

			super.doPost(request, response);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
