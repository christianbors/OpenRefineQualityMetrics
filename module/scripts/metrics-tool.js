$(document).ready(function() {
  //$('#demo').html( '<table cellpadding="0" cellspacing="0" border="0" class="display" id="example"></table>' );

  var dataSet = [];
  var columnStore = [];
  var colWidth = 50;
  var selectedColName = "Altitude";
  var selectedMetricIndex = 0;
  var selectedOverviewRect;
  var metricData;

  var selectedMetricModal;

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

            $.post(
              "../../command/core/get-rows?" + $.param({ project: theProject.id, start: 0 }) + "&callback=?",
              [],
              function(data) {
                theProject["row-model"] = data;
                $.post(
                "../../command/core/get-rows?" + $.param({ project: theProject.id, start: 0, limit: data.filtered }) + "&callback=?",
                [],
                function(data) {
                  var rowModel = data;

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
                    "scrollY": "600",
                    "scrollCollapse": true,
                    "paging": true,
                    "dom": 'rt<"bottom"ip><"clear">'
                  });

                  $.each(columnStore, function(key, value){
                    $("#columnFormMetricModal").append('<option value="' + value.title + '">' + value.title + '</option>');
                  });

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
                        var i = $(".metricCheck").length;
                        $("<li class='list-group-item pop metricCheck' data-toggle='popover'><label for='metricCheck" + i + "'>"
                        + "</label><input class='form-control' id='eval"+(i)+"'/><button class='btn btn-default remove-btn'>Remove</button></li>").insertBefore("#addCheckButton");
                      });

                      $("#createMetricBtn").on("click", function(btn) {
                        var params = { 
                          project: theProject.id, 
                          metric: $("#metricSelectMetricModal").val(), 
                          columns: $("#columnFormMetricModal").val(), 
                          description: "test", 
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
                        
                      var z = d3.scale.ordinal()
                        .range(colorbrewer.Reds[minScale])
                        .domain([0, overlayModel.availableMetrics]);

                      $("#raw-data-container").css({marginLeft: $("#overviewTable > thead > tr > th").outerWidth()});
                      $('#detailViewHeader').css('margin', 0 + 'px');

                      $('#dataset').dataTable().fnDraw();
                      $('#dataset').dataTable().fnAdjustColumnSizing();

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
                        })
                        .text(function(col) { 
                          return col.name; 
                        });

                      var dataCols = dataSet[0];

                      td.append("svg")
                        .attr("width", function(d, i) {
                          return colWidths[i];
                        })
                        .attr("height", 12)
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
                            } else {
                              return 0;
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
                        })
                        .append("svg:title")
                        .text(function(d) {
                          if (d != null) {
                            var metricName = this.parentNode.parentNode.parentNode.parentNode.__data__;
                            var metricCurrent = d.metrics.filter(function(m) {
                              return m.name == metricName.name;
                            });
                            return (100 * metricCurrent[0].measure) + "%"; 
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
                          })
                          .append("svg:title")
                            .text(function(d) { 
                              return (100 * d.measure) + "%"; 
                            }
                          );
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
                          if (d3.event.shiftKey) {
                              console.log("shift+click")
                          } 
                          if (selectedOverviewRect != null) {
                            selectedOverviewRect.style("stroke-width", 0)
                              .style("stroke", "transparent");
                          }
                          var rowIndex = overlayModel.availableMetrics.indexOf(this.parentNode.__data__);
                          selectedOverviewRect = d3.select(this)
                            .style("stroke-width", 2)
                            .style("stroke", "black");
                          selectedColName = d.columnName;
                          metricData = overlayModel.metricColumns.filter(function(d) {
                            if (d != null) {
                              return d.columnName == selectedColName;
                            }
                          })[0].metrics[rowIndex];
                          refillEditForm(metricData, selectedColName, rowIndex);
                          redrawDetailView(theProject, metricData, datatablesHeader, selectedMetricIndex, selectedColName, rowModel, overlayModel, z(selectedMetricIndex));
                        }
                      });

                      $("#overviewPanel").css({height: $("#overviewTable").height() + margin});

                      // metricData = overlayModel.metricColumns.filter(function(d) {
                      //   if (d != null) {
                      //     return d.columnName == selectedColName;
                      //   }
                      // })[0].metrics[selectedMetricIndex];

                      // redrawDetailView(theProject, metricData, datatablesHeader, selectedMetricIndex, selectedColName, rowModel, overlayModel);

                      drawDatatableScrollVis(theProject, rowModel, columnStore, overlayModel);
                      //todo: edit when selecting other metric

                      $("#btnReset").click(function() {
                        refillEditForm(metricData, selectedColName, selectedMetricIndex);
                      });

                      $("#btnSave").click(function() {
                        metricData.name = $("#metricName").text();
                        metricData.description = $("#metricDescription").text();
                        // var checks = $("#metricCheck");
                        var baseMetric = metricData.evaluables[0];
                        metricData.evaluables = [];
                        // metricData.evaluables.push(baseMetric);
                        for (var i = 0; i < $(".metricCheck").length; ++i) {
                          metricData.evaluables.push($("#eval" + (i)).val());
                        }
                        $.post("../../command/metric-doc/updateMetric?" + $.param(
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

  Sortable.create(checksList, { /* options */ });
});

function redrawDetailView(theProject, metricData, datatableHeader, selectedMetricIndex, selectedColName, rowModel, overlayModel, rectColor) {
  $('#detailViewHeader').text(selectedColName + " - " + overlayModel.availableMetrics[selectedMetricIndex]);

  d3.select("#heatmap").select("svg").remove();
  // heatmap drawing
  var headerHeightComp = datatableHeader - $('#detailViewHeader').height();
  var marginHeatmap = {top: headerHeightComp, right: 50, bottom: 70, left: 35};
  var width = parseInt(d3.select("#heatmap").style("width")) - marginHeatmap.left - marginHeatmap.right,
      height = $(".dataTables_scrollBody").height();
      // height = rawDataHeight - marginHeatmap.top;
  
  if (width > (metricData.evaluables.length*100)) width = metricData.evaluables.length*100;

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

  var xAxis = d3.svg.axis()
    .scale(xScale)
    .orient("bottom")
    .ticks(metricData.evaluables.length)
    .tickFormat(function(d) {
      var script = metricData.evaluables[d];
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

  var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left")
    .tickFormat(d3.format("d"));

  var svg = d3.select("#heatmap").append("svg")
    .attr("width", width + marginHeatmap.left + marginHeatmap.right)
    .attr("height", height + marginHeatmap.top + marginHeatmap.bottom)
    .append("g")
    .attr("transform", "translate(" + marginHeatmap.left + "," + marginHeatmap.top + ")");
  
  if (metricData.dirtyIndices != null) {

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
          return "transparent";
        } else {
          return rectColor;
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
      .style("font-size", 12)
      .call(wrap, width/metricData.evaluables.length)
      .style("text-anchor", "start")
      .attr("x", 6)
      .attr("y", 6);

    function wrap(text, width) {
      text.each(function() {
        var text = d3.select(this),
            words = text.text().split(/\s+/).reverse(),
            word,
            line = [],
            lineNumber = 0,
            lineHeight = 1.1, // ems
            y = text.attr("y"),
            dy = parseFloat(text.attr("dy")),
            tspan = text.text(null).append("tspan").attr("x", 0).attr("y", y).attr("dy", dy + "em");
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

    svg.append("g")
      .attr("class", "y axis")
      .call(yAxis);

    metricDetail.on("click", function(d) {
      console.log(d.index);
      $.post(
        "../../command/core/get-rows?" + $.param({ project: theProject.id, start: d.index, limit: 0 }) + "&callback=?",
        [],
        function(data) {
          var dataSet = [];
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
          return "transparent";
        } else {
          return rectColor;
        }
        
      });
      d3.select(this.parentNode).style("fill", "transparent");
    });
  }
}

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

  var z = d3.scale.ordinal()
    .range(colorbrewer.Reds[minScale])
    .domain([0, overlayModel.availableMetrics]);

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

  var bins = cols.selectAll(".metrics-bin").data(function(d) {
      if (d.dirtyIndices != null) {
        return d.dirtyIndices; 
      } else {
        return [];
      }
    })
    .enter().append("rect")
    .attr("class", "metrics-bin");

  /*
  .attr("x", function (d, i) {
      var col = this.parentNode.parentNode;
      var currentCol = columnStore.filter(function(column) {
        return col.__data__.columnName == column.title;
      })[0];
      return overlayX(columnStore.indexOf(currentCol)); 
    })
  */
  bins
    .attr("width", function (d, i) {
      return  12; 
    })
    .style("fill", function(d, i) {
      var metricsCol = this.parentNode.parentNode.__data__;
      var current = this.parentNode.__data__;
      return z(metricsCol.metrics.indexOf(current));
    });

  bins.each(function (d) {
    var ys = d3.select(this)
      .attr("y", overlayY(d.index))
      .attr("height", 1);
  });

  bins.on("click", function(d) {
    console.log(d.index);
    $.post(
      "../../command/core/get-rows?" + $.param({ project: theProject.id, start: d.index, limit: 500 }) + "&callback=?",
      [],
      function(data) {
        var columns = theProject.columnModel.columns;
        var dataSet = [];
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
        var dataTable = $('#dataset').dataTable();
        dataTable.fnClearTable();
        dataTable.fnAddData(dataSet);
      },
      "jsonp"
    );
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
  $("#metricName").text(d.name);
  $("#metricDescription").text(d.description);

  $(".metricCheck").remove();
  $("#typeDetail").remove();
  
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

  if (d.name == "validity") {
    $("#simpleList").append("<li class='list-group-item' id='typeDetail'><label>Detected Data Types (Placeholder)</label><table><tbody /></table></li>");
    var dataTypes = [{type: "String", val: 20}, {type: "Numeric", val: 70}, {type: "Date/Time", val: 3}, {type: "unknown", val: 7}];
    var tr = d3.select("#typeDetail").select("tbody").selectAll("tr")
      .data(dataTypes)
      .enter()
      .append("tr");
    tr.append("td")
      .text(function(d) {
        return d.type;
      });
    tr.append("td")
      .append("svg")
      .attr("width", 71)
      .attr("height", 12)
      .append("rect")
      .attr("width", function(d) {
        return (d.val/100)*71;
      })
      .attr("height", 12)
      .style("fill", "steelblue");
    // var types = d3.select("#typeDetail").append("div")
    //   .append("g")
    //   .selectAll("text")
    //   .data(dataTypes)
    //   .enter()
    //   .append("text")
    //   .text(function(data) { 
    //     return data.type; 
    //   });
    }

  if (d.evaluables.length > 0) {
    var metric = d.evaluables[0];
    metric = metric.substr(0, metric.indexOf("("));
    $("#base" + metric).prop("checked", true);
  }
  if (d.evaluables.length > 0) {
    for (var i = 0; i < d.evaluables.length; i++) {
      $("<li class='list-group-item pop metricCheck' data-toggle='popover'><label for='metricCheck" + i + "'>"
        + "</label><input class='form-control' id='eval"+(i)+"'/><button class='btn btn-default remove-btn'>Remove</button></li>").insertBefore("#addCheckButton");
      $("#eval" + i).val(d.evaluables[i]);
    }
    $(".remove-btn").click(function() {
      this.parentNode.remove();
    });
  }
}

function fillModalAfterColumnSelection(theProject) {
  var selectedCols = $("#columnFormMetricModal").val();
  $("#metricSelectMetricModal .btn").remove();
  d3.select("#typeDetailModal").select("tbody").selectAll("tr").remove();
  d3.select("#typeDetailModal").select("thead tr").selectAll("td").remove();

  var dataTypes;
  var metrics;
  if(selectedCols.length == 1) {
    dataTypes = [{type: "String", val: [20]}, {type: "Numeric", val: [70]}, {type: "Date/Time", val: [3]}, {type: "unknown", val: [7]}];
    metrics = theProject.overlayModels.metricsOverlayModel.availableMetrics;
  } else {
    dataTypes = [{type: "String", val: [20, 10]}, {type: "Numeric", val: [70, 85]}, {type: "Date/Time", val: [3, 1]}, {type: "unknown", val: [7, 4]}];
    metrics = theProject.overlayModels.metricsOverlayModel.availableSpanningMetrics;
  }
  if (selectedCols.length > 1) {
    d3.select("#typeDetailModal").select("thead tr")
    .selectAll('td')
    .data(selectedCols).enter()
    .append('td')
    .text(function(d) { 
      return d; 
    });
  }
  var tr = d3.select("#typeDetailModal").select("tbody").selectAll("tr")
    .data(dataTypes)
    .enter()
    .append("tr");
  tr.append("th")
    .text(function(d) {
      return d.type;
    });
  for(var selectedColIdx = 0; selectedColIdx < selectedCols.length; selectedColIdx++) {
    tr.append("td")
      .append("svg")
      .attr("width", 71)
      .attr("height", 12)
      .append("rect")
      .attr("width", function(d) {
        return (d.val[selectedColIdx]/100)*71;
      })
      .attr("height", 12)
      .style("fill", "steelblue");
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

function capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
}