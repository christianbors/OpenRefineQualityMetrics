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
      '<button type="button" class="btn" id="disable-eval">' + disableButtonClass + '</button>'+
      '<button type="button" class="btn btn-warning" id="comment-eval">comment</button></div>'
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
  headers.on("click", function(d) {
    console.log("test:" + d.target.textContent);
    var colIdx;
    for (var i = 0; i < d.target.parentNode.childNodes.length; i++) {
        if (d.target.parentNode.childNodes[i] == d.target) {
            colIdx = i;
            break;
        }
    }
    var popoverColumn = theProject.overlayModels.metricsOverlayModel.metricColumns.filter(function(col) {
        return col.columnName == d.target.textContent;
    })[0];
    var popoverSnippet = '';
    for(var i = 0; i < popoverColumn.metrics.length; i++) {
      popoverSnippet += "<div class='checkbox'><label><input checked='true' id='"+colIdx+"' class='overview-popover' type='checkbox'>" + popoverColumn.metrics[i].name + "</label></div>";
      // popoverColumn.metrics[i].name
      if($($("g." + popoverColumn.metrics[i].name)[colIdx]).css('display') == 'none') {
        $("input.overview-popover #" + i).prop("checked", false);
      }
    }
    $(this).data("bs.popover").options.content = popoverSnippet;
  });
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
  }).on("click", function () {
    var _this = this;
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
  console.log(data);
}