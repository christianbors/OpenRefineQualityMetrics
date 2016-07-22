var contextPopover;

function renderTableHeader() {
  if(overlayModel.spanningMetrics != null) {
    overlayModel.spanningMetrics.push(overlayModel.uniqueness);
  } else if (overlayModel.uniqueness != null) {
    overlayModel.spanningMetrics = new Array(overlayModel.uniqueness);
  }
  $("#datasetHeader").empty();
  $("#datasetHeader").append('<tr><th></th><th colspan="'+columns.length+'">Multiple-Column Metrics</th></tr>');
  $.each(overlayModel.spanningMetrics, function(key, metric) {
    var spanMetricRow = '<tr class="span-metric-row">';
    spanMetricRow += '<th style="padding-left:10px; padding-top:0px; padding-bottom:0px; font-weight:normal;">' + metric.name + '</th>';
    spanMetricRow += '<td style="padding:0px;" colspan="' + columns.length + '"></td>'
    spanMetricRow += '</tr>';
    var spanMetricColumns = '<tr class="span-metric-column-row"><td></td>';
    $.each(metric.spanningColumns, function(idx, column) {
      var colspan = Math.floor(columns.length/metric.spanningColumns.length);
      if(metric.spanningColumns.length < columns.length && idx == 0) {
        colspan += 1;
      }
      spanMetricColumns += "<th colspan="+colspan+">"+column+"</th>";
    });
    spanMetricColumns += '</tr>';
    $("#datasetHeader").append(spanMetricColumns);
    $("#datasetHeader").append(spanMetricRow);
  });

  $("#datasetHeader").append('<tr><th></th><th colspan="'+columns.length+'">Single-Column Metrics</th></tr>');
  var singleColsRow = '<tr><td></td>';
  $.each(columns, function(key, value) {
    singleColsRow += '<th>'+value.name+'</th>';
  });
  singleColsRow += '</tr>';
  $("#datasetHeader").append(singleColsRow);
  $.each(overlayModel.availableMetrics, function(key, value) {
    var row = '<tr class="metric-row"><th style="padding-left:10px; padding-top:0px; padding-bottom:0px; font-weight:normal;">' + value +'</th>';
    $.each(columns, function(keyCol, col) {
      row += '<td style="padding:0px;"></td>';
    });
    row += "</tr>"
    $("#datasetHeader").append(row);
  });

  $("#datasetHeader").append(singleColsRow);
}

function renderMetricOverview() {
  tr = d3.selectAll("tr.metric-row").data(overlayModel.availableMetrics);
  colWidths = [];
  var headerCols = $(".metric-row > td");
  $.each(headerCols, function(i, header) {
    colWidths.push(header.offsetWidth);
  });

  var td = tr.selectAll("td").data(overlayModel.metricColumns);
  td.append("svg")
   .attr("width", function(d, i) {
      // return this.parentNode.scrollWidth;
      return (colWidths[i]);
    })
    .attr("height", 20)
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
  .attr("height", function(d, i) {
    return 20;
  })
  .attr("width", function(d, i) {
    if (d != null) {
      var metricCurrent = d;
      return metricCurrent.measure * this.parentNode.scrollWidth;
    }
  })
  .style("fill", function(d, i) {
    if (d != null) {
      return fillMetricColor(d.name);
    }
  });

  td.on("click", selectMetric);
  rawDataTable = $('#dataset').DataTable().columns.adjust().draw();
  updateSVGInteractions();
}

function renderSpanningMetricOverview() {
  if(overlayModel.spanningMetrics != null) {
    overlayModel.spanningMetrics.push(overlayModel.uniqueness);
  } else if (overlayModel.uniqueness != null) {
    overlayModel.spanningMetrics = new Array(overlayModel.uniqueness);
  }
  if (overlayModel.spanningMetrics != null) {
    d3.select("#spanningMetricPanel").selectAll("table").remove();
    var td = d3.selectAll(".dataTables_scrollHead thead .span-metric-row td").data(overlayModel.spanningMetrics);
    td.append("svg")
      .classed("overview-svg", true)
      .attr("width", function(d, i) {
        return $("#dataset").width();
      })
      .attr("height", 20)
    .append("rect")
      .attr("height", 20)
      .attr("width", function(d, i) {
        var width = d3.select(this.parentNode).node().clientWidth;
        return d.measure * width;
      })
      .style("fill", function(d, i) {
        if (d != null) {
          return fillMetricColor(d.name);
        }
      });
    td.on("click", selectMetric);
    rawDataTable = $('#dataset').DataTable().columns.adjust().draw();
    updateSVGInteractions();
  }
}

