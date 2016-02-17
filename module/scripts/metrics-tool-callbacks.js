$(document).on("click", "#remove-eval", function() {
	$("#" + selectedEditEvaluable).remove();
	$.each($(".metricInput").not(".disabled"), function(i, activeEval) {
		metricData[0].evaluables.push(activeEval.value);
	});
	updateMetric();
});

$(document).on("click", "#disable-eval", function() {
	var editEval = $("#" + selectedEditEvaluable);
	if(editEval[0].lastElementChild.classList.contains("disabled")) {
		editEval[0].lastElementChild.classList.remove("disabled");
		this.textContent = "disable";
	} else {
		editEval[0].lastElementChild.classList.add("disabled");
		this.textContent = "enable";
	}
	metricData[0].evalTuples[editEval.attr("idx")].disabled = editEval[0].lastElementChild.classList.contains("disabled");
	updateMetric();
});

$(document).on("click", "#comment-eval", function() {
	metricData
	$("#addComment").modal("show");
	$("#addCommentBtn").on("click", function(d, i) {
		var text = $("#commentText").val();
		$("#addComment").modal("hide");
		var selection = $("#" + selectedEditEvaluable);
		metricData[0].evalTuples[selection.attr("idx")].comment = text;
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
    metricData[0].concat = $(this).text();
    updateMetric();
    // TODO: insert whatever you want to do with $(this) here
});