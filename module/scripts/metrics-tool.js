var theProject,
    overlayModel,
    rowModel,
    rawDataTable,
    detailWidth,
    dragbarbottom,
    dragheight = 20,
    dragbarw = 20,
    detailWidths,
    metricType = [],
    selectedChecks = [],
    selectedOverviewRect = [],
    selectedColName = [],
    selectedCol = [],
    selectedColOpacity = [],
    metricData = [],
    totalEvalTuples = [],
    rowIndex = [],
// overview vis
    colWidths,
// data overlay vis
    overlayY,
// detail view dimensions
    detailViewHeight,
    detailViewWidth,
    detailViewMargin,
// context menu
    contextMetric,
    contextColumn,
// coloring scale should be defined globally
    z,
    selectedEditEvaluable,
    dataSet = [],
    columns,
    columnStore = [],
    columnsStore = [],
    colWidth = 50,
    dataTypes,
    datatablesHeader,
    selectedMetricModal,
// selection for creating a metric
    columnForMetricToBeCreated,
    metricToBeCreated = [];
var pageLength = 500;

var tooltipInvalid = d3.tip()
  .attr("class", "d3-tip")
  .offset([-10, 0])
  .html(function(d) {
    if(d.index != null && d.index.length > 1 && d.index[d.index.length-1] - d.index[0] === d.index.length-1) {
      return "<strong>Rows:</strong> <span style='color:steelblue'>" + (d.index[0] + 1) + " - " + (d.index[d.index.length-1] + 1) + "</span>";
    } else {
      var indexVals = [].concat( d.index );
      $.each(indexVals, function(i, data) { indexVals[i] = data + 1; });
      return "<strong>Row:</strong> <span style='color:steelblue'>" + indexVals.join(", ") + "</span>";
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
    if(d != null) {
      return d.index.indexOf(iDataIndex) != -1;
    }
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
                  columns = theProject.columnModel.columns;
                  for (var r = 0; r < data.rows.length; r++) {
                    var row = data.rows[r];
                    var rowValues = [''];
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

                  columnStore = [{"title": ""}];
                  $.each(columns, function(index, value) {
                    rowIndex.push(index);
                    columnStore[index+1] = {"title": value.name};
                  });

                  var dataCols = dataSet[0];

                  $.each(columns, function(key, value){
                    $("#columnFormMetricModal").append('<option value="' + value.name + '">' + value.name + '</option>');
                    $("#columnDuplicateModal").append('<option value="' + value.name + '">' + value.name + '</option>');
                  });

                  $("#columnFormMetricModal").attr("size", columnStore.length-1);

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

                      if (overlayModel.uniqueness != null) {
                        $('#uniqueness > tbody:last-child').append("<tr><td>" + overlayModel.uniqueness.name + "</td></tr>");
                      }

                      var minScale = overlayModel.availableMetrics.length + overlayModel.availableSpanningMetrics.length;
                      
                      z = d3.scale.ordinal()
                        .range(colorbrewer.YlOrRd[minScale+1])
                        .domain([minScale, 0]);

                      renderTableHeader();

                      var scrollHeight = 650 - $("#datasetHeader").height() - $("div.bottom").height() + getScrollBarWidth();

                      if(dataSet.length < pageLength) pageLength = dataSet.length;

                      $('#dataset').dataTable( {
                        "data": dataSet,
                        "columns": columnStore,
                        "scrollX": true,
                        "pageLength": pageLength,
                        "paging": true,
                        "bSort": false,
                        "bFilter": true,
                        "dom": 'rt<"bottom"ip><"clear">',
                        "drawCallback": dataTableRedrawn
                      });

                      $(".dataTables_scrollBody").css('height', scrollHeight + 'px');

                      $("#raw-data-container").css({marginLeft: $("#overviewTable > thead > tr > th").outerWidth()});
                      $('#detailViewHeader').css('marginTop', 0 + 'px')
                        .css('marginBottom', 0 + 'px');

                      $("#overviewPanel").css({height: $("#overviewTable").height() + margin});
                      
                      $("#overviewTable thead tr td").popover({
                        html: 'true',
                        trigger: 'manual',
                        placement: 'auto top',
                        animation: 'false',
                        container: 'body',
                        content: ''
                      });

                      d3.select("div.dataTables_scrollBody").append("svg").attr("id", "overlay");
                      $("#overlay").css({top: 0, 
                        left: 0,
                        position:'absolute', 
                        width: $(".dataTables_scrollBody > table").width(), 
                        height: $(".dataTables_scrollBody").height(),
                        'pointer-events': 'none'
                      });

                      renderMetricOverview();
                      renderSpanningMetricOverview();
                      drawDatatableScrollVis();
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
      // if(overlayModel.availableSpanningMetrics.indexOf(metricData[i].name) != -1) {
        return metricData[i].spanningColumns.toString() + " - " + capitalizeFirstLetter(metricData[i].name);
      // } else {
        // return metricData[i].columnName + " - Uniqueness";
      // }
    }
  })
}

function fillModalAfterColumnSelection(theProject) {
  var selectedCols = $("#columnFormMetricModal").val();
  $("#metricSelectMetricModal .btn").remove();
  d3.select("#typeDetailModal").select("tbody").selectAll("tr").remove();
  d3.select("#typeDetailModal").select("thead tr").selectAll("td").remove();

  var metrics;
  dataTypes = null;
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
            if(dataTypes == null) {
              dataTypes = data[typeIdx];
            }
            if(data[typeIdx].val[colIdx] > dataTypes.val[colIdx]) {
              dataTypes = data[typeIdx];
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
          if(overlayModel.metricColumns[columnIndex] != null) {
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