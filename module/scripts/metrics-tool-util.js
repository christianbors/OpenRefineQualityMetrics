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
  var param = {
    project: theProject.id, 
    metricIndex: selectedMetricIndex[0],
    columnName: selectedColName[0]
  };
  $.post("../../command/metric-doc/evaluateSelectedMetric?" + $.param(param), null, function(data) {
    metricData[0] = data;
    overlayModel.metricColumns.filter(function(col) {
      return col.columnName == selectedColName[0];
    })[0].metrics[selectedMetricIndex[0]] = data;
    var overviewTd = d3.select("#overviewTable tbody tr td.selected rect").attr("width", function(d, i) {
      if (d != null) {
        var metricName = this.parentNode.parentNode.parentNode.__data__;
        var metricCurrent = d.metrics.filter(function(m) {
          return m.name == metricName.name;
        });
        if (metricCurrent.length > 0) {
          return metricCurrent[0].measure * colWidths[i];
        }
      }
    });
    
    var col = d3.selectAll("#overlay g.metrics-overlay").filter(function(d, i){
      return d.columnName == selectedColName[0];
    });
    col.selectAll("g").remove();
    var newGroups = col.selectAll("g")
      .data(function(d) {
        if (d != null) {
          return d.metrics;
        } else {
          return [];
        }
      }).enter()
      .append("g")
      .attr("class", "metrics-overlay-col")
      .attr("class", function(d, i) {
        return d.name;
      })
      .attr("transform", function(d, i) {
        var offset = 0;
        if (i > 0) {
          var attr = this.parentNode.children[i-1].attributes["transform"];
          var transl = attr.value.match(/\d+/);
          offset = offset + parseInt(transl[0]);
        }
        if(d.dirtyIndices != null) {
          offset = offset + 12;
        }
        return "translate(-" + offset + ",0)";
      });
    // col = d3.select("#overlay").selectAll(".metrics-overlay");

    var bins = newGroups.selectAll("rect.metrics-bin")
      .data(function(d) {
        if (d.dirtyIndices != null) {
          return d.dirtyIndices; 
        } else {
          return [];
        }
      }).enter()
      .append("rect")
      .attr("class", "metrics-bin")
      .attr("width", function (d, i) {
        return  12; 
      })
      .attr("height", 1)
      // .attr("y", function(d) { return overlayY(d.index); })
      .style("fill", function(d, i) {
        var metricsCol = this.parentNode.parentNode.__data__;
        var current = this.parentNode.__data__;
        return z(metricsCol.metrics.indexOf(current));
      });

    bins.each(function (d) {
      var y = overlayY(d.index);
      var ys = d3.select(this)
        .attr("y", y);
    });

    redrawDetailView(theProject, metricData, selectedMetricIndex[0], rowModel, overlayModel);
  },
  "json");
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

function detaildragresize(d) {
  //Max x on the left is x - width 
  //Max x on the right is width of screen + (dragbarw/2)
  // var dragx = Math.max(d.x + (dragbarw/2), Math.min(detailWidth, d.x + dragbarw + d3.event.dx));
  // console.log(d.x+10 + ", max of(" + detailWidth + ", " + (d.x + 20 + d3.event.dx));
  //recalculate width
  var selectedIdx = this.__data__;
  detailWidth = detailWidths[selectedIdx] + d3.event.dx;
  console.log(detailWidth);

  detailWidths[selectedIdx] = detailWidth;

  var bins = d3.selectAll("g.metric-detail-row");
  bins.selectAll("rect")
    .filter(function(d, i) { return i === selectedIdx; })
    .attr("width", detailWidth);
  bins.selectAll("rect")
    .filter(function(d, i) { return i > selectedIdx; })
    .attr("x", function(d, i) {
      return parseInt(this.attributes.x.value) + d3.event.dx;
    });

  //move the right drag handle
  dragbarbottom.filter(function(d, i) { return i === selectedIdx; })
    .attr("width", detailWidth)
    .attr("border", 1);

  var axisWidths = [];
  axisWidths.push(0);
  for (var i = 1; i <= totalEvalTuples.length; i++) {
    axisWidths.push(detailWidths[i-1] + axisWidths[i-1]);
  }

  var ordinalScale = [];
  for (var i = 0; i <= totalEvalTuples.length; i++) ordinalScale.push(i);

  var xScale = d3.scale.ordinal()
    .domain(ordinalScale)
    .range(axisWidths);

  var x = d3.scale.ordinal( )
    .domain(ordinalScale)
    .range(axisWidths);

  var xAxis = d3.svg.axis()
    .scale(xScale)
    .orient("bottom")
    .ticks(totalEvalTuples.length)
    .tickFormat(function(d) {
      if(d < totalEvalTuples.length) {
        var script = totalEvalTuples[d].evaluable;
        if (script != null) {
          var label;
          for (var i = 0; i < metricData.length; i++) {
            if (script.toLowerCase().indexOf(metricData[i].name) < 0) {
              label = script;
            } else {
              label = metricData[i].name;
              break;
            }
          }
          return label;
        }
      }
    });

  d3.select("g.x.axis").call(xAxis);
  var heatAxis = d3.select("g.x.axis");
  heatAxis
    .selectAll(".tick text")
    .style("font-size", 12)
    .call(wrap, detailWidths)
    .style("text-anchor", "start")
    .attr("x", 6)
    .attr("y", 6);

  var separatorPos = [];
  var separatorIdx = 0;
  for (var m = 0; m < metricData.length-1; m++) {
    var curM = metricData[m];
    var separatorWidth = 0;
    if(m > 0) {
      separatorWidth += separatorPos[m-1];
    }
    for (var i = 0; i < curM.evalTuples.length; i++) {
      separatorWidth += detailWidths[separatorIdx];
      separatorIdx++;
    }
    separatorPos.push(separatorWidth);
  }
  var lines = d3.selectAll("line.separator")
    .attr("x1", function(d, i) {
      return separatorPos[i];
    })
    .attr("x2", function(d, i) {
      return separatorPos[i];
    });
  //resize the drag rectangle
  //as we are only resizing from the right, the x coordinate does not need to change
}

function detaildragdone(d) {
  var axis = d3.selectAll("g.metric-detail-row g");
}

function capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
}

function lowercaseFirstLetter(string) {
    return string.charAt(0).toLowerCase() + string.slice(1);
}