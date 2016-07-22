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

                      if (overlayModel.uniqueness != null) {
                        $('#uniqueness > tbody:last-child').append("<tr><td>" + overlayModel.uniqueness.name + "</td></tr>");
                      }

                      $("#datasetHeader").append('<tr><th></th><th colspan="'+columns.length+'">Multiple-Column Metrics</th></tr>');
                      $.each(overlayModel.spanningMetrics, function(key, metric) {
                        var spanMetricRow = '<tr class="span-metric-row">';
                        spanMetricRow += '<th style="padding-left:10px; padding-top:0px; padding-bottom:0px; font-weight:normal;">' + metric.name + '</th>';
                        spanMetricRow += '<td style="padding:0px;" colspan="' + columns.length + '"></td>'
                        spanMetricRow += '</tr>';
                        var spanMetricColumns = '<tr><td></td>';
                        $.each(metric.spanningColumns, function(idx, column) {
                          var colspan = Math.floor(columns.length/metric.spanningColumns.length);
                          if(idx == 0) {
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

                      $('#dataset').dataTable( {
                        "data": dataSet,
                        "columns": columnStore,
                        "scrollY": "500px",
                        "scrollX": true,
                        "scrollCollapse": true,
                        "scroller": true,
                        "bSort": false,
                        "bFilter": true,
                        "dom": 'rt<"bottom"i><"clear">'
                      });

                      //this reorders the metrics to be in line with the actual displayed columns
                      var sortedMetrics = new Array();
                      for(var idx = 0; idx < theProject.columnModel.columns.length; idx++) {
                        var foundColumn = overlayModel.metricColumns.filter(function(col) {
                          if (columns[idx] != null) {
                            return col.columnName == columns[idx].name;
                          }
                        })[0];
                        if (foundColumn != null) {
                          sortedMetrics[idx] = foundColumn;
                        } else {
                          sortedMetrics[idx] = null;
                        }
                      }
                      overlayModel.metricColumns = sortedMetrics;

                      var minScale = overlayModel.availableMetrics.length + overlayModel.availableSpanningMetrics.length;
                      
                      // tr = d3.select("#overviewTable").select("tbody").selectAll("tr").data(overlayModel.availableMetrics).enter().append("tr");
                      // tr.append("th").text(function(d) { return d; });
                      tr = d3.selectAll("tr.metric-row").data(overlayModel.availableMetrics);

                      z = d3.scale.ordinal()
                        .range(colorbrewer.YlOrRd[minScale+1])
                        .domain([minScale, 0]);

                      $("#raw-data-container").css({marginLeft: $("#overviewTable > thead > tr > th").outerWidth()});
                      $('#detailViewHeader').css('marginTop', 0 + 'px')
                        .css('marginBottom', 0 + 'px');

                      $("#overviewPanel").css({height: $("#overviewTable").height() + margin});
                      
                      // colWidths = [];
                      // $.each($("#dataset > tbody > tr")[0].children, function(i, header) {
                      //   colWidths.push(header.offsetWidth);
                      // });

                      // var overviewTable = d3.select("#overviewTable").select("thead tr")
                      //   .selectAll('td')
                      //   .data(columns).enter()
                      //   .append('td')
                      //   .attr("width", function(d, i) {
                      //     return colWidths[i];
                      // }).text(function(col) { 
                      //     return col.name; 
                      // }).attr("data-toggle", "popover")
                      //   .on("contextmenu", addMetricToColumn);

                      $("#overviewTable thead tr td").popover({
                        html: 'true',
                        trigger: 'manual',
                        placement: 'auto top',
                        animation: 'false',
                        container: 'body',
                        content: ''
                      });

                      renderMetricOverview();
                      renderSpanningMetricOverview();
                      drawDatatableScrollVis(theProject, rowModel, columnStore);
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