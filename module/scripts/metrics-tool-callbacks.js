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

$(document).on("click", "#remove-metric", function(d) {
	var popover = $("div.popover-content");
	if(selectedColName.length <= 1) {
		$.post("../../command/metric-doc/deleteMetric?" + $.param(
	        { 
	          metricName: contextMetric.name, 
	          column: contextColumn,
	          project: theProject.id
	        }) + "&callback=?",
			{},
	      	function(response) {
	        	console.log("success");
	      	}, 
	      	"jsonp"
    	);
	} else {
		$.post("../../command/metric-doc/deleteMetric?" + $.param(
	        { 
	          metricName: contextMetric.name, 
	          columnNames: selectedColName,
	          project: theProject.id
	        }) + "&callback=?",
	      function(response) {
	        console.log("success");
	      }, 
	      "jsonp"
	    );
	}
	d3.selectAll("#overviewTable tbody tr").filter(function(d) {
		return d.name == contextMetric.name;
	}).selectAll("td svg rect")
	.filter(function(d) {
		return d.columnName === contextColumn;
	}).remove();
	var gs = d3.selectAll("g.metrics-overlay").filter(function(d) {
		return d.columnName === contextColumn;
	}).select("g." + contextMetric.name).remove();
});

$(document).on("click", "#merge-metric", function() {
	console.log("merge");
});

$(document).on("click", "#duplicate-metric", function() {
	console.log("duplicate");
});

$('#concat button').click(function() {
    $('#concat button').addClass('active').not(this).removeClass('active');
    metricData[0].concat = $(this).text();
    updateMetric();
    // TODO: insert whatever you want to do with $(this) here
});

$(document).on("click", "input.dataview-popover", function(d) {
	var g = $("g." + d.currentTarget.parentNode.textContent);
	var gSelected = $(g[d.currentTarget.id]);
	if(gSelected.css('display') != 'none') {
		gSelected.hide();
	} else {
		gSelected.show();
	}
});

$(document).on("click", "input.overview-popover", function(d) {
	var checked = $(d.currentTarget).is( ":checked" );
	if(checked) {
		metricToBeCreated.push(d.currentTarget.parentNode.textContent);
	} else {
		var index = metricToBeCreated.indexOf(d.currentTarget.parentNode.textContent);
		if (index > -1) {
		    metricToBeCreated.splice(index, 1);
		}
	}
});