function renderMetricOverview() {
  // tr.selectAll("td").remove();
  var td = tr.selectAll("td").data(overlayModel.metricColumns).enter().append("td");
  td.append("svg")
  .attr("width", function(d, i) {
  return (colWidths[i]-1);
  })
  .attr("height", 12)
  .classed("overview-svg", true)
  .attr("data-toggle", "popover")
  .append("rect")
  .data(function(d, i) {
  var metrics = [];
  for (var m = 0; m < overlayModel.metricColumns.length; m++) {
  var metric = overlayModel.metricColumns[m].metrics[d];
  if(metric != null) metric.columnName = overlayModel.metricColumns[m].columnName;
  metrics.push(metric);
  }
  return metrics;
  })
  .classed("metric", true)
  .attr("height", 12)
  .attr("width", function(d, i) {
  if (d != null) {
  var metricCurrent = d;
  return metricCurrent.measure * (colWidths[i]-1);
  }
  })
  .style("fill", function(d, i) {
  if (d != null) {
  return fillMetricColor(d.name);
  }
  });

  td.on("click", selectMetric);
  updateSVGInteractions();
}

function renderSpanningMetricOverview() {
  if(overlayModel.spanningMetrics != null) {
    overlayModel.spanningMetrics.push(overlayModel.uniqueness);
  } else {
    overlayModel.spanningMetrics = new Array(overlayModel.uniqueness);
  }

  d3.select("#spanningMetricPanel").selectAll("table").remove();
  var spanningTables = d3.select("#spanningMetricPanel").selectAll("table").data(overlayModel.spanningMetrics).enter().append("table").each(function(d, i) {
  var table = d3.select(this).attr("table-layout", "fixed")
    .attr("width", $("#overviewTable").width())
    .attr("id", "spanningOverviewTable")
    .attr("class", "overviewTable")
    .style("margin-bottom", 10);
  var hrow = table.append("thead").append("tr");
  hrow.append("th");
  var spanColsCurrent = d.spanningColumns;
  // hrow.selectAll("td").data(d.spanningColumns).enter().append("td").text(function(d) { return d; });
  hrow.selectAll("td").data(d.spanningColumns).enter().append("td")
    .text(function(d) { 
      return d; 
    })
    .attr("width", function(d, i) { 
      return $("#overviewTable").width()/spanColsCurrent.length;
    });

  var trow = table.append("tbody").append("tr");
  trow.append("th").text(function(d) { 
      return d.name; 
    })
    .attr("width", function(d, i) { 
      return $("#overviewTable > thead > tr > th").outerWidth(); 
    });

  trow.append("td").attr("colspan", columns.length);
  trow.selectAll("td").append("svg")
    .classed("overview-svg", true)
    .attr("width", function(d, i) {
      return $("#dataset").width();
    })
    .attr("height", 6)
    .append("rect")
    .attr("height", 6)
    .attr("width", function(d, i) {
  var width = d3.select(this.parentNode).node().clientWidth;
      return d.measure * width;
    })
    .style("fill", function(d, i) {
      if (d != null) {
        return fillMetricColor(d.name);
      }
    });
    trow.select("td").on("click", selectMetric);
  });

  updateSVGInteractions();
}

function updateSVGInteractions() {
  var svgs = d3.selectAll(".overview-col svg");

  svgs.call(tooltipOverview);
  svgs.on("mouseover", function(d) {
    if (d.spanningEvaluable != null) {
      tooltipOverview.show(d);
    } else {
      tooltipOverview.show(d.metrics[this.parentNode.parentNode.__data__]);
    };
  }).on("mouseout", function(d) {
    tooltipOverview.hide();
  });
  svgs.on("contextmenu", function(d, i) {
    if (d.spanningEvaluable != null) {
      contextMetric = d;
    } else {
      contextMetric = d.metrics[this.parentNode.parentNode.__data__];
    }
    d3.event.preventDefault();
    tooltipOverview.hide();
    
    contextColumn = d.columnName;
    var _this = this;
    if(selectedColName.length > 1) {
      $(this).data("bs.popover").options.content = '<div class="btn-group" role="group"><button type="button" class="btn btn-danger" id="remove-metric">Remove</button>'+
      '<button type="button" class="btn btn-default" id="merge-metric">Merge</button>'+
      '<button type="button" class="btn btn-default" id="duplicate-metric">Duplicate</button></div>';
    } else {
      $(this).data("bs.popover").options.content = '<div class="btn-group" role="group"><button type="button" class="btn btn-danger" id="remove-metric">Remove</button>'+
      '<button type="button" class="btn btn-default" id="duplicate-metric">Duplicate</button></div>';
    }
    $(this).popover("toggle");

    $(".popover").on("mouseleave", function () {
      $(_this).popover('hide');
    });
  });
  $("svg.overview-svg").popover({
    html: 'true',
    trigger: 'manual',
    placement: 'auto top',
    animation: 'false',
    container: 'body',
    content: '<div class="btn-group" role="group"><button type="button" class="btn btn-danger" id="remove-metric">Remove</button>'+
      '<button type="button" class="btn btn-default" id="duplicate-metric">Duplicate</button></div>'
  });
}

function selectMetric(d) {
  var metric;
  if(d.spanningEvaluable != null) {
    metric = d;
  } else {
  metric = d.metrics[this.parentNode.__data__];
  }

  if (d3.event.shiftKey) {
  contextColumn = null;
  } else {
  if (selectedOverviewRect != null) {
    metricType = [];
    selectedOverviewRect = [];
      selectedColName = [];
      metricData = [];
      d3.selectAll(".selected").classed("selected", false);
  }
  }

  metricData.push(metric);
  if(d.spanningEvaluable != null) {
    metricType.push("spanning");
    selectedColName.push(d.spanningColumns);
  } else {
    metricType.push("single");
    selectedColName.push(d.columnName);
  }
  
  selectedOverviewRect.push(d3.select(this).attr("class", "selected"));

  totalEvalTuples = [];
  selectedChecks = [];
  for (var i = 0; i < metricData.length; i++) {
    var enabledTuples = metricData[i].evalTuples.filter(function(d) {
      return !d.disabled;
      });
      if(metricData[i].spanningEvaluable != null) {
      totalEvalTuples.push(metricData[i].spanningEvaluable);
        selectedChecks.push(metricData[i].name);
      }
    totalEvalTuples.push.apply(totalEvalTuples, enabledTuples);
    for(var j = 0; j < enabledTuples.length; j++) {
      selectedChecks.push(metricData[i].name);
    }
  }
  if(metricData.length > 0) {
    refillEditForm(metricData, selectedColName);
    fillLegend();
    redrawDetailView(theProject, metricData, rowModel, overlayModel);

    for (var colI = 0; colI < columnStore.length; colI++) {
      var colCurrent = rawDataTable.column(colI).visible(true);
      $("#overlay").show();
    }
    for (var i = 0; i < metricData.length; i++) {
      for (var colI = 0; colI < columnStore.length; colI++) {
        if($.inArray(columnStore[colI].title, metricData[i].spanningColumns) == -1) {
          var colCurrent = rawDataTable.column(colI);
          colCurrent.visible(false);
          $("#overlay").hide();
        }
      }
    }
    for (var i = 0; i < metricData.length; i++) {
      if(metricData[i].spanningColumns == null) {
        for (var colI = 0; colI < columnStore.length; colI++) {
          var colCurrent = rawDataTable.column(colI).visible(true);
          $("#overlay").show();
        }
      }
    }

  }
}