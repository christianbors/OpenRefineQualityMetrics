var theProject,
    rowModel;
var detailWidth,
    dragbarbottom,
    dragheight = 20,
    dragbarw = 20;
var detailWidths;
var selectedMetricIndex = [0]
    selectedOverviewRect = [],
    selectedColName = [],
    selectedColOpacity = [],
    metricData = [],
    totalEvalTuples = [];
//context menu
var contextMetric,
    contextColumn;
// coloring scale should be defined globally
var z;
var selectedEditEvaluable;
var dataSet = [];
var columnStore = [];
var colWidth = 50;

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
      return "<span style='color:steelblue'>" + capitalizeFirstLetter(d.name) + "</span><br>" +
        "<strong>Metric Value:</strong> <span style='color:steelblue'>" + d.measure + "</span><br>" + 
        "<strong>Number of Checks:</strong> <span style='color:steelblue'>" + d.evalTuples.length + "</span><br>" + 
        "<strong>Data Type:</strong> <span style='color:steelblue'>" + d.datatype + "</span>";
    }
  });

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
                    columnStore[index] = {"title": value.name};
                  });

                  var dataCols = dataSet[0];

                  $('#dataset').dataTable( {
                    "data": dataSet,
                    "columns": columnStore,
                    "scrollY": "500",
                    "scrollCollapse": true,
                    "paging": true,
                    "scroller": true,
                    "bSort": false,
                    "dom": 'rt<"bottom"i><"clear">'
                  });

                  $.each(columnStore, function(key, value){
                    $("#columnFormMetricModal").append('<option value="' + value.title + '">' + value.title + '</option>');
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
                      var overlayModel = data;
                      var margin = 20;

                      $('#uniqueness > tbody:last-child').append("<tr><td>" + overlayModel.uniqueness.name + "</td></tr>");

                      $("#recalculate").on("click", function(d) {
                        // recalculate
                        // Refine.postProcess('metric-doc', 'evaluateMetrics', {}, {}, {}, {});
                        $.post("../../command/metric-doc/evaluateMetrics?" + $.param({ project: theProject.id }), null, 
                        function(data) {
                          window.location.reload(false);
                        }, "json");
                      });

                      $("#persist").on("click", function(d) {
                        $.post("../../command/metric-doc/persistMetrics?" + $.param({ project: theProject.id }), null, 
                          function(data) {}, 
                          "json");
                      })

                      $("#addCheck").on("click", function(d) {
                        metricData[0].evalTuples.push({evaluable: "", comment: "", disabled: false});
                        addEvaluableEntry();
                      });

                      $("#createMetricBtn").on("click", function(btn) {
                        var params = { 
                          project: theProject.id, 
                          metric: $("#metricSelectMetricModal").val(), 
                          columns: $("#columnFormMetricModal").val(), 
                          datatype: "unknown"
                        };
                        $.post("../../command/metric-doc/createMetric?" + $.param(params) + "&callback=?",
                          function(data) {
                            $("#addMetricModal").modal("hide");
                          });
                      });

                      var datatablesHeader = $(".dataTables_scrollHead").height();
                      var rawDataHeight = $('#raw-data-container').height();

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
                      tr.append("th").text(function(d) { return d.name; });
                      var td = tr.selectAll("tr").data(overlayModel.metricColumns).enter().append("td");

                      var minScale = 3;
                      if (overlayModel.availableMetrics.length > 2) minScale = overlayModel.availableMetrics.length;
                        
                      z = d3.scale.ordinal()
                        .range(colorbrewer.Reds[minScale])
                        .domain([0, overlayModel.availableMetrics]);

                      $("#raw-data-container").css({marginLeft: $("#overviewTable > thead > tr > th").outerWidth()});
                      $('#detailViewHeader').css('marginTop', 0 + 'px')
                        .css('marginBottom', 0 + 'px');

                      $('#dataset').DataTable().columns.adjust().draw();
                      // $('#dataset').DataTable().columns().header().draw();

                      var colWidths = [];
                        $.each($("#dataset > thead > tr > th"), function(i, header) {
                          colWidths.push(header.offsetWidth-1);
                        });


                      var columns = theProject.columnModel.columns;
                      for (var col = 0; col < columns.length; col++) {
                        var column = columns[col];
                        columnStore[column.cellIndex] = {"title": column.name};
                      }

                      var overviewTable = d3.select("#overviewTable").select("thead tr")
                        .selectAll('td')
                        .data(columns).enter()
                        .append('td')
                        .attr("width", function(d, i) {
                          return colWidths[i];
                      }).text(function(col) { 
                          return col.name; 
                      }).attr("data-toggle", "popover")
                        .on("click", addMetricToColumn);
                      $("#overviewTable thead tr td").popover({
                        html: 'true',
                        trigger: 'manual',
                        placement: 'auto top',
                        animation: 'false',
                        container: 'body',
                        content: ''
                      }).on("click", function () {
                        var _this = this;
                        $(this).popover("toggle");
                        $(".popover").on("mouseleave", function () {
                            $(_this).popover('hide');
                        });
                      });

                      var dataCols = dataSet[0];

                      td.append("svg")
                        .attr("width", function(d, i) {
                          return colWidths[i];
                        })
                        .attr("height", 12)
                        .classed("overview-svg", true)
                        .attr("data-toggle", "popover")
                      .append("rect")
                        .attr("height", 12)
                        .attr("width", function(d, i) {
                          if (d != null) {
                            var metricName = this.parentNode.parentNode.parentNode.__data__;
                            var metricCurrent = d.metrics.filter(function(m) {
                              return m.name == metricName.name;
                            });
                            if (metricCurrent.length > 0) {
                              return metricCurrent[0].measure * colWidths[i];
                            }
                          }
                        })
                        .style("fill", function(d, i) {
                          if (d != null) {
                            var metricName = this.parentNode.parentNode.parentNode.__data__;
                            var metricCurrent = d.metrics.filter(function(m) {
                              return m.name == metricName.name;
                            });
                            var idx = d.metrics.indexOf(metricCurrent[0]);
                            return z(d.metrics.indexOf(metricCurrent[0]));
                          }
                        });

                      var spanningArray;
                      if(overlayModel.spanningMetrics != null) {
                        spanningArray = overlayModel.spanningMetrics;
                        spanningArray.push(overlayModel.uniqueness);
                      } else {
                        spanningArray = new Array(overlayModel.uniqueness);
                      }

                      // var utr = d3.select("#uniquenessTable").select("tbody").selectAll("tr").data(uniquenessArray).enter().append("tr");
                      var spanningTables = d3.select("#spanningMetricPanel").selectAll("table").data(spanningArray).enter().append("table").each(function(d, i) {
                        var table = d3.select(this);
                        var hrow = table.append("thead").append("tr");
                        hrow.append("th");
                        hrow.selectAll("td").data(d.spanningColumns).enter().append("td").text(function(d) { return d;});

                        var trow = table.append("tbody").append("tr");
                        trow.append("th").text(function(d) { return d.name; })
                          .attr("width", function(d, i) { return $("#overviewTable > thead > tr > th").outerWidth(); });

                        trow.append("svg")
                          .attr("width", function(d, i) {
                            return ($("#dataset").width()/dataCols.length) * d.spanningColumns.length;
                          })
                          .attr("height", 12)
                          .append("rect")
                          .attr("height", 12)
                          .attr("width", function(d, i) {
                            return d.measure * (($("#dataset").width()/dataCols.length) * d.spanningColumns.length);
                          })
                          .style("fill", function(d, i) {
                            return z(overlayModel.availableMetrics.length);
                          });
                      });
                      // spanningTable.append("thead").append("th");

                      // utr.append("th").text(function(d) { 
                      //   return d.name; })
                      //   .attr("width", function(d, i) {
                      //     return $("#overviewTable > thead > tr > th").outerWidth();
                      //   });
                      // utr.append("svg")
                      //   .attr("width", function(d, i) {
                      //     return ($("#dataset").width()/dataCols.length) * overlayModel.duplicateDependencies.length;
                      //   })
                      //   .attr("height", 12)
                      //   .append("rect")
                      //   .attr("height", 12)
                      //   .attr("width", function(d, i) {
                      //     return d.measure * (($("#dataset").width()/dataCols.length) * overlayModel.duplicateDependencies.length);
                      //   })
                      //   .style("fill", function(d, i) {
                      //     return z(overlayModel.availableMetrics.length);
                      //   })
                      //   .append("svg:title")
                      //     .text(function(d) { 
                      //       return (100 * d.measure) + "%"; 
                      //     }
                      //   );

                      td.on("click", function(d) {
                        if (d != null) {
                          var rowIndex = overlayModel.availableMetrics.indexOf(this.parentNode.__data__);
                          if (d3.event.shiftKey) {
                            contextColumn = null;
                          } else {
                            if (selectedOverviewRect != null) {
                              selectedMetricIndex = [];
                              selectedOverviewRect = [];
                              selectedColName = [];
                              metricData = [];
                              d3.selectAll(".selected").classed("selected", false);
                            }
                          }
                          selectedColOpacity = []
                          selectedMetricIndex.push(rowIndex);
                          selectedOverviewRect.push(d3.select(this).attr("class", "selected"));
                          selectedColName.push(d.columnName);
                          metricData.push(overlayModel.metricColumns.filter(function(d) {
                            if (d != null) {
                              return selectedColName[selectedColName.length-1].indexOf(d.columnName) >= 0;
                            }
                          })[0].metrics[rowIndex]);

                          totalEvalTuples = [];
                          for (var i = 0; i < metricData.length; i++) {
                            var enabledTuples = metricData[i].evalTuples.filter(function(d) {
                              return !d.disabled;
                            });
                            totalEvalTuples.push.apply(totalEvalTuples, enabledTuples);
                            for(var j = 0; j < enabledTuples.length; j++) {
                              selectedColOpacity.push((i+1) * (1/selectedMetricIndex.length));
                            }
                          }
                          refillEditForm(metricData, selectedColName, rowIndex);

                          $('#detailMetricHeader').empty();
                          $('#detailColumnHeader').text("Detail View");
                          if(selectedMetricIndex.length == 1) {
                            $('#detailMetricHeader').append("<h5>Selected Metric:</h5>");
                          } else {
                            $('#detailMetricHeader').append("<h5>Selected Metrics:</h5>");
                          }
                          for(var indexCount = 0; indexCount < selectedMetricIndex.length; indexCount++) {
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
                              return z(((i+1) * (1/selectedMetricIndex.length)) * selectedMetricIndex[i]);
                            });
                          mHeaders.append("text").text(function(d, i) {
                            return selectedColName[i] + " - " + capitalizeFirstLetter(overlayModel.availableMetrics[selectedMetricIndex[i]].name);
                          })

                          var headerHeightComp = datatablesHeader;// - $('#detailViewHeader').height();
                          var marginHeatmap = {top: headerHeightComp, right: 50, bottom: 70, left: 35};
                          var width = parseInt(d3.select("#heatmap").style("width")) - marginHeatmap.left - marginHeatmap.right,
                              height = $(".dataTables_scrollBody").height();
                          if (width > (totalEvalTuples.length*100)) width = totalEvalTuples.length*100;
                          detailWidths = [];
                          for (var i = 0; i < totalEvalTuples.length; i++) {
                            detailWidths.push(width/totalEvalTuples.length);
                          }

                          redrawDetailView(theProject, metricData, rowIndex, rowModel, overlayModel, height, width, marginHeatmap);
                        }
                      });
                      td.select("svg").call(tooltipOverview);
                      td.select("svg").on("mouseover", function(d) {
                        if (d != null) {
                          var rowIndex = this.parentNode.parentNode.sectionRowIndex;
                          tooltipOverview.show(d.metrics[rowIndex]);
                        };
                      }).on("mouseout", function(d) {
                        tooltipOverview.hide();
                      });
                      td.select("svg").on("contextmenu", function(d, i) {
                        d3.event.preventDefault();
                        var rowIndex = this.parentNode.parentNode.sectionRowIndex;
                        contextMetric = d.metrics[rowIndex];
                        contextColumn = d.columnName;
                        var _this = this;
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
                          '<button type="button" class="btn" id="merge-metric">Merge</button>'+
                          '<button type="button" class="btn btn-warning" id="duplicate-metric">Duplicate</button></div>'
                      });

                      $("#overviewPanel").css({height: $("#overviewTable").height() + margin});

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

  var overlayY = d3.scale.linear()
    .domain([rowModel.filtered, 0])
    .rangeRound([$(".dataTables_scrollBody").height(), 0]);

  var minScale = 3;
  if (overlayModel.availableMetrics.length > 2) minScale = overlayModel.availableMetrics.length;

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
        return d.metrics;
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
    var metricsCol = this.parentNode.parentNode.__data__;
    var current = this.parentNode.__data__;
    return z(metricsCol.metrics.indexOf(current));
  });

  bins.call(tooltipInvalid);

  bins.each(function (d) {
    var ys = d3.select(this)
      .attr("y", overlayY(d.index))
      .attr("height", 1);
  });

  bins.on("click", function(d) {
    $("#dataset td").removeClass("highlight");
    var selThis = this;
    var selectedRows = d3.select(this.parentNode).selectAll("rect").filter(function(r) {
      return d3.select(this).attr("y") === d3.select(selThis).attr("y");
    })[0];
    $("#dataset").DataTable().row(selectedRows[0].__data__.index).scrollTo();
    $.each(selectedRows, function(i, rowCurrent) {
      $.each($("#dataset").DataTable().row(rowCurrent.__data__.index).node().children, function(i, td) {
        td.classList.add("highlight");
      });
    });
  });

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
  });

  bins.on("mouseout", function(d) {
      d3.select(this).style("fill", function(d, i) {
        var metricsCol = this.parentNode.parentNode.__data__;
        var current = this.parentNode.__data__;
        return z(metricsCol.metrics.indexOf(current));
      });
      tooltipInvalid.hide();
    });
  // var headers = d3.select("#raw-data-container").select("#dataset_wrapper").select(".dataTables_scroll").select(".dataTables_scrollBody");//.selectAll("td").data(overlayModel.metricColumns);
  // var svg = headers.insert("svg", "#dataset")
  //   .attr("class", "overlay")
  //   .attr("width", $(".dataTables_scrollBody").width())
  //   .attr("height", $(".dataTables_scrollBody").height())
  //   .attr("top", tablePos.top)
  //   .attr("left", tablePos.left);
}

