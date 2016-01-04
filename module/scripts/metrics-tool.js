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

                $("#raw-data-container").css({marginLeft: $("#metricNames").width()});
                $("#overviewTable").css({width: $("#overviewPanel").width() - $("#metricNames").width()});

                $.getJSON(
                  "../../command/metric-doc/get-metrics-overlay-model?" + $.param({ project: theProject.id }), null,
                  function(data) {
                    var overlayModel = data;

                    $.each(data.availableMetrics, function(index, value) {
                      $('#metricNames > tbody:last-child').append("<tr><td>" + value + "</td></tr>")
                    });

                    colWidth = ($("#overviewPanel").width() - $("#metricNames").width())/columnStore.length;

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


                    var margin = 20
                        width = parseInt(d3.select("#heatmap").style("width")) - margin*2,
                        height = parseInt(d3.select("#heatmap").style("height")) - margin*2;
                    
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
                      .domain([0, rowModel.filtered])
                      .rangeRound([height, 0]);

                    var z = d3.scale.linear()
                      .domain([0, 1])
                      .range(["white", "green"])
                      .interpolate(d3.interpolateHcl);

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
                      return d.columnName == "Altitude";
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
                        .attr("height", 6 );
                    });

                    svg.append("g")
                      .attr("class", "x axis")
                      .attr("transform", "translate(0," + height + ")")
                      .call(xAxis);

                    svg.append("g")
                      .attr("class", "y axis")
                      .call(yAxis);
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

  // var margin = {top: 20, right: 20, bottom: 30, left: 40},
  var pancreas = [
    {
      "values": [
        6, 
        0, 
        10, 
        12, 
        5, 
        20, 
        39, 
        26, 
        37, 
        21, 
        7, 
        4, 
        0, 
        2, 
        0, 
        7, 
        1, 
        1, 
        12, 
        3, 
        1, 
        5, 
        4, 
        8
      ], 
      "key": 50
    }, 
    {
      "values": [
        4, 
        5, 
        13, 
        3, 
        29, 
        49, 
        54, 
        42, 
        50, 
        37, 
        10, 
        14, 
        13, 
        13, 
        15, 
        15, 
        9, 
        12, 
        19, 
        8, 
        17, 
        19, 
        10, 
        9
      ], 
      "key": 60
    }, 
    {
      "values": [
        18, 
        39, 
        61, 
        39, 
        42, 
        41, 
        54, 
        99, 
        96, 
        89, 
        89, 
        55, 
        46, 
        36, 
        17, 
        5, 
        13, 
        28, 
        22, 
        22, 
        24, 
        27, 
        31, 
        11
      ], 
      "key": 70
    }, 
    {
      "values": [
        33, 
        69, 
        47, 
        59, 
        112, 
        108, 
        128, 
        150, 
        140, 
        157, 
        125, 
        115, 
        62, 
        44, 
        48, 
        42, 
        18, 
        36, 
        35, 
        66, 
        62, 
        90, 
        89, 
        49
      ], 
      "key": 80
    }, 
    {
      "values": [
        82, 
        76, 
        59, 
        109, 
        58, 
        102, 
        106, 
        98, 
        96, 
        137, 
        98, 
        60, 
        54, 
        46, 
        66, 
        65, 
        45, 
        73, 
        81, 
        59, 
        77, 
        112, 
        74, 
        92
      ], 
      "key": 90
    }, 
    {
      "values": [
        76, 
        37, 
        64, 
        79, 
        102, 
        83, 
        93, 
        68, 
        81, 
        83, 
        98, 
        91, 
        82, 
        96, 
        74, 
        75, 
        102, 
        107, 
        81, 
        109, 
        100, 
        78, 
        65, 
        79
      ], 
      "key": 100
    }, 
    {
      "values": [
        70, 
        40, 
        47, 
        90, 
        57, 
        72, 
        60, 
        93, 
        89, 
        65, 
        91, 
        65, 
        85, 
        86, 
        98, 
        92, 
        114, 
        111, 
        113, 
        83, 
        75, 
        60, 
        85, 
        68
      ], 
      "key": 110
    }, 
    {
      "values": [
        68, 
        42, 
        57, 
        39, 
        52, 
        76, 
        62, 
        40, 
        45, 
        32, 
        35, 
        66, 
        73, 
        98, 
        96, 
        108, 
        125, 
        96, 
        82, 
        72, 
        57, 
        63, 
        65, 
        70
      ], 
      "key": 120
    }, 
    // {
    //   "values": [
    //     64, 
    //     72, 
    //     52, 
    //     49, 
    //     66, 
    //     34, 
    //     30, 
    //     44, 
    //     36, 
    //     40, 
    //     32, 
    //     66, 
    //     68, 
    //     99, 
    //     104, 
    //     92, 
    //     100, 
    //     60, 
    //     53, 
    //     40, 
    //     50, 
    //     47, 
    //     55, 
    //     43
    //   ], 
    //   "key": 130
    // }, 
    // {
    //   "values": [
    //     49, 
    //     55, 
    //     19, 
    //     40, 
    //     32, 
    //     18, 
    //     26, 
    //     20, 
    //     35, 
    //     32, 
    //     49, 
    //     56, 
    //     49, 
    //     77, 
    //     56, 
    //     67, 
    //     72, 
    //     69, 
    //     53, 
    //     55, 
    //     47, 
    //     37, 
    //     35, 
    //     40
    //   ], 
    //   "key": 140
    // }, 
    // {
    //   "values": [
    //     36, 
    //     41, 
    //     68, 
    //     20, 
    //     26, 
    //     31, 
    //     27, 
    //     15, 
    //     10, 
    //     13, 
    //     29, 
    //     57, 
    //     80, 
    //     41, 
    //     51, 
    //     54, 
    //     44, 
    //     41, 
    //     30, 
    //     45, 
    //     44, 
    //     33, 
    //     14, 
    //     34
    //   ], 
    //   "key": 150
    // }, 
    {
      "values": [
        21, 
        29, 
        52, 
        22, 
        20, 
        25, 
        17, 
        11, 
        12, 
        24, 
        24, 
        18, 
        53, 
        24, 
        44, 
        43, 
        47, 
        43, 
        29, 
        35, 
        45, 
        42, 
        17, 
        41
      ], 
      "key": 160
    }, 
    {
      "values": [
        28, 
        44, 
        37, 
        26, 
        22, 
        28, 
        7, 
        11, 
        8, 
        15, 
        12, 
        8, 
        16, 
        13, 
        29, 
        22, 
        10, 
        10, 
        20, 
        29, 
        29, 
        21, 
        15, 
        26
      ], 
      "key": 170
    }, 
    {
      "values": [
        32, 
        39, 
        29, 
        38, 
        17, 
        18, 
        7, 
        7, 
        8, 
        6, 
        14, 
        7, 
        14, 
        22, 
        6, 
        9, 
        4, 
        9, 
        21, 
        17, 
        32, 
        23, 
        25, 
        16
      ], 
      "key": 180
    }, 
    {
      "values": [
        40, 
        28, 
        29, 
        28, 
        18, 
        7, 
        10, 
        4, 
        11, 
        3, 
        2, 
        12, 
        4, 
        14, 
        3, 
        12, 
        1, 
        5, 
        12, 
        27, 
        17, 
        16, 
        14, 
        26
      ], 
      "key": 190
    }, 
    {
      "values": [
        18, 
        26, 
        20, 
        42, 
        17, 
        0, 
        0, 
        4, 
        1, 
        0, 
        5, 
        8, 
        2, 
        3, 
        1, 
        4, 
        0, 
        4, 
        11, 
        22, 
        11, 
        17, 
        25, 
        14
      ], 
      "key": 200
    }, 
    {
      "values": [
        11, 
        15, 
        16, 
        18, 
        11, 
        2, 
        0, 
        5, 
        0, 
        0, 
        12, 
        5, 
        3, 
        3, 
        2, 
        3, 
        0, 
        2, 
        25, 
        10, 
        12, 
        9, 
        25, 
        25
      ], 
      "key": 210
    }, 
    {
      "values": [
        9, 
        14, 
        16, 
        12, 
        11, 
        2, 
        0, 
        6, 
        0, 
        0, 
        0, 
        1, 
        1, 
        4, 
        4, 
        4, 
        0, 
        0, 
        10, 
        8, 
        6, 
        7, 
        13, 
        6
      ], 
      "key": 220
    }, 
    {
      "values": [
        5, 
        3, 
        13, 
        3, 
        16, 
        8, 
        10, 
        4, 
        0, 
        0, 
        0, 
        0, 
        6, 
        5, 
        7, 
        1, 
        3, 
        0, 
        4, 
        10, 
        8, 
        11, 
        5, 
        1
      ], 
      "key": 230
    }, 
    {
      "values": [
        2, 
        5, 
        5, 
        2, 
        10, 
        8, 
        4, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        4, 
        2, 
        4, 
        0, 
        1, 
        2, 
        3, 
        1, 
        1, 
        2
      ], 
      "key": 240
    }, 
    {
      "values": [
        0, 
        6, 
        4, 
        5, 
        0, 
        1, 
        5, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        4, 
        5, 
        3, 
        0, 
        7, 
        1, 
        5, 
        1, 
        9, 
        0
      ], 
      "key": 250
    }, 
    {
      "values": [
        0, 
        5, 
        4, 
        1, 
        0, 
        1, 
        4, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        3, 
        1, 
        5, 
        3, 
        2, 
        2, 
        0, 
        0, 
        0
      ], 
      "key": 260
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        1, 
        0, 
        1, 
        4, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        1, 
        2, 
        8, 
        1, 
        3, 
        0, 
        1, 
        0, 
        0
      ], 
      "key": 270
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        3, 
        1, 
        1, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        1, 
        1, 
        0, 
        3, 
        0, 
        0, 
        1, 
        0
      ], 
      "key": 280
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        2, 
        6, 
        3, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        4, 
        1, 
        3, 
        0, 
        0, 
        0, 
        0, 
        1
      ], 
      "key": 290
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        2, 
        7, 
        4, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        6, 
        4, 
        1, 
        0, 
        0, 
        0, 
        1, 
        3
      ], 
      "key": 300
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        5, 
        4, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        5, 
        0, 
        0, 
        0, 
        0, 
        3, 
        2
      ], 
      "key": 310
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        2, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        3, 
        5
      ], 
      "key": 320
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        4, 
        1
      ], 
      "key": 330
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0
      ], 
      "key": 340
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0
      ], 
      "key": 350
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0
      ], 
      "key": 360
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0
      ], 
      "key": 370
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0
      ], 
      "key": 380
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0
      ], 
      "key": 390
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0
      ], 
      "key": 400
    }, 
    {
      "values": [
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0, 
        0
      ], 
      "key": 410
    }
  ];

  function resize() {
    var width = parseInt(d3.select("#heatmap").style("width")) - margin*2,
        height = parseInt(d3.select("#heatmap").style("height")) - margin*2;

    /* Update the range of the scale with new width/height */
    xScale.range([0, width]).nice(d3.time.year);
    yScale.range([height, 0]).nice();

    /* Update the axis with the new scale */
    svg.select('.x.axis')
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    svg.select('.y.axis')
        .call(yAxis);

    /* Force D3 to recalculate and update the line */
    svg.selectAll(".bin")
        .attr("d", bins);
  }

  d3.select(window).on('resize', resize);

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