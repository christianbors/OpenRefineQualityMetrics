var theProject,
    overlayModel,
    rowModel;
var rawDataTable;
var detailWidth,
    dragbarbottom,
    dragheight = 20,
    dragbarw = 20;
var detailWidths;
var metricType = [],
    selectedChecks = [],
    selectedOverviewRect = [],
    selectedColName = [],
    selectedCol = [],
    selectedColOpacity = [],
    metricData = [],
    totalEvalTuples = [],
    rowIndex = [];
// overview vis
var colWidths;
// data overlay vis
var overlayY;
// detail view dimensions
var detailViewHeight,
    detailViewWidth,
    detailViewMargin;
// context menu
var contextMetric,
    contextColumn;
// coloring scale should be defined globally
var z;
var selectedEditEvaluable;
var dataSet = [];
var columnStore = [];
var columnsStore = [];
var colWidth = 50;

var dataTypes;

var datatablesHeader,
    rawDataHeight;

var selectedMetricModal;

// selection for creating a metric
var columnForMetricToBeCreated;
    metricToBeCreated = [];

var tooltipInvalid = d3.tip()
  .attr("class", "d3-tip")
  .offset([-10, 0])
  .html(function(d) {
    if(d != null && d.first != null) {
      return "<strong>Rows:</strong> <span style='color:steelblue'>" + d.first + " - " + d.last + "</span>";
    } else {
      return "<strong>Row:</strong> <span style='color:steelblue'>" + d + "</span>";
    }
  });
var tooltipOverview = d3.tip()
  .attr("class", "d3-tip")
  .offset([-10, 0])
  .html(function(d) {
    if (d != null) {
      var text = "<span style='color:steelblue'>" + capitalizeFirstLetter(d.name) + "</span><br>" +
        "<strong>Metric Value:</strong> <span style='color:steelblue'>" + d.measure + "</span><br>" + 
        "<strong>Number of Checks:</strong> <span style='color:steelblue'>" + d.evalTuples.length + "</span><br>";
      if(d.dirtyIndices != null) {
        text += "<strong>Erroneous Entries:</strong> <span style='color:steelblue'>" + d.dirtyIndices.length + "</span><br>";
      }
      text += "<strong>Data Type:</strong> <span style='color:steelblue'>" + d.datatype + "</span>";
      return text;
    }
  });

var filterFunction = function (oSettings, aData, iDataIndex) {
  var gs = d3.selectAll("g.metric-detail-row").filter(function(d, i) {
    return d.index === iDataIndex;
  });
  return gs[0].length > 0;
};