function refillEditForm(d, colName, metricIndex) {
  $("#metricInfoDetailHeader").text("Metric Detail - " + capitalizeFirstLetter(d[0].name) + " - " + selectedColName[0]);
  $("#metricName").text(d[0].name);
  $("#metricDescription").text(d[0].description);

  $(".metricCheck").remove();
  $("#typeDetail").remove();
  
  if (d[0].datatype.indexOf("numeric") > -1) {
    $("#dataTypeNumeric").addClass('active');
  } else {
    $("#dataTypeNumeric").removeClass('active');
  }
  if (d[0].datatype.indexOf("string") > -1) {
    $("#dataTypeString").addClass('active');
  } else {
    $("#dataTypeString").removeClass('active');
  }
  if (d[0].datatype.indexOf("datetime") > -1) {
    $("#dataTypeDateTime").addClass('active');
  } else {
    $("#dataTypeDateTime").removeClass('active');
  }
  if (d[0].datatype.indexOf("categoric") > -1) {
    $("#dataTypeCategoric").addClass('active');
  } else {
    $("#dataTypeCategoric").removeClass('active');
  }

  $("#concat button").removeClass('active');
  $("#concat" + d[0].concat).addClass('active');

  if (d[0].evalTuples.length > 0) {
    var metric = d[0].evalTuples[0].evaluable;
    metric = metric.substr(0, metric.indexOf("("));
    $("#base" + metric).prop("checked", true);
  }
  if (d[0].evalTuples.length > 0) {
    for (var i = 0; i < d[0].evalTuples.length; i++) {
      addEvaluableEntry(d[0].evalTuples[i].evaluable)
    }
  }
}

