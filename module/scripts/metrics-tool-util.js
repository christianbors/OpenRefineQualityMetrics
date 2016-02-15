function updateMetric(theProject, metric) {
	$.post("../../command/metric-doc/updateMetric?" + $.param(
        { 
          metricName: metric.name, 
          column: selectedColName,
          metricIndex: selectedMetricIndex,
          metricDatatype: metric.datatype,
          metricDescription: metric.description,
          metricEvaluables: metric[0].evaluables,
          project: theProject.id 
        }) + "&callback=?",
      {},
      {},
      function(response) {
        console.log("success");
      }, 
      "jsonp"
    );
}