$(document).ready(function() {

  URL.getParameters = function () {
    var r = {};

    var params = window.location.search;
    if (params.length > 1) {
        params = params.substr(1).split("&");
        $.each(params, function () {
                pair = this.split("=");
                r[pair[0]] = unescape(pair[1]);
            });
    }

    return r;
  };

  theProject = { id : URL.getParameters().project};

  $.getJSON(
    "../../command/core/get-project-metadata?" + $.param({ project: theProject.id }), null,
    function(data) {
      if (data.status == 'error') {
        alert(data.message);
        if (fError) {
          fError();
        }
      } else {
        theProject.metadata = data;
        $.getJSON(
          "../../command/core/get-models?" + $.param({ project: theProject.id }), null,
          function(data) {
            for (var n in data) {
              if (data.hasOwnProperty(n)) {
                theProject[n] = data[n];
              }
            }

            $.post(
              "../../command/core/get-rows?" + $.param({ project: theProject.id, start: 0 }) + "&callback=?",
              [],
              function(data) {
                theProject["row-model"] = data;
                $.post(
                "../../command/core/get-rows?" + $.param({ project: theProject.id, start: 0, limit: data.filtered }) + "&callback=?",
                [],
                function(data) {
                  rowModel = data;

                  // Un-pool objects
                  var columns = theProject.columnModel.columns;
                  for (var r = 0; r < data.rows.length; r++) {
                    var row = data.rows[r];
                    var rowValues = [];
                    $.each(columns, function(index, value) {
                      if (value != null) {
                        var cell = row.cells[value.cellIndex]
                        if (cell != null) {
                          rowValues.push(cell.v);
                        } else {
                          rowValues.push('');
                        }
                      }
                    });
                    dataSet.push(rowValues);
                  }

                  columnStore = [];
                  $.each(columns, function(index, value) {
                    rowIndex.push(index);
                    columnStore[index] = {"title": value.name};
                  });

                  var dataCols = dataSet[0];

                  $('#dataset').dataTable( {
                    "data": dataSet,
                    "columns": columnStore,
                    // "scrollX": true,
                    "sScrollY": "350px",
                    "scrollCollapse": true,
                    "paging": true,
                    "scroller": true,
                    "bSort": false,
                    "bFilter": true,
                    "dom": 'rt<"bottom"i><"clear">',
                    "bAutoWidth": true
                  });

                  $.each(columnStore, function(key, value){
                    $("#columnFormMetricModal").append('<option value="' + value.title + '">' + value.title + '</option>');
                    $("#columnDuplicateModal").append('<option value="' + value.title + '">' + value.title + '</option>');
                  });

                  $("#columnFormMetricModal").attr("size", columnStore.length);

                  // modal on click change
                  $( "#columnFormMetricModal" ).change(function() {
                    fillModalAfterColumnSelection(theProject);
                  });

                  $.getJSON(
                    "../../command/metric-doc/getMetricsOverlayModel?" + $.param({ project: theProject.id }), 
                    null,
                    function(data) {
                      overlayModel = data;
                      var margin = 20;

                      $('#uniqueness > tbody:last-child').append("<tr><td>" + overlayModel.uniqueness.name + "</td></tr>");

                      datatablesHeader = $(".dataTables_scrollHead").height();
                      rawDataHeight = $('#raw-data-container').height();

                      //this reorders the metrics to be in line with the actual displayed columns
                      var sortedMetrics = new Array();
                      for(var idx = 0; idx < theProject.columnModel.columns.length; idx++) {
                        var foundColumn = overlayModel.metricColumns.filter(function(col) {
                          if (columnStore[idx] != null) {
                            return col.columnName == columnStore[idx].title;
                          }
                        })[0];
                        if (foundColumn != null) {
                          sortedMetrics[idx] = foundColumn;
                        } else {
                          sortedMetrics[idx] = null;
                        }
                      }
                      overlayModel.metricColumns = sortedMetrics;

                      var tr = d3.select("#overviewTable").select("tbody").selectAll("tr").data(overlayModel.availableMetrics).enter().append("tr");
                      tr.append("th").text(function(d) { return d; });
                      var td = tr.selectAll("td").data(overlayModel.metricColumns).enter().append("td");


                      var minScale = overlayModel.availableMetrics.length + overlayModel.availableSpanningMetrics.length;
                        
                      z = d3.scale.ordinal()
                        .range(colorbrewer.YlOrRd[minScale+1])
                        .domain([minScale, 0]);

                      $("#raw-data-container").css({marginLeft: $("#overviewTable > thead > tr > th").outerWidth()});
                      $('#detailViewHeader').css('marginTop', 0 + 'px')
                        .css('marginBottom', 0 + 'px');

                      rawDataTable = $('#dataset').DataTable().columns.adjust().draw();
                      var columns = theProject.columnModel.columns;
                      for (var col = 0; col < columns.length; col++) {
                        var column = columns[col];
                        columnStore[column.cellIndex] = {"title": column.name};
                        columnsStore.push(column.name);
                      }
                      $("#overviewPanel").css({height: $("#overviewTable").height() + margin});
                      
                      colWidths = [];
                      $.each($("#dataset > tbody > tr")[0].children, function(i, header) {
                        colWidths.push(header.offsetWidth);
                      });

                      var overviewTable = d3.select("#overviewTable").select("thead tr")
                        .selectAll('td')
                        .data(columns).enter()
                        .append('td')
                        .attr("width", function(d, i) {
                          return colWidths[i];
                      }).text(function(col) { 
                          return col.name; 
                      }).attr("data-toggle", "popover")
                        .on("contextmenu", addMetricToColumn);

                      $("#overviewTable thead tr td").popover({
                        html: 'true',
                        trigger: 'manual',
                        placement: 'auto top',
                        animation: 'false',
                        container: 'body',
                        content: ''
                      });

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

                      if(overlayModel.spanningMetrics != null) {
                        overlayModel.spanningMetrics.push(overlayModel.uniqueness);
                      } else {
                        overlayModel.spanningMetrics = new Array(overlayModel.uniqueness);
                      }

                      // var utr = d3.select("#uniquenessTable").select("tbody").selectAll("tr").data(uniquenessArray).enter().append("tr");
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
                        hrow.selectAll("td").data(d.spanningColumns).enter().append("td").text(function(d) { 
                            return d; 
                        }).attr("width", function(d, i) { 
                          return $("#overviewTable").width()/spanColsCurrent.length;
                        });

                        var trow = table.append("tbody").append("tr");
                        trow.append("th").text(function(d) { return d.name; })
                          .attr("width", function(d, i) { return $("#overviewTable > thead > tr > th").outerWidth(); });

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

                        trow.select("td").select("svg").call(tooltipOverview);
                        trow.select("td").select("svg").on("mouseover", function(d) {
                          if (d != null) {
                              tooltipOverview.show(d);
                          };
                        }).on("mouseout", function(d) {
                          tooltipOverview.hide();
                        });
                        trow.select("td").on("click", function(d) {
                          if (d3.event.shiftKey) {
                            contextColumn = null;
                          } else {
                            if (selectedOverviewRect != null) {
                              metricType = [];
                              selectedOverviewRect = [];
                              selectedColName = [];
                              selectedColIdx = [];
                              metricData = [];
                              d3.selectAll(".selected").classed("selected", false);
                            }
                          }
                          var spanColIdx = -1;
                          for(var idx = 0; idx < overlayModel.availableSpanningMetrics.length; idx++) {
                            if(overlayModel.availableSpanningMetrics[idx].name === lowercaseFirstLetter(d.name)) {
                              spanColIdx = overlayModel.availableMetrics.length + idx;
                            }
                          }
                          if(spanColIdx == -1) spanColIdx = overlayModel.availableSpanningMetrics.length + overlayModel.availableSpanningMetrics.length + 1;

                          metricData.push(d);
                          metricType.push("spanning");
                          selectedColName.push(d.spanningColumns);
                          selectedOverviewRect.push(d3.select(this).attr("class", "selected"));
                          selectedColIdx.push(columnsStore.indexOf(d.spanningColumns[0]));

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

                            rawDataTable = $('#dataset').DataTable();

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
                          }

                        });

                        trow.select("td").select("svg").on("contextmenu", function(d, i) {
                          d3.event.preventDefault();
                          tooltipOverview.hide();
                          contextMetric = d;
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
                      });

                      td.on("click", function(d) {
                        if (d != null) {
                          var rowMetric = this.parentNode.__data__;
                          if (d3.event.shiftKey) {
                            contextColumn = null;
                          } else {
                            if (selectedOverviewRect != null) {
                              metricType = [];
                              selectedOverviewRect = [];
                              selectedColName = [];
                              metricData = [];
                              selectedColIdx = [];
                              d3.selectAll(".selected").classed("selected", false);
                            }
                          }
                          selectedOverviewRect.push(d3.select(this).attr("class", "selected"));
                          metricType.push("single");
                          selectedColName.push(d.columnName);
                          selectedColIdx.push(columnsStore.indexOf(d.columnName));
                          metricData.push(d.metrics[rowMetric]);

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
                            
                            for (var i = 0; i < metricData.length; i++) {
                              if(metricData[i].spanningColumns == null) {
                                for (var colI = 0; colI < columnStore.length; colI++) {
                                  var colCurrent = rawDataTable.column(colI).visible(true);
                                  $("#overlay").show();
                                }
                              }
                            }

                            redrawDetailView(theProject, metricData, rowModel, overlayModel);
                          }
                        }
                      });
                      td.select("svg").call(tooltipOverview);
                      td.select("svg").on("mouseover", function(d) {
                        if (d.metrics[this.parentNode.parentNode.__data__] != null) {
                          tooltipOverview.show(d.metrics[this.parentNode.parentNode.__data__]);
                        };
                      }).on("mouseout", function(d) {
                        tooltipOverview.hide();
                      });
                      td.select("svg").on("contextmenu", function(d, i) {
                        if (d.metrics[this.parentNode.parentNode.__data__] != null) {
                          d3.event.preventDefault();
                          tooltipOverview.hide();
                          contextMetric = d.metrics[this.parentNode.parentNode.__data__];
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
                        }
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

                      drawDatatableScrollVis(theProject, rowModel, columnStore, overlayModel);
                      //todo: edit when selecting other metric
                      dataViewPopover();
                    }, 
                    'json'
                  );
                },
                "jsonp");
              },
              "jsonp"
            );
          },
          'json'
        );
      }
    },
    'json'
  );
  // Sortable.create(checksList, { /* options */ });
});

