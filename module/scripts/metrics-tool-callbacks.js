$(document).on("click", "#remove-eval", function() {
  selectedEditEvaluable.remove();
});

$(document).on("click", "#disable-eval", function() {
	if(selectedEditEvaluable.lastElementChild.classList.contains("disabled")) {
		selectedEditEvaluable.lastElementChild.classList.remove("disabled");
		this.textContent = "disable";
	} else {
		selectedEditEvaluable.lastElementChild.classList.add("disabled");
		this.textContent = "enable";
	}
});

$(document).on("click", "#comment-eval", function() {
	metricData
	$("#addComment").modal("show");
	$("#addCommentBtn").on("click", function(d, i) {
		var text = $("#commentText").val();
		$("#addComment").modal("hide");
		var selection = $("#" + selectedEditEvaluable);
		metricData[0].comments[selection.attr("idx")] = text;
		var input = selection.children().last();
		input.tooltip({'trigger': 'hover', 
			'title': text, 
			placement: 'bottom'
		});
	});
		// input.attr("data-toggle", "tooltip")
		// .attr("data-placement", "right")
		// .attr("data-html", "true")
		// .attr("title", "1st line of text <br> 2nd line of text");
	// var id = selectedEditEvaluable.attributes.add(data-toggle="tooltip" data-placement="right" data-html="true" title="1st line of text <br> 2nd line of text");
});

$('#concat button').click(function() {
    $('#concat button').addClass('active').not(this).removeClass('active');
    var val = $(this).text();
    $.post("../../command/metric-doc/updateMetric?" + $.param(
        { 
          metricName: metricData[0].name, 
          column: selectedColName[0],
          metricIndex: selectedMetricIndex[0],
          metricDatatype: metricData[0].datatype,
          metricDescription: metricData[0].description,
          metricEvaluables: metricData[0].evaluables,
          concat: val,
          comments: metricData[0].comments,
          project: theProject.id 
        }) + "&callback=?",
      {},
      {},
      function(response) {
        console.log("success");
      }, 
      "jsonp"
    );
    // TODO: insert whatever you want to do with $(this) here
});