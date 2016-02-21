function addEvaluableEntry(value) {
	var i = $(".metricCheck").length;
	$("<li class='input-group metricCheck' idx='" + i + "' id='metricEvaluable" + i + "'></li>").insertBefore("#addCheckButton");
  $("#metricEvaluable" + i).append("<span class='input-group-addon' id='edit"+ i +"' data-toggle='popover'>edit</span>");
  $("#metricEvaluable" + i).append("<input data-toggle='tooltip' type='text' class='form-control pop metricInput' placeholder='Check' id='eval"+i+"'/>  "); //TODO: aria-describedby='basic-addon1'>
  $("#eval" + i).keypress(function(event){
    if (event.which == 13) {
      metricData[0].evalTuples[i].evaluable = this.value;
      updateMetric();
    }
  });
  var disableButtonClass = "disable";
  if(metricData[0].evalTuples[i].disabled) {
    $("#eval" + i).addClass("disabled");
    disableButtonClass = "enable";
  }

  $("#edit" + i).popover({
    html: 'true',
    trigger: 'manual',
    placement: 'auto top',
    animation: 'false',
    container: 'body',
    content: '<div class="btn-group" role="group"><button type="button" class="btn btn-danger" id="remove-eval">remove</button>'+
      '<button type="button" class="btn btn-default" id="disable-eval">' + disableButtonClass + '</button>'+
      '<button type="button" class="btn btn-default" id="comment-eval">comment</button></div>'
  }).on("click", function () {
    selectedEditEvaluable = this.parentNode.id;
    var _this = this;
    $(this).popover("toggle");
    $(".popover").on("mouseleave", function () {
        $(_this).popover('hide');
    });
  });

  if (value != "") {
      $("#eval" + i).val(value);
  }
}

function dataViewPopover() {
  var headers = $("div.dataTables_scrollHeadInner > table.dataTable > thead > tr > th.sorting_disabled");
  headers.addClass("popoverHeader");
  headers.attr("data-toggle", "popover");
  //'</ul>';// role="group"><button type="button" class="btn btn-danger" id="remove-eval">remove</button>'+
      //'<button type="button" class="btn" id="disable-eval">tyst[er</button>'+
      //'<button type="button" class="btn btn-warning" id="comment-eval">comment</button></div>'
  headers.popover({
    html: 'true',
    trigger: 'manual',
    placement: 'auto top',
    animation: 'false',
    container: 'body',
    title: 'Show/Hide Metric Overlay',
    content: ''
  });
  d3.selectAll("th.sorting_disabled").on("contextmenu", function () {
    d3.event.preventDefault();
    var _this = this;
    var colIdx;
    for (var i = 0; i < _this.parentNode.childNodes.length; i++) {
        if (this.parentNode.childNodes[i] == _this) {
            colIdx = i;
            break;
        }
    }
    var popoverColumn = theProject.overlayModels.metricsOverlayModel.metricColumns.filter(function(col) {
        return col.columnName == _this.textContent;
    })[0];
    var popoverSnippet = '';
    for(var i = 0; i < popoverColumn.metrics.length; i++) {
      popoverSnippet += "<div class='checkbox'><label><input checked='true' id='"+colIdx+"' class='dataview-popover' type='checkbox'>" + 
        popoverColumn.metrics[i].name + "</label></div>";
      if($($("g." + popoverColumn.metrics[i].name)[colIdx]).css('display') == 'none') {
        $("input.dataview-popover #" + i).prop("checked", false);
      }
    }
    $(this).data("bs.popover").options.content = popoverSnippet;

    $(this).popover("toggle");
    $(".popover").on("mouseleave", function () {
        $(_this).popover('hide');
    });
  });
}