function drawDatatableScrollVis(theProject, rowModel, columnStore, overlayModel) {
  var tablePos = $(".dataTables_scrollBody").position();
  $("#overlay").css({top: tablePos.top, 
    left: tablePos.left,
    position:'absolute', 
    width: $(".dataTables_scrollBody").width(), 
    height: $(".dataTables_scrollBody").height()
  });

  var colWidths = [];
  $.each($("#dataset > thead > tr > th"), function(i, header) {
    colWidths.push(header.offsetWidth);
  });

  //this determines the width offset of the overlay
  colWidths[0] = colWidths[0];
  for(var i = 1; i < colWidths.length; i++) {
    colWidths[i] = colWidths[i] + colWidths[i-1];
  };

  var overlayX = d3.scale.ordinal().range(colWidths);

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
      return "translate(" + colWidths[i] + ",0)";
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

  // var headers = d3.select("#raw-data-container").select("#dataset_wrapper").select(".dataTables_scroll").select(".dataTables_scrollBody");//.selectAll("td").data(overlayModel.metricColumns);
  // var svg = headers.insert("svg", "#dataset")
  //   .attr("class", "overlay")
  //   .attr("width", $(".dataTables_scrollBody").width())
  //   .attr("height", $(".dataTables_scrollBody").height())
  //   .attr("top", tablePos.top)
  //   .attr("left", tablePos.left);
}

