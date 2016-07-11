var rowFilter = false;

$("#recalculate").on("click", function(d) {
// recalculate
$.post("../../command/metric-doc/evaluateMetrics?" + $.param({ project: theProject.id }), null, 
	function(data) {
	  window.location.reload(false);
	}, "json");
});

$("#persist").on("click", function(d) {
	$.post("../../command/metric-doc/persistMetrics?" + $.param({ project: theProject.id }), null, 
	  function(data) {}, 
	  "json");
});

$("#addCheck").on("click", function(d) {
	var newEvaluable = {evaluable: "", comment: "", disabled: false};
	metricData[0].evalTuples.push(newEvaluable);
	addEvaluableEntry(newEvaluable);
});

$("#metricSelectMetricModal").on("click", function(btn) {
	$(btn.target).addClass("active");
});

$("#createMetricBtn").on("click", function(btn) {
	var params = { 
		project: theProject.id, 
		metric: $("#metricSelectMetricModal > .active")[0].value,
		columns: $("#columnFormMetricModal").val(), 
		dataType: "numeric"
	};
	$.post("../../command/metric-doc/createMetric?" + $.param(params) + "&callback=?",
		function(data) {
		$("#addMetricModal").modal("hide");
	});
});

$("#filtering").on("click", function() {
    if(rowFilter == false) {
    	$.fn.dataTableExt.search = [filterFunction];
        $("#dataset").DataTable().draw();
        if(metricData[0].spanningEvaluable == null) $("#overlay").hide();
        var svg = d3.select("rect.rect-disabled")
	    	.attr("fill", "gainsboro");
		var button = this.firstChild.textContent = "Show all Entries";
		rowFilter = true;
    } else {
    	$.fn.dataTableExt.search = [];
        $("#dataset").DataTable().draw();
        if(metricData[0].spanningEvaluable == null) $("#overlay").show();
        var svg = d3.select("rect.rect-disabled")
	    	.attr("fill", "transparent");
		var button = this.firstChild.textContent = "Only show dirty Entries";
		rowFilter = false;
    }
})

$(document).on("click", "#remove-eval", function() {
	$(editButton).popover("toggle");
	$("#metricEvaluable" + selectedEditEvaluable).remove();
	if(metricData[0].spanningEvaluable != null) selectedEditEvaluable = parseInt(selectedEditEvaluable) - 1;

	metricData[0].evalTuples.splice(selectedEditEvaluable, 1);
	metricChange = "removeEval";
	updateMetric();
});

$(document).on("click", "#disable-eval", function() {
	var editEval = $("#metricEvaluable" + selectedEditEvaluable);
	if(editEval[0].firstElementChild.lastElementChild.classList.contains("disabled")) {
		editEval[0].firstElementChild.lastElementChild.classList.remove("disabled");
		this.textContent = "disable";
	} else {
		editEval[0].firstElementChild.lastElementChild.classList.add("disabled");
		this.textContent = "enable";
	}
	var idx = $(editEval[0]).val();
	metricData[0].evalTuples[selectedEditEvaluable].disabled = editEval[0].firstElementChild.lastElementChild.classList.contains("disabled");
	metricChange = "disableEval";
	updateMetric();
	$(editButton).popover("toggle");
});

$(document).on("click", "#comment-eval", function() {
	$("#addComment").modal("show");
	$("#addCommentBtn").on("click", function(d, i) {
		var text = $("#commentText").val();
		$("#addComment").modal("hide");
		var selection = $("#metricEvaluable" + selectedEditEvaluable);
		var selIdx = $(selection[0]).val();
		metricData[0].evalTuples[selIdx].comment = text;
		var input = selection.children().last();
		input.tooltip({'trigger': 'hover', 
			'title': text, 
			placement: 'bottom'
		});
		metricChange("none");
		updateMetric();
	});
});

$(document).on("click", "#remove-metric", function(d) {
	var popover = $("div.popover-content");
	$.post("../../command/metric-doc/deleteMetric?" + $.param(
        { 
          metricName: contextMetric.name, 
          column: selectedColName[0],
          project: theProject.id
        }) + "&callback=?",
		{},
      	function(response) {
        	console.log("success");
      	}, 
      	"jsonp"
	);

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
	$.post("../../command/metric-doc/mergeMetric?" + $.param(
		{ 
			metricIndices: selectedMetricIndex, 
			columnNames: selectedColName,
			project: theProject.id
		}) + "&callback=?",
		function(response) {
			console.log("success");
		}, 
		"jsonp"
	);
});

$(document).on("click", "#duplicate-metric", function() {
	if(selectedColName.length == 0) {
		alert("please select a metric to duplicate first");
	} else {
		$( "#duplicateMetricModal" ).modal("show");
	}
});

$("#duplicateMetricBtn").on("click", function(btn) {

	$.post("../../command/metric-doc/duplicateMetric?" + $.param(
		{ 
			metricName: contextMetric.name,
			column: selectedColName[0],
			targetColumn: $("#columnDuplicateModal").val()[0],
			project: theProject.id
		}) + "&callback=?",
		function(response) {
			console.log("success");
		}, 
		"jsonp"
	);
	$("#addMetricModal").modal("hide");

});

$('#concat button').click(function() {
    $('#concat button').addClass('active').not(this).removeClass('active');
    metricChange = "concat";
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

$("#exportMetrics").on("click", function(d) {
	var model = JSON.stringify(overlayModel);
	var blob = new Blob([model], {type: "text/plain;charset=utf-8"});
  	saveAs(blob, "metricsOverlayModel.json");
});

$("#importMetrics").on("click", function(d) {
	$.getJSON(
    	"../../command/core/get-all-project-metadata",
    	null,
    	function(d) {
    		$.each(d.projects, function(proj) {
    			$.getJSON(
                    "../../command/metric-doc/getMetricsOverlayModel?" + $.param({ project: proj }), 
                    null,
                    function(d) {
                    	if(d != null) {
                    		console.log("we have an overlay model");
                    	}
                    },
                    "json"
                );
    		})
    	},
    	"json"
    	);
})

$('input[type=radio][name=datatype]').change(function() {
	metricData[0].datatype = this.value;
});

$("#alertMetricUpdateClose").on("click", function(d) {
	$("#alertMetricUpdate").hide();
})