function updateMetric() {
	$.post("../../command/metric-doc/updateMetric?" + $.param(
	        { 
	          metricName: metricData[0].name, 
	          column: selectedColName[0],
	          metricIndex: selectedMetricIndex[0],
	          metricDatatype: metricData[0].datatype,
	          metricDescription: metricData[0].description,
	          metricEvalTuples: metricData[0].evalTuples,
            metricEvalCount: metricData[0].evalTuples.length,
	          concat: metricData[0].concat,
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
}

function addMetricToColumn(data, index) {
  d3.event.preventDefault();
  var cellIndex = data.cellIndex;
  var popoverColumn = theProject.overlayModels.metricsOverlayModel.metricColumns.filter(function(col) {
      return col.columnName == data.name;
  })[0];
  if(columnForMetricToBeCreated == null || popoverColumn.columnName != columnForMetricToBeCreated) {
    columnForMetricToBeCreated = popoverColumn.columnName;
    metricToBeCreated = [];
  }
  $("#metricSelectMetricModal .btn").remove();
  var popoverSnippet = '';
  var avMetrics = theProject.overlayModels.metricsOverlayModel.availableMetrics;
  for(var i = 0; i < avMetrics.length; i++) {
    var cl = "btn btn-default";
    var activeMetric = popoverColumn.metrics.filter(function(metric) {
      return metric.name == avMetrics[i].name;
    });
    var checkedMetric = metricToBeCreated.filter(function(metric) {
      return metric == avMetrics[i].name;
    });
    var disabled = "",
        checked = "";
    if(activeMetric.length > 0) {
      cl += " disabled";
      disabled = " disabled";
    }
    if(checkedMetric.length > 0) {
      checked = " checked";
    }
    popoverSnippet += "<div" + disabled + " class='checkbox'><label for='" + cellIndex + "'" +
      "><input id='"+cellIndex+"' class='overview-popover'" + disabled + checked + 
      " type='checkbox'>" + avMetrics[i].name + "</label></div>";

    $("#metricSelectMetricModal").append('<button type="button" value="' + avMetrics[i].name + '" class="' + cl + '">'+ capitalizeFirstLetter(avMetrics[i].name) + '</button>');
    $("#metricSelectMetricModal > button").on("click", function() {
      $("#metricSelectMetricModal").val($(this).text());
    });
  }
  popoverSnippet += "<button type='button' class='btn' id='addMetricBtn'>Add Metrics</button>";
  var bsPopover = $(this).data("bs.popover");
  bsPopover.options.content = popoverSnippet;
  $(document).on("click", "#addMetricBtn", function(d) {
    var colSelected = $("#columnFormMetricModal option").filter(function(option) {
      return this.value == columnForMetricToBeCreated;
    }).attr("selected", true);
    var metricsSelected = $("#metricSelectMetricModal").children().filter(function(button) {
      return metricToBeCreated.indexOf(this.value) > -1;
    });
    metricsSelected.addClass("active");
    $( "#addMetricModal" ).modal("show");
  });

  var _this = this;
  $(this).popover("toggle");
  $(".popover").on("mouseleave", function () {
      $(_this).popover('hide');
  });
}

function wrap(text, widths) {
  text.each(function() {
    var text = d3.select(this),
        words = text.text().split(/\s+/).reverse(),
        word,
        line = [],
        lineNumber = 0,
        lineHeight = 1.1, // ems
        y = text.attr("y"),
        dy = parseFloat(text.attr("dy")),
        tspan = text.text(null).append("tspan").attr("x", 0).attr("y", y).attr("dy", dy + "em"),
        width;
    if(widths.length == 1) width = widths[0];
    else width = widths[this.__data__];

    while (word = words.pop()) {
      line.push(word);
      tspan.text(line.join(" "));
      if (tspan.node().getComputedTextLength() > width) {
        line.pop();
        tspan.text(line.join(" "));
        line = [word];
        tspan = text.append("tspan").attr("x", 0).attr("y", y).attr("dy", ++lineNumber * lineHeight + dy + "em").text(word);
      }
    }
  });
}