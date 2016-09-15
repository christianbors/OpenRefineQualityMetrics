var detailViewY;
var maxErrorDens = 0;

function redrawDetailView(theProject, metricData, rowModel) {
  d3.select("#heatmap").select("svg").remove();
  $("#filtering").show();
  
  var axisWidths = [];
  maxErrorDens = 0;
  axisWidths.push(0);
  // initialize selected metric evaluables
  var headerHeightComp = $(".dataTables_scrollhead").height() - ($("#legend").height() + $("#legend")[0].offsetTop);
  detailViewMargin = {top: headerHeightComp, right: 50, bottom: 20, left: 35};
  detailViewWidth = parseInt(d3.select("#heatmap").style("width")) - detailViewMargin.left - detailViewMargin.right,
  detailViewHeight = $(".dataTables_scrollBody").height();
  if (detailViewWidth > (totalEvalTuples.length*100)) detailViewWidth = totalEvalTuples.length*100;
  detailWidths = [];

  for (var i = 0; i < totalEvalTuples.length; i++) {
    detailWidths.push(detailViewWidth/totalEvalTuples.length);
  }
  for (var i = 1; i <= totalEvalTuples.length; i++) {
    axisWidths.push(detailWidths[i-1] + axisWidths[i-1]);
  }
  var ordinalScale = [];
  for (var i = 0; i <= totalEvalTuples.length; i++) ordinalScale.push(i);

  // heatmap drawing
  var xScale = d3.scale.ordinal()
    .domain(ordinalScale)
    .range(axisWidths);

  var yScale = d3.scale.linear()
    .range([detailViewHeight, 0])
    .nice();

  var x = d3.scale.ordinal( )
    .domain(ordinalScale)
    .range(axisWidths);

  detailViewY = d3.scale.linear()
    .domain([rowModel.filtered, 0])
    .rangeRound([detailViewHeight, 0]);

  var xAxis = d3.svg.axis()
    .scale(xScale)
    .orient("bottom")
    .ticks(totalEvalTuples.length)
    .tickFormat(function(d) {
      if(d < totalEvalTuples.length) {
        var script = totalEvalTuples[d].evaluable;
        if (script != null) {
          var label;
          for (var i = 0; i < metricData.length; i++) {
            if (script.toLowerCase().indexOf(metricData[i].name) < 0) {
              label = script;
            } else {
              label = metricData[i].name;
              break;
            }
          }
          return label;
        }
      }
    });

  var yAxis = d3.svg.axis()
    .scale(detailViewY)
    .orient("left")
    .tickFormat(d3.format("d"));

  var svg = d3.select("#heatmap").append("svg")
    .attr("width", detailViewWidth + detailViewMargin.left + detailViewMargin.right)
    .attr("height", detailViewHeight + detailViewMargin.top + detailViewMargin.bottom)
    .append("g")
    .attr("transform", "translate(" + detailViewMargin.left + "," + detailViewMargin.top + ")");
  
  svg.append("rect")
    .attr("x", 0)
    .attr("y", 0)
    .attr("height", detailViewHeight)
    .attr("width", detailViewWidth)
    .attr("class", "rect-disabled")
    .attr("fill", "transparent");
  //TODO: create arrays that contain all dirty indices
  var dirtyArray = []
  for (var dirtyIdx = 0; dirtyIdx < rowModel.filtered; dirtyIdx++) {
    var dirtyEntry = {"index": dirtyIdx, "dirty":[]};
    var push = false;
    for(var metricIdx = 0; metricIdx < metricData.length; metricIdx++) {
      if(metricData[metricIdx].dirtyIndices != null) {
        var dirtyRow = metricData[metricIdx].dirtyIndices.filter(function(d) {
          return d.index == dirtyIdx;
        })[0];
        if (dirtyRow != null) {
          push = true;
          if(dirtyIdx > 0 && dirtyEntry.dirty.length == 0) {
            var prevIdx = metricIdx - 1;
            if(prevIdx >= 0) {
              for(var prevIdx = 0; prevIdx < metricIdx; prevIdx++) {
                var emptyArray = []
                for (var falseIdx = 0; falseIdx < metricData[prevIdx].evalTuples.length; falseIdx++) {
                  emptyArray.push(true);
                }
                dirtyEntry.dirty.push.apply(dirtyEntry.dirty, emptyArray)
              }
            }
          }
          dirtyEntry.dirty.push.apply(dirtyEntry.dirty, dirtyRow.dirty);
        }
      }
    }
    if(push) {
      if(dirtyArray[detailViewY(dirtyIdx)] == null) {
        dirtyArray[detailViewY(dirtyIdx)] = {};
        dirtyArray[detailViewY(dirtyIdx)].dirty = [];
        dirtyArray[detailViewY(dirtyIdx)].index = [];
      }
      dirtyArray[detailViewY(dirtyIdx)].index.push(dirtyEntry.index);
      $.each(dirtyEntry.dirty, function(i, v) {
        if(dirtyArray[detailViewY(dirtyIdx)].dirty[i] == null) {
          dirtyArray[detailViewY(dirtyIdx)].dirty[i] = []  
        }
        dirtyArray[detailViewY(dirtyIdx)].dirty[i].push(v);
        if(dirtyArray[detailViewY(dirtyIdx)].dirty[i].length > maxErrorDens) {
          maxErrorDens = dirtyArray[detailViewY(dirtyIdx)].dirty[i].length;
        }
      })
      // dirtyArray.push(dirtyEntry);
    }
  }

  if (dirtyArray.length > 0) {

    var metricDetail = svg.selectAll(".metric-detail-row")
      .data(dirtyArray)
      .enter()
      .append("g")
      .attr("class", "metric-detail-row");

    var bins = metricDetail.selectAll(".bin")
        .data(function (d) {
          if(d != null) {
            return d.dirty;
          } else {
            return [];
          }
        })
        .enter( ).append("rect")
        .attr("class", "bin");

    bins.attr("x", function (d, i) {
      return x(i);
    }).attr("width", function (d, i) {
      return x(i + 1) - x(i);
    }).style("fill", function(d, i) {
      if (d.indexOf(false) == -1) {
        return "white";
      } else {
        return fillMetricColor(selectedChecks[i]);
      }
    }).style("opacity", function(d, i) {
      // return 0.25;
      var count = d.reduce(function(n, val) {
        return n + (val === false);
      }, 0);
      return count/maxErrorDens;
    });

    var separatorPos = [];
    var separatorIdx = 0;
    for (var m = 0; m < metricData.length-1; m++) {
      var curM = metricData[m];
      var separatorWidth = 0;
      if(m > 0) {
        separatorWidth += separatorPos[m-1];
      }
      for (var i = 0; i < curM.evalTuples.length; i++) {
        separatorWidth += detailWidths[separatorIdx];
        separatorIdx++;
      }
      separatorPos.push(separatorWidth);
    }

    // svg.selectAll(".separator")
    svg.selectAll(".separator")
        .data(separatorPos)
        .enter().append("line")
        .attr("y1", 0)
        .attr("y2", detailViewHeight + 6)
        .attr("x1", function(d, i) {
          return separatorPos[i];
        })
        .attr("x2", function(d, i) {
          return separatorPos[i];
        })
        .attr("stroke", "black")
        .attr("stroke-width", "2")
        .attr("class", "separator");
    // .style("opacity", function(d, i) {
    //   return selectedColOpacity[i];
    // })
    svg.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + (detailViewHeight) + ")")
      .attr("x", detailViewHeight)
      .attr("y", 0)
      .call(xAxis)
    .selectAll(".tick text")
      .style("font-size", 12)
      .call(wrap, detailViewWidth/totalEvalTuples.length)
      .style("text-anchor", "start")
      .attr("x", 6)
      .attr("y", 6);

    detailWidth = detailViewWidth/totalEvalTuples.length;

    var axis = d3.selectAll("g.x.axis g.tick");

    var drag = d3.behavior.drag().origin(Object).on("drag", detaildragresize).on("dragend", detaildragdone);

    dragbarbottom = axis.filter(function(d, i) {
        return d < totalEvalTuples.length;
      })
      .append("rect")
      .attr("x", function(d) { 
        return d.x; })
      .attr("y", function(d) { 
        return d.y; })
      .attr("id", "dragright")
      .attr("height", dragbarw)
      .attr("width", detailViewWidth/totalEvalTuples.length)
      .attr("fill-opacity", 0)
      .attr("cursor", "ew-resize")
      .call(drag);

    svg.append("g")
      .attr("class", "y axis")
      .call(yAxis);

    metricDetail.each(function (d, i) {
      var ys = d3.select(this).selectAll(".bin")
        .attr("y", i )
        .attr("height", 1);
    });

    metricDetail.on("click", function(d) {
      $("#dataset td").removeClass("highlight");

      var thisSelected = d.index[0];
      var bodyHeight = $("#dataset tbody").height();

      var regex = /(\d+)/g;
      var nums = $(".dataTables_info").text().replace(/,/g, "").match(regex);
      var from = parseInt(nums[0]) - 1;
      var to = parseInt(nums[1]) - 1;

      if(from > thisSelected || to < thisSelected) {
        var page = Math.floor(thisSelected / pageLength);
        $('#dataset').DataTable().page(page).draw('page');
      }

      var selectedRowPosTop;
      $.each(d.index, function(i, index) {
        if(i == 0) {
          $('div.dataTables_scrollBody').animate({
            scrollTop: $("#dataset").DataTable().row((index)).node().offsetTop
          }, 500);
        }
        $.each($("#dataset").DataTable().row((index)).node().children, function(i, td) {
          td.classList.add("highlight");
        });
      });
    });

    bins.on("mouseover", function(d) {
      d3.select(this.parentNode).selectAll("rect").style("fill", "steelblue");
      var attrY = d3.select(this).attr("y");
      tooltipInvalid.show(this.parentNode.__data__);

      d3.select(this.parentNode).style("fill", "black");
    });

    bins.on("mouseout", function(d) {
      $("#dataset tr").removeClass("hover");
      d3.select(this.parentNode).selectAll("rect").style("fill", function(d, i) {
        if (d == true) {
          return "white";
        } else {
          return fillMetricColor(selectedChecks[i]);
        }
        
      });
      d3.select(this.parentNode).style("fill", "white");
      tooltipInvalid.hide();
    });

    var regex = /(\d+)/g;
    var ratio = this.scrollTop/this.scrollHeight;
    var nums = $(".dataTables_info").text().replace(/,/g, "").match(regex);
    var total = nums[nums.length-1];
    var pos = ratio * total;
    d3.select("rect.posHighlight").remove();

    var detailHeat = d3.select("#heatmap svg g").append("rect")
      .classed("posHighlight", true)
      .attr("x", 0)
      .attr("y", detailViewY(nums[0]))
      .attr("width", detailViewWidth)
      .attr("height", detailViewY(nums[1]) - detailViewY(nums[0]));
  }
}