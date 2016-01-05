$(document).ready(function() {
  //$('#demo').html( '<table cellpadding="0" cellspacing="0" border="0" class="display" id="example"></table>' );

  var dataSet = [];
  var columnStore = [];
  var colWidth = 50;

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
              "../../command/core/get-rows?" + $.param({ project: theProject.id, start: 0, limit: 100 }) + "&callback=?",
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
                  "../../command/metric-doc/get-metrics-overlay-model?" + $.param({ project: theProject.id }), null,
                  function(data) {
                    var overlayModel = data;
                    var margin = 20;

                    $.each(data.availableMetrics, function(index, value) {
                      $('#metricNames > tbody:last-child').append("<tr><td>" + value + "</td></tr>")
                    });

                    var datatablesHeader = $(".dataTables_scrollHead").height();
                    var rawDataHeight = $('#raw-data-container').height();
                    $("#raw-data-container").css({marginLeft: $("#metricNames").width()});
                    $("#overviewTable").css({width: $("#overviewPanel").width() - $("#metricNames").width() - margin});
                    $("#heatmap").css({marginTop: datatablesHeader});

                    colWidth = ($("#overviewPanel").width() - $("#metricNames").width() - margin)/columnStore.length;

                    //this reorders the metrics to be in line with the actual displayed columns
                    var sortedMetrics = new Array();
                    for(var idx = 0; idx < overlayModel.metricColumns.length; idx++) {
                      sortedMetrics[idx] = overlayModel.metricColumns.filter(function(col) {
                        return col.columnName == columnStore[idx].title;
                      })[0];
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
                        var metricName = this.parentNode.parentNode.parentNode.__data__;
                        var metricCurrent = d.metrics.filter(function(m) {
                          return m.name == metricName;
                        });
                        return metricCurrent[0].measure * colWidth;
                      });

                    $("#overviewPanel").css({height: $("#overviewTable").height()});

                    // heatmap drawing
                    var width = parseInt(d3.select("#heatmap").style("width")) - margin*2,
                        height = rawDataHeight - margin*2;
                    
                    var xScale = d3.time.scale()
                      .range([0, width])
                      .nice(d3.time.year);

                    var yScale = d3.scale.linear()
                      .range([height, 0])
                      .nice();

                    //var x = d3.time.scale()
                    var x = d3.scale.linear( )
                      .domain([0, 24])
                      .rangeRound([0, width]);

                    var y = d3.scale.linear()
                      .domain([rowModel.filtered, 0])
                      .rangeRound([height, 0]);

                    var z = d3.scale.linear()
                      .domain([0, 1])
                      .range(["white", "green"])
                      .interpolate(d3.interpolateRgb);

                    var formatTime = d3.time.format("%I %p"),
                      formatHour = function (d) {
                        if (d == 12) return "noon";
                        if (d == 24 || d == 0) return "midnight";
                        return formatTime(new Date(2013, 2, 9, d, 00));
                      };

                    var xAxis = d3.svg.axis()
                      .scale(x)
                      .orient("bottom")
                      .tickFormat(formatHour);

                    var yAxis = d3.svg.axis()
                      .scale(y)
                      .orient("left")
                      .tickFormat(d3.format("d"));

                    var metricData = data.metricColumns.filter(function(d) {
                      return d.columnName == "km/h";
                    })[0].metrics[0];

                    var svg = d3.select("#heatmap").append("svg")
                      .attr("width", width)
                      .attr("height", height)
                      .append("g"); //.attr("transform", "translate(" + margin.left + "," + margin.top + ")")
                    
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
                      .attr("transform", "translate(0," + height + ")")
                      .call(xAxis);

                    svg.append("g")
                      .attr("class", "y axis")
                      .call(yAxis);

                    function resize() {
                      var margin = 20
                          width = parseInt(d3.select("#heatmap").style("width")) - margin*2,
                          height = rawDataHeight - margin*2;

                      /* Update the range of the scale with new width/height */
                      xScale.range([0, width]).nice(d3.time.year);
                      yScale.range([height, 0]).nice();

                      /* Update the axis with the new scale */
                      // svg.select('.x.axis')
                      //     .attr("transform", "translate(0," + height + ")")
                      //     .call(xAxis);

                      // svg.select('.y.axis')
                      //     .call(yAxis);

                      /* Force D3 to recalculate and update the line */
                      svg.selectAll(".bin")
                          .attr("d", bins);
                    }

                    d3.select(window).on('resize', resize);

                    $('#dataset').dataTable().fnDraw();
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

  $("[data-toggle=popover]").popover({
    html: 'true',
    trigger: 'manual',
    placement: 'auto left',
    animation: 'false',
    content: '<div class="btn-group" role="group"><button type="button" class="btn btn-danger">remove</button><button type="button" class="btn btn-warning">edit</button></div>'
    // content:'<span class="label label-warning">Warning</span><span class="label label-danger">Danger</span>'
  }).on("mouseenter", function () {
    var _this = this;
    $(this).popover("show");
    $(".popover").on("mouseleave", function () {
        $(_this).popover('hide');
    });
  }).on("mouseleave", function () {
    var _this = this;
    setTimeout(function () {
        if (!$(".popover:hover").length) {
            $(_this).popover("hide");
        }
    }, 300);
  });
/*
  $.getJSON("../../command/core/get-columns-info?" + $.param({ project: theProject.id }),function(data) {
    for (var col = 0; col < data.length; col++) {
      var column = data[col];
      columns[col] = {"title": column.name};
    }

    var dataCols = dataSet[0];

    if(dataCols) {
      if(dataCols.length == columns.length) {
        $('#dataset').dataTable( {
          "data": dataSet,
          "columns": columns
        } );
      }
    }

    //load column names into the modal
    for(var col = 0; col < columns.length; col++) {
      var colName = columns[col];
      $("<option />")
     .attr("value", colName.title)
     .attr("label", colName.title)
     .appendTo("#columnFormMetricModal");
    }
  }); */
} );

function showMetric() {
  params = { "column_name": "ID",
    project: 2421247403318
  };
  body = {};
  updateOptions = {};
  callbacks = {
    "onDone": function(response) {
      doStatsDialog(response);
    }
  }

  $.post("../../command/metric-doc/completeness", params, function(response) {
    var dialog = $(DOM.loadHTML("metric-doc", "../../scripts/completeness.html"));

    var elmts = DOM.bind(dialog);
    elmts.dialogHeader.text("Metrics for column \"" + params.column_name + "\"");

    if (response["measure"]) { elmts.dialogCompleteness.text(response["measure"]) };

    // var level = DialogSystem.showDialog(dialog);
    // elmts.okButton.click(function() {
    //   DialogSystem.dismissUntil(level - 1);
    // });
  });
}