function refillEditForm(d, colName) {
  $("#addCheckFooter").show();
  $("#metricInfoDetailHeader").text("Metric Detail - " + capitalizeFirstLetter(d[0].name) + " - " + selectedColName[0]);
  $("#metricName").text(d[0].name);
  $("#metricDescription").text(d[0].description);
  var evalLength = d[0].evalTuples.length;
  if(d[0].spanningEvaluable != null) {
    evalLength += 1;
  }
  $("#checksHeaderText").text("Checks (" + evalLength + ")");

  $(".metricCheck").remove();
  $("#typeDetail").remove();
  
  if (d[0].datatype.indexOf("numeric") > -1) {
    $("#dataTypeNumeric").prop('checked', true);
  } else {
    $("#dataTypeNumeric").prop('checked', false);
  }
  if (d[0].datatype.indexOf("string") > -1) {
    $("#dataTypeString").prop('checked', true);
  } else {
    $("#dataTypeString").prop('checked', false);
  }
  if (d[0].datatype.indexOf("datetime") > -1) {
    $("#dataTypeDateTime").prop('checked', true);
  } else {
    $("#dataTypeDateTime").prop('checked', false);
  }
  if (d[0].datatype.indexOf("categoric") > -1) {
    $("#dataTypeCategoric").prop('checked', true);
  } else {
    $("#dataTypeCategoric").prop('checked', false);
  }

  $("#concat button").removeClass('active');
  $("#concat" + d[0].concat).addClass('active');

  if (d[0].evalTuples.length > 0) {
    var metric = d[0].evalTuples[0].evaluable;
    metric = metric.substr(0, metric.indexOf("("));
    $("#base" + metric).prop("checked", true);
  }
  if (d[0].spanningEvaluable != null) {
    addEvaluableEntry(d[0].spanningEvaluable);
  }
  if (d[0].evalTuples.length > 0) {
    for (var i = 0; i < d[0].evalTuples.length; i++) {
      addEvaluableEntry(d[0].evalTuples[i]);
    }
  }
}

function fillLegend() {
  $('#detailMetricHeader').empty();
  $('#detailColumnHeader').text("Detail View")
    .append(' <button type="button" class="btn btn-info btn-xs">Info</button>');
  if(metricData.length == 1) {
    $('#detailMetricHeader').append("<h5>Selected Metric:</h5>");
  } else {
    $('#detailMetricHeader').append("<h5>Selected Metrics:</h5>");
  }
  for(var indexCount = 0; indexCount < metricData.length; indexCount++) {
    $('#detailMetricHeader').append("<h5 class='metric'></h5>");
  }
  var mHeaders = d3.selectAll("#detailMetricHeader h5.metric");
  mHeaders.append("svg")
    .attr("marginRight", 8)
    .attr("height", 12)
    .attr("width", 16)
    .append("rect")
    .attr("height", 12)
    .attr("width", 12)
    .attr("fill", function(d, i) {
      return fillMetricColor(metricData[i].name);
    });
  mHeaders.append("text").text(function(d, i) {
    // overlayModel.availableMetrics.indexOf(metricData[i].name)
    if(overlayModel.availableMetrics.indexOf(metricData[i].name) != -1) {
      return metricData[i].columnName + " - " + capitalizeFirstLetter(metricData[i].name);
    } else {
      if(overlayModel.availableSpanningMetrics.indexOf(metricData[i].name) != -1) {
        return metricData[i].columnName + " - " + capitalizeFirstLetter(metricData[i].name);
      } else {
        return metricData[i].columnName + " - Uniqueness";
      }
    }
  })
}

