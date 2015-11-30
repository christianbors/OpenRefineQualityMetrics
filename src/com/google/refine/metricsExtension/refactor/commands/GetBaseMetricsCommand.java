package com.google.refine.metricsExtension.refactor.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.metricsExtension.model.BaseMetrics;

public class GetBaseMetricsCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        
        JSONWriter writer = new JSONWriter(response.getWriter());
        try {
			writer.object();
			writer.key("metrics");
			writer.array();
			for(BaseMetrics m : BaseMetrics.values()) {
				writer.value(m);
			}
			
			writer.endArray();
			writer.endObject();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