function fillModalAfterColumnSelection(theProject) {
  var selectedCols = $("#columnFormMetricModal").val();
  $("#metricSelectMetricModal .btn").remove();
  d3.select("#typeDetailModal").select("tbody").selectAll("tr").remove();
  d3.select("#typeDetailModal").select("thead tr").selectAll("td").remove();

  var dataTypes;
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
    });
  if(selectedCols.length == 1) {
    dataTypes = [{type: "String", val: [20]}, {type: "Numeric", val: [70]}, {type: "Date/Time", val: [3]}, {type: "unknown", val: [7]}];
    metrics = theProject.overlayModels.metricsOverlayModel.availableMetrics;
  } else {
    dataTypes = [{type: "String", val: [20, 10]}, {type: "Numeric", val: [70, 85]}, {type: "Date/Time", val: [3, 1]}, {type: "unknown", val: [7, 4]}];
    metrics = theProject.overlayModels.metricsOverlayModel.availableSpanningMetrics;
  }

  $.each(metrics, function(key, value) {
      var cl = "btn btn-default";
      if(selectedCols.length == 1) {
        var selectedMetrics = theProject.overlayModels.metricsOverlayModel.metricColumns[0].metrics;
        for (var i = 0; i < selectedMetrics.length; i++) {
          if(selectedMetrics[i].name == value.name) {
            cl += " disabled";
            break;
          }
        }
      } else if (selectedCols.length > 1) {
        var spanningMetrics = theProject.overlayModels.metricsOverlayModel.spanningMetrics;
        if (spanningMetrics != null) {
          for (var i = 0; i < spanningMetrics.length; i++) {
            if (value.name == spanningMetrics[i].name.toLowerCase()) {
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
      $("#metricSelectMetricModal").append('<button type="button" value="' + value.name + '" class="' + cl + '">'+ capitalizeFirstLetter(value.name) + '</button>');
      $("#metricSelectMetricModal > button").on("click", function() {
        $("#metricSelectMetricModal").val($(this).text());
      });
    });
}

function detaildragresize(d) {
  //Max x on the left is x - width 
  //Max x on the right is width of screen + (dragbarw/2)
  var dragx = Math.max(d.x + (dragbarw/2), Math.min(detailWidth, d.x + dragbarw + d3.event.dx));
  // console.log(d.x+10 + ", max of(" + detailWidth + ", " + (d.x + 20 + d3.event.dx));
  //recalculate width
  detailWidth = detailWidth + d3.event.dx;
  console.log(detailWidth);

  var selectedIdx = this.__data__;
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
      var script = totalEvalTuples[d].evaluable;
      if (script != null) {
        var label;
        if (script.indexOf(metricData.name) < 0) {
          label = script.substr(3, script.indexOf(",")-3);
        } else {
          label = script;
        }
        return label;
      }
    });

  d3.select("g.x.axis").call(xAxis);
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