function updateSVGInteractions() {
  var svgs = d3.selectAll(".overview-svg");

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
  contextPopover = $("svg.overview-svg").popover({
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
    redrawDetailView(theProject, metricData, rowModel);

    // for (var colI = 0; colI < columnStore.length; colI++) {
    //   var colCurrent = rawDataTable.column(colI).visible(true);
    //   $("#overlay").show();
    // }
    // for (var i = 0; i < metricData.length; i++) {
    //   for (var colI = 0; colI < columnStore.length; colI++) {
    //     if($.inArray(columnStore[colI].title, metricData[i].spanningColumns) == -1) {
    //       var colCurrent = rawDataTable.column(colI);
    //       colCurrent.visible(false);
    //       $("#overlay").hide();
    //     }
    //   }
    // }
    // for (var i = 0; i < metricData.length; i++) {
    //   if(metricData[i].spanningColumns == null) {
    //     for (var colI = 0; colI < columnStore.length; colI++) {
    //       var colCurrent = rawDataTable.column(colI).visible(true);
    //       $("#overlay").show();
    //     }
    //   }
    // }
    rawDataTable.column(0).visible(true);

  }
}

function drawDatatableScrollVis(theProject, rowModel, columnStore) {
  d3.select("div.dataTables_scrollBody").append("svg").attr("id", "overlay");
  $("#overlay").css({top: 0, 
    left: 0,
    position:'absolute', 
    width: $(".dataTables_scrollBody > table").width(), 
    height: $(".dataTables_scrollBody").height(),
    'pointer-events': 'none'
  });

  var colWidths = [];
  // $.each($("#dataset > thead > tr > th"), function(i, header) {
  //   colWidths.push(header.offsetWidth);
  // });
  var headerCols = $("#dataset > tbody > tr").first().find("td");
  $.each(headerCols, function(i, header) {
    colWidths.push(header.offsetWidth);
  });

  //this determines the width offset of the overlay
  var colWidthsCalc = [];
  var firstWidth = (colWidths[0] + colWidths[1])-1;
  colWidthsCalc.push(firstWidth);
  // colWidths[0] = colWidths[0] + $(".metric-row").first().find("th").outerWidth();
  for(var i = 1; i < colWidths.length-1; i++) {
    colWidthsCalc.push((colWidthsCalc[i-1] + colWidths[i+1]));
  };

  var overlayX = d3.scale.ordinal().range(colWidthsCalc);

  overlayY = d3.scale.linear()
    .domain([rowModel.filtered, 0])
    .rangeRound([$(".dataTables_scrollBody").height(), 0]);

  var minScale = 3;
  if (overlayModel.availableMetrics.length > 2) minScale = overlayModel.availableMetrics.length;

  // d3.select("#overlay").attr("transform", "translate(0, -" + $("#dataset_wrapper")[0].clientHeight + ")");
  var overlay = d3.select("#overlay").selectAll(".metrics-overlay")
    .data(overlayModel.metricColumns)
    .enter().append("g")
    .attr("class", "metrics-overlay")
    .attr("transform", function (d, i) {
      var translate = 0;
      if (d != null) {
        $.each(d.metrics, function(idx, metric) {
          if(metric.dirtyIndices != null) {
            translate++;
          }
        });
      }
      return "translate(" + colWidthsCalc[i] + ",0)";
    })
    .attr('pointer-events', 'all');

  var cols = overlay.selectAll(".metrics-overlay-col")
    .data(function(d) {
      if (d != null) {
        var array = $.map(d.metrics, function(value, index) {
            return [value];
        });
        return array;
      } else {
        return [];
      }
    })
    .enter().append("g")
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
    })
    .attr("display", function(d, i) {
      if(d.dirtyIndices == null) return "none";
    });

  cols.append("rect")
    .attr("height", $(".dataTables_scrollBody").height())
    .attr("width", 12)
    .attr("fill", function(d) {
      if (d.dirtyIndices != null) {
        return "white";
      } else {
        return "transparent";
      }
    });

  var bins = cols.selectAll(".metrics-bin")
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
  }).style("fill", function(d, i) {
    var current = this.parentNode.__data__;
    return fillMetricColor(current.name);
    // return z(overlayModel.availableMetrics.indexOf(current.name));
  });
  bins.call(tooltipInvalid);

  bins.each(function (d) {
    var ys = d3.select(this)
      .attr("y", overlayY(d.index))
      .attr("height", 1);
  });

  bins.on("click", selectRow);

  bins.on("mouseover", function(d) {
    d3.select(this).style("fill", "steelblue");
    var selThis = this;
    var sameRows = d3.select(this.parentNode).selectAll("rect").filter(function(r) {
      return d3.select(this).attr("y") === d3.select(selThis).attr("y");
    })[0];
    if(sameRows.length > 1) {
      var indices = {
        first: sameRows[0].__data__.index,
        last: sameRows[sameRows.length-1].__data__.index
      };
      tooltipInvalid.show(indices);
    } else {
      tooltipInvalid.show(d.index)
    }
    $.each(sameRows, function(i, rowCurrent) {
      $("#dataset").DataTable().row(rowCurrent.__data__.index).node().classList.add("hover");
    });
  });

  bins.on("mouseout", function(d) {
    $("#dataset tr").removeClass("hover");
    d3.select(this).style("fill", function(d, i) {
      var metricsCol = this.parentNode.parentNode.__data__;
      var current = this.parentNode.__data__;
      return fillMetricColor(current.name);
      // for(var idx = 0; idx < overlayModel.availableMetrics.length; idx++) {
      //   if(overlayModel.availableMetrics[idx].name === current.name) {
      //     return z(idx);
      //   }
      // }
    });
    tooltipInvalid.hide();
  });
  
  overlay.append("line")
    .attr("x1", function(d) {
      var attr = this.previousSibling.attributes["transform"];
      var transl = attr.value.match(/\d+/);
      var offset = parseInt(transl[0]);
      return "-" + (offset+1);
    })
    .attr("x2", function(d) {
      var attr = this.previousSibling.attributes["transform"];
      var transl = attr.value.match(/\d+/);
      var offset = parseInt(transl[0]);
      return "-" + (offset+1);
    })
    .attr("y1", 0)
    .attr("y2", $(".dataTables_scrollBody").height())
    .attr("stroke", "#ddd")
    .attr("stroke-width", "2");

  overlay.append("line")
    .attr("x1", 0)
    .attr("x2", 0)
    .attr("y1", 0)
    .attr("y2", $(".dataTables_scrollBody").height())
    .attr("stroke", "#ddd")
    .attr("stroke-width", "2");


  $(".dataTables_scrollBody").scroll(function() {
    $('#overlay').css({
        'top': $(this).scrollTop() 
         //Why this 15, because in the CSS, we have set left 15, so as we scroll, we would want this to remain at 15px left
    });
  });
  // var headers = d3.select("#raw-data-container").select("#dataset_wrapper").select(".dataTables_scroll").select(".dataTables_scrollBody");//.selectAll("td").data(overlayModel.metricColumns);
  // var svg = headers.insert("svg", "#dataset")
  //   .attr("class", "overlay")
  //   .attr("width", $(".dataTables_scrollBody").width())
  //   .attr("height", $(".dataTables_scrollBody").height())
  //   .attr("top", tablePos.top)
  //   .attr("left", tablePos.left);
}

function updateOverlayPositions() {
  $('#dataset').DataTable().columns.adjust().draw();
  var colWidths = [];
  // $.each($("#dataset > thead > tr > th"), function(i, header) {
  //   colWidths.push(header.offsetWidth);
  // });
  var headerCols = $("#dataset > tbody > tr").first().find("td");
  $.each(headerCols, function(i, header) {
    colWidths.push(header.offsetWidth);
  });

  //this determines the width offset of the overlay
  var colWidthsCalc = [];
  var firstWidth = (colWidths[0] + colWidths[1])-1;
  colWidthsCalc.push(firstWidth);
  // colWidths[0] = colWidths[0] + $(".metric-row").first().find("th").outerWidth();
  for(var i = 1; i < colWidths.length-1; i++) {
    colWidthsCalc.push((colWidthsCalc[i-1] + colWidths[i+1]));
  };

  var overlay = d3.select("#overlay").selectAll(".metrics-overlay")
    .attr("transform", function (d, i) {
      var translate = 0;
      if (d != null) {
        $.each(d.metrics, function(idx, metric) {
          if(metric.dirtyIndices != null) {
            translate++;
          }
        });
      }
      return "translate(" + colWidthsCalc[i] + ",0)";
    });
}