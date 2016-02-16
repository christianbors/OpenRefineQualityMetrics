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

function addEvaluableEntry(value) {
	var i = $(".metricCheck").length;
	$("<li class='input-group metricCheck' idx='" + i + "' id='metricEvaluable" + i + "'>" + 
        "<span class='input-group-addon' id='edit"+ i +"' data-toggle='popover'>edit</span>" + 
        "<input data-toggle='tooltip' type='text' class='form-control pop metricInput' placeholder='Check' id='eval"+i+"'/>  " + //TODO: aria-describedby='basic-addon1'>
        "</li>").insertBefore("#addCheckButton");
	if (value != "") {
      $("#eval" + i).val(value);
	}
	$("[data-toggle=popover]").popover({
      html: 'true',
      trigger: 'manual',
      placement: 'auto top',
      animation: 'false',
      container: 'body',
      content: '<div class="btn-group" role="group"><button type="button" class="btn btn-danger" id="remove-eval">remove</button>'+
        '<button type="button" class="btn" id="disable-eval">disable</button>'+
        '<button type="button" class="btn btn-warning" id="comment-eval">comment</button></div>'
    }).on("click", function () {
      selectedEditEvaluable = this.parentNode.id;
      var _this = this;
      $(this).popover("toggle");
      $(".popover").on("mouseleave", function () {
          $(_this).popover('hide');
      });
    });
    $('.metricInput').keypress(function(event){
    	if (event.which == 13) {
    		console.log("enter");
    		metricData[0].evaluables[parseInt(this.parentNode.attributes.idx.value)] = this.value;
    		updateMetric();
    	}
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
	          metricEvaluables: metricData[0].evaluables,
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