function fillModalAfterColumnSelection(theProject) {
  var selectedCols = $("#columnFormMetricModal").val();
  $("#metricSelectMetricModal .btn").remove();
  d3.select("#typeDetailModal").select("tbody").selectAll("tr").remove();
  d3.select("#typeDetailModal").select("thead tr").selectAll("td").remove();

  dataTypes = [];
  var metrics;
  var params = { 
    project: theProject.id, 
    columns: $("#columnFormMetricModal").val(), 
  };
  $.post("../../command/metric-doc/evaluateDataTypes?" + $.param(params) + "&callback=?",
    function(data) {
        var tr = d3.select("#typeDetailModal").select("tbody").selectAll("tr")
          .data(data)
          .enter()
          .append("tr");
        tr.append("th")
          .text(function(d) {
            return d.type;
          });
        if (selectedCols.length > 1) {
          d3.select("#typeDetailModal").select("thead tr")
            .selectAll('td')
            .data(selectedCols).enter()
            .append('td')
            .text(function(d) { 
              return d; 
            })
        }
        for(var selectedColIdx = 0; selectedColIdx < selectedCols.length; selectedColIdx++) {
          tr.append("td")
            .append("svg")
            .attr("width", 71)
            .attr("height", 12)
            .append("rect")
            .attr("width", function(d) {
              return (d.val[selectedColIdx]/rowModel.filtered) *71;
            })
            .attr("height", 12)
            .style("fill", "steelblue");
          tr.append("td")
            .text(function(d) {
              return d.val[selectedColIdx];
            })
        }
        for(var typeIdx = 0; typeIdx < data.length; typeIdx++) {
          for(var colIdx = 0; colIdx < selectedCols.length; colIdx++) {
            if(dataTypes[selectedCols[colIdx]] == null) {
              dataTypes[selectedCols[colIdx]] = data[typeIdx];
            }
            if(data[typeIdx].val[colIdx] > dataTypes[selectedCols[colIdx]].val[colIdx]) {
              dataTypes[selectedCols[colIdx]] = data[typeIdx];
            }
          }
        }
    });
  if(selectedCols.length == 1) {
    metrics = overlayModel.availableMetrics;
  } else {
    metrics = overlayModel.availableSpanningMetrics;
  }

  $.each(metrics, function(key, value) {
      var cl = "btn btn-default";
      if(selectedCols.length == 1) {
        for(var columnIndex = 0; columnIndex < overlayModel.metricColumns.length; columnIndex++) {
          if(overlayModel.metricColumns[columnIndex].columnName === selectedCols[0]) {
            var selectedMetrics = overlayModel.metricColumns[columnIndex].metrics;
            var selectedMetricsArray = $.map(selectedMetrics, function(value, index) {
              return [value];
            });
            for (var i = 0; i < selectedMetricsArray.length; i++) {
              if(selectedMetricsArray[i].name == value) {
                cl += " disabled";
                break;
              }
            }
          }
        }
      } else if (selectedCols.length > 1) {
        var spanningMetrics = overlayModel.spanningMetrics;
        if (spanningMetrics != null) {
          for (var i = 0; i < spanningMetrics.length; i++) {
            if (value == spanningMetrics[i].name) {
              var disable = true;
              for (var j = 0; j < selectedCols.length; j++) {
                var colIdx = spanningMetrics[i].spanningColumns.indexOf(selectedCols[j]);
                if (colIdx < 0) {
                  disable = false;
                  break;
                }
              }
              if(disable) {
                cl += " disabled";
                break;
              }
            }
          }
        }
      }
      $("#metricSelectMetricModal").append('<button type="button" value="' + value + '" class="' + cl + '">'+ capitalizeFirstLetter(value) + '</button>');
      $("#metricSelectMetricModal > button").on("click", function() {
        $("#metricSelectMetricModal").val($(this).text());
      });
    });
}