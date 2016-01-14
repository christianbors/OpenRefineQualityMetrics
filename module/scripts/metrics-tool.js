$(document).ready(function() {
  //$('#demo').html( '<table cellpadding="0" cellspacing="0" border="0" class="display" id="example"></table>' );

  var dataSet = [];
  var columnStore = [];
  var colWidth = 50;
  var selectedColName = "Altitude";
  var selectedMetricIndex = 0;
  var selectedBaseMetric;
  var selectedOverviewRect;
  var metricData;

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

  var theProject = { id : URL.getParameters().project};

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
            
            var columns = theProject.columnModel.columns;
            for (var col = 0; col < columns.length; col++) {
              var column = columns[col];
              columnStore[column.cellIndex] = {"title": column.name};
            }

            var overviewTable = d3.select("#overviewTable");
            overviewTable.insert('thead','tbody')
                .append('tr')
                .selectAll('th')
                .data(columnStore).enter()
                .append('th')
                .text(function(col) { return col.title; });
            overviewTable.append('tbody');

            var dataCols = dataSet[0];

            // if(dataCols) {
            //   if(dataCols.length == columnStore.length) {
            //     $('#dataset').dataTable( {
            //       "data": dataSet,
            //       "columns": columnStore
            //     } );
            //   }
            // }

            //load column names into the modal
            for(var col = 0; col < columns.length; col++) {
              var colName = columns[col];
              $("<option />")
             .attr("value", colName.title)
             .attr("label", colName.title)
             .appendTo("#columnFormMetricModal");
            }
            /*
            for (var i = 0; i < columns.length; i++) {
              var column = columns[i];
              if (column.cellIndex == cellIndex) {
                return column;
              }
            }*/

            $.post(
              "../../command/core/get-rows?" + $.param({ project: theProject.id, start: 0, limit: 500 }) + "&callback=?",
              [],
              function(data) {
                var rowModel = data;

                // Un-pool objects
                for (var r = 0; r < data.rows.length; r++) {
                  var row = data.rows[r];
                  var rowValues = [];
                  for (var c = 0; c < theProject.columnModel.columns.length; c++) {
                    var cell = row.cells[c];
                    if (cell != null) {
                      rowValues.push(cell.v);
                    } else {
                      rowValues.push('');
                    }
                  }
                  dataSet.push(rowValues);
                }

                var dataCols = dataSet[0];

                if(columnStore) {
                  if(dataCols.length == columnStore.length) {
                    $('#dataset').dataTable( {
                      "data": dataSet,
                      "columns": columnStore,
                      "scrollY": "400px",
                      "scrollCollapse": true,
                      "paging": false,
                      "dom": 'rt<"bottom"iflp><"clear">'
                    } );
                  }
                }

                $.getJSON(
                  "../../command/metric-doc/get-metrics-overlay-model?" + $.param({ project: theProject.id }), 
                  null,
                  function(data) {
                    var overlayModel = data;
                    var margin = 20;

                    $.each(data.availableMetrics, function(index, value) {
                      $('#metricNames > tbody:last-child').append("<tr><td>" + value + "</td></tr>")
                    });

                    $("input:radio[name=optionsRadios]").click(function() {
                      selectedBaseMetric = $(this).attr("value");
                    });

                    $("#recalculate").on("click", function(d) {
                      // recalculate
                      // Refine.postProcess('metric-doc', 'evaluateMetrics', {}, {}, {}, {});
                      $.post("../../command/metric-doc/evaluateMetrics?" + $.param({ project: theProject.id }), null, 
                      function(data) {
                        window.location.reload(false);
                      }, "json");
                    });

                    $("#addCheck").on("click", function(d) {
                      var i = $(".metricCheck").length + 1;
                      var checkName = "Check " + i;
                      $("<li class='list-group-item pop metricCheck' data-toggle='popover'><label for='metricCheck" + i + "'>"
                      + checkName + "</label><input class='form-control' id='eval"+(i)+"'/></li>").insertBefore("#addCheckButton");
                    });

                    var datatablesHeader = $(".dataTables_scrollHead").height();
                    var rawDataHeight = $('#raw-data-container').height();
                    $("#raw-data-container").css({marginLeft: $("#metricNames").width()});
                    $("#overviewTable").css({width: $("#overviewPanel").width() - $("#metricNames").width() - margin});
                    // datatablesHeader = datatablesHeader - $('#detailViewHeader').height();
                    // $('#detailViewHeader').css('padding-bottom', '-' + datatablesHeader + 'px');
                    $('#detailViewHeader').css('margin', 0 + 'px');
                    // $("#heatmap").css({marginTop: datatablesHeader});

                    colWidth = ($("#overviewPanel").width() - $("#metricNames").width() - margin)/columnStore.length;

                    //this reorders the metrics to be in line with the actual displayed columns
                    var sortedMetrics = new Array();
                    for(var idx = 0; idx < theProject.columnModel.columns.length; idx++) {
                      var foundColumn = overlayModel.metricColumns.filter(function(col) {
                        return col.columnName == columnStore[idx].title;
                      })[0];
                      if (foundColumn != null) {
                        sortedMetrics[idx] = foundColumn;
                      } else {
                        sortedMetrics[idx] = null;
                      }
                    }
                    overlayModel.metricColumns = sortedMetrics;

                    var tr = d3.select("#overviewTable").select("tbody").data(overlayModel.availableMetrics).append("tr");
                    var td = tr.selectAll("tr").data(overlayModel.metricColumns).enter().append("td");

                    td.append("svg")
                      .attr("width", colWidth)
                      .attr("height", 12)
                    .append("rect")
                      .attr("height", 12)
                      .attr("width", function(d) {
                        if (d != null) {
                          var metricName = this.parentNode.parentNode.parentNode.__data__;
                          var metricCurrent = d.metrics.filter(function(m) {
                            return m.name == metricName;
                          });
                          return metricCurrent[0].measure * colWidth;
                        }
                      });

                    td.on("click", function(d) {
                      if (selectedOverviewRect != null) {
                        selectedOverviewRect.style("stroke-width", 0)
                          .style("stroke", "transparent");
                      }
                      selectedOverviewRect = d3.select(this)
                        .style("stroke-width", 2)
                        .style("stroke", "black");
                      selectedColName = d.columnName;
                      selectedMetricIndex = overlayModel.availableMetrics.indexOf(d.metrics[0].name);
                      metricData = overlayModel.metricColumns.filter(function(d) {
                        if (d != null) {
                          return d.columnName == selectedColName;
                        }
                      })[0].metrics[selectedMetricIndex];
                      redrawDetailView(theProject, metricData, datatablesHeader, selectedMetricIndex, selectedColName, rowModel, overlayModel);
                    });

                    $("#overviewPanel").css({height: $("#overviewTable").height() + margin});

                    metricData = overlayModel.metricColumns.filter(function(d) {
                      if (d != null) {
                        return d.columnName == selectedColName;
                      }
                    })[0].metrics[selectedMetricIndex];

                    redrawDetailView(theProject, metricData, datatablesHeader, selectedMetricIndex, selectedColName, rowModel, overlayModel);

                    //todo: edit when selecting other metric
                    
                    // function resize() {
                    //   var margin = 20
                    //       width = parseInt(d3.select("#heatmap").style("width")) - margin*2,
                    //       height = rawDataHeight - margin*2;

                      /* Update the range of the scale with new width/height */
                      // xScale.range([0, width]).nice(d3.time.year);
                      // yScale.range([height, 0]).nice();

                      /* Update the axis with the new scale */
                      // svg.select('.x.axis')
                      //     .attr("transform", "translate(0," + height + ")")
                      //     .call(xAxis);

                      // svg.select('.y.axis')
                      //     .call(yAxis);

                      /* Force D3 to recalculate and update the line */
                    //   svg.selectAll(".bin")
                    //       .attr("d", bins);
                    // }

                    // d3.select(window).on('resize', resize);

                    $('#dataset').dataTable().fnDraw();

                    refillEditForm(metricData, selectedColName, selectedMetricIndex);
                    if (metricData.evaluables.length > 0) {
                      var metric = metricData.evaluables[0];
                      selectedBaseMetric = metric.substr(0, metric.indexOf("("));
                    }

                    $("#btnReset").click(function() {
                      refillEditForm(metricData, selectedColName, selectedMetricIndex);
                    });

                    $("#btnSave").click(function() {
                      metricData.name = $("#metricName").val();
                      metricData.description = $("#metricDescription").val();
                      // var checks = $("#metricCheck");
                      metricData.evaluables = [];
                      metricData.evaluables.push(selectedBaseMetric + "(value)");
                      for (var i = 0; i < $(".metricCheck").length; ++i) {
                        metricData.evaluables.push($("#eval" + (i+1)).val());
                      }
                      $.post("../../command/metric-doc/update-metric?" + $.param(
                          { 
                            metricName: metricData.name, 
                            column: selectedColName,
                            metricIndex: selectedMetricIndex,
                            metricDatatype: metricData.datatype,
                            metricDescription: metricData.description,
                            metricEvaluables: metricData.evaluables,
                            project: theProject.id 
                          }) + "&callback=?",
                        {},
                        {},
                        function(response) {
                          console.log("success");
                        }, 
                        "jsonp"
                      );
                    });
                  }, 
                  'json'
                );
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

  Sortable.create(simpleList, { /* options */ });
});

function redrawDetailView(theProject, metricData, datatableHeader, selectedMetricIndex, selectedColName, rowModel, overlayModel) {
  $('#detailViewHeader').text(selectedColName + " - " + overlayModel.availableMetrics[selectedMetricIndex]);

  d3.select("#heatmap").select("svg").remove();
  // heatmap drawing
  var headerHeightComp = datatableHeader - $('#detailViewHeader').height();
  var marginHeatmap = {top: headerHeightComp, right: 0, bottom: 50, left: 35};
  var width = parseInt(d3.select("#heatmap").style("width")) - marginHeatmap.left - marginHeatmap.right,
      height = $(".dataTables_scrollBody").height();
      // height = rawDataHeight - marginHeatmap.top;
  
  var xScale = d3.scale.linear()
    .domain([0, metricData.evaluables.length])
    .range([0, width]);

  var yScale = d3.scale.linear()
    .range([height, 0])
    .nice();

  var x = d3.scale.linear( )
    .domain([0, metricData.evaluables.length])
    .range([0, width]);

  var y = d3.scale.linear()
    .domain([rowModel.filtered, 0])
    .rangeRound([height, 0]);

  var z = d3.scale.linear()
    .domain([0, 1])
    .range(["white", "steelblue"])
    .interpolate(d3.interpolateRgb);

  var xAxis = d3.svg.axis()
    .scale(xScale)
    .orient("bottom")
    .ticks(metricData.evaluables.length)
    .tickFormat(function(d) {
      return metricData.evaluables[d];
    });

  var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left")
    .tickFormat(d3.format("d"));

  var svg = d3.select("#heatmap").append("svg")
    .attr("width", width + marginHeatmap.left + marginHeatmap.right)
    .attr("height", height + marginHeatmap.top + marginHeatmap.bottom)
    .append("g")
    .attr("transform", "translate(" + marginHeatmap.left + "," + marginHeatmap.top + ")");
  
  var metricDetail = svg.selectAll(".metric-detail-row")
    .data(metricData.dirtyIndices)
    .enter( ).append("g")
    .attr("class", "metric-detail-row");

  var bins = metricDetail.selectAll(".bin")
      .data(function (d) { return d.dirty; })
      .enter( ).append("rect")
      .attr("class", "bin");

  bins.attr("x", function (d, i) { 
      return x(i); 
    })
    .attr("width", function (d, i) { 
      return  x(i+1) - x(i); 
    })
    .style("fill", function(d) {
      if (d == true) {
        return z(0);
      } else {
        return z(1);
      }
      
    });

  metricDetail.each(function (d) {
    var ys = d3.select(this).selectAll(".bin")
      .attr("y", y(d.index) )
      .attr("height", 1);
  });

  svg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + (height) + ")")
    .call(xAxis)
  .selectAll(".tick text")
    .style("text-anchor", "start")
    .attr("x", 6)
    .attr("y", 6);

  svg.append("g")
    .attr("class", "y axis")
    .call(yAxis);

  metricDetail.on("click", function(d) {
    console.log(d.index);
    $.post(
      "../../command/core/get-rows?" + $.param({ project: theProject.id, start: d.index, limit: 500 }) + "&callback=?",
      [],
      function(data) {
        var dataSet = [];
        for (var r = 0; r < data.rows.length; r++) {
          var row = data.rows[r];
          var rowValues = [];
          for (var c = 0; c < theProject.columnModel.columns.length; c++) {
            var cell = row.cells[c];
            if (cell != null) {
              rowValues.push(cell.v);
            } else {
              rowValues.push('');
            }
          }
          dataSet.push(rowValues);
        }
        var dataTable = $('#dataset').dataTable();
        dataTable.fnClearTable();
        dataTable.fnAddData(dataSet);
      },
      "jsonp"
    );
  });

  bins.on("mouseover", function(d) {
    d3.select(this).style("fill", "red");
    d3.select(this.parentNode).style("fill", "black");
  });

  bins.on("mouseout", function(d) {
    d3.select(this).style("fill", function(d) {
      if (d == true) {
        return z(0);
      } else {
        return z(1);
      }
      
    });
    d3.select(this.parentNode).style("fill", "transparent");
  });
}

function refillEditForm(d, colName, metricIndex) {
  $("#metricName").val(d.name);
  $("#metricDescription").val(d.description);

  $(".metricCheck").remove();
  
  if (d.datatype.indexOf("numeric") > -1) {
    $("#dataTypeNumeric").addClass('active');
  } else {
    $("#dataTypeNumeric").removeClass('active');
  }
  if (d.datatype.indexOf("string") > -1) {
    $("#dataTypeString").addClass('active');
  } else {
    $("#dataTypeString").removeClass('active');
  }
  if (d.datatype.indexOf("datetime") > -1) {
    $("#dataTypeDateTime").addClass('active');
  } else {
    $("#dataTypeDateTime").removeClass('active');
  }
  if (d.datatype.indexOf("categoric") > -1) {
    $("#dataTypeCategoric").addClass('active');
  } else {
    $("#dataTypeCategoric").removeClass('active');
  }

  if (d.evaluables.length > 0) {
    var metric = d.evaluables[0];
    metric = metric.substr(0, metric.indexOf("("));
    $("#base" + metric).prop("checked", true);
    selectedBaseMetric = metric;
  }
  if (d.evaluables.length > 1) {
    for (var i = 1; i < d.evaluables.length; i++) {
      var checkName = "Check " + i;
      // insertBefore("addCheckButton")
      $("<li class='list-group-item pop metricCheck' data-toggle='popover'><label for='metricCheck" + i + "'>"
       + checkName + "</label><input class='form-control' id='eval"+(i)+"'/></li>").insertBefore("#addCheckButton");
      $("#eval" + i).val(d.evaluables[i]);
    }
  }
}

function capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
}