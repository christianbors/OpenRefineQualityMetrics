function redrawDetailView(theProject, metricData, selectedMetricIndex, rowModel, overlayModel, height, width, marginHeatmap) {
  d3.select("#heatmap").select("svg").remove();
  
  var axisWidths = [];
  axisWidths.push(0);
  // initialize selected metric evaluables

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
    .range([height, 0])
    .nice();

  var x = d3.scale.ordinal( )
    .domain(ordinalScale)
    .range(axisWidths);

  var y = d3.scale.linear()
    .domain([rowModel.filtered, 0])
    .rangeRound([height, 0]);

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
    .scale(y)
    .orient("left")
    .tickFormat(d3.format("d"));

  var svg = d3.select("#heatmap").append("svg")
    .attr("width", width + marginHeatmap.left + marginHeatmap.right)
    .attr("height", height + marginHeatmap.top + marginHeatmap.bottom)
    .append("g")
    .attr("transform", "translate(" + marginHeatmap.left + "," + marginHeatmap.top + ")");
  
  svg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + (height) + ")")
    .attr("x", height)
    .attr("y", 0)
    .call(xAxis)
  .selectAll(".tick text")
    .style("font-size", 12)
    .call(wrap, width/totalEvalTuples.length)
    .style("text-anchor", "start")
    .attr("x", 6)
    .attr("y", 6);


  detailWidth = width/totalEvalTuples.length;

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
    .attr("width", width/totalEvalTuples.length)
    .attr("fill-opacity", 0)
    .attr("cursor", "ew-resize")
    .call(drag);

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
    if(push) dirtyArray.push(dirtyEntry);
  }

  if (dirtyArray.length > 0) {

    var metricDetail = svg.selectAll(".metric-detail-row")
      .data(dirtyArray)
      .enter( ).append("g")
      .attr("class", "metric-detail-row");

    var bins = metricDetail.selectAll(".bin")
        .data(function (d) { return d.dirty; })
        .enter( ).append("rect")
        .attr("class", "bin");

    bins.attr("x", function (d, i) {
      return x(i);
    }).attr("width", function (d, i) {
      return x(i + 1) - x(i);
    }).style("fill", function(d, i) {
      if (d == true) {
        return "transparent";
      } else {
        return z(selectedColOpacity[i] * selectedMetricIndex);
      }
    });
    // .style("opacity", function(d, i) {
    //   return selectedColOpacity[i];
    // })

    metricDetail.each(function (d) {
      var ys = d3.select(this).selectAll(".bin")
        .attr("y", y(d.index) )
        .attr("height", 1);
    });

    metricDetail.on("click", function(d) {
      $("#dataset td").removeClass("highlight");
      var attrY = d3.select(this.firstChild).attr("y");
      var selectedRows = d3.selectAll("g.metric-detail-row").filter(function(r) {
        return d3.select(this.firstChild).attr("y") === attrY;
      })[0];
      $("#dataset").DataTable().row(selectedRows[0].__data__.index).scrollTo();
      $.each(selectedRows, function(i, rowCurrent) {
        $.each($("#dataset").DataTable().row(rowCurrent.__data__.index).node().children, function(i, td) {
          td.classList.add("highlight");
        });
      });
    });

    $('.dataTables_scrollBody').on('scroll', function() {
      var regex = /(\d+)/g;
      var nums = $(".dataTables_info").text().replace(/,/g, "").match(regex);
      d3.select("rect.posHighlight").remove();

      var detailHeat = d3.select("#heatmap svg g").append("rect")
        .classed("posHighlight", true)
        .attr("x", 0)
        .attr("y", y(nums[0]))
        .attr("width", width)
        .attr("height", y(nums[1]) - y(nums[0]));
    });

    bins.on("mouseover", function(d) {
      d3.select(this.parentNode).selectAll("rect").style("fill", "steelblue");
      var attrY = d3.select(this).attr("y");
      var sameRows = d3.selectAll("g.metric-detail-row").filter(function(r) {
        return d3.select(this.firstChild).attr("y") === attrY;
      })[0];
      if(sameRows.length > 1) {
        var indices = {
          first: sameRows[0].__data__.index,
          last: sameRows[sameRows.length-1].__data__.index
        };
        tooltipInvalid.show(indices);
      } else {
        tooltipInvalid.show(this.parentNode.__data__.index);
      }
      // d3.select(this.parentNode).style("fill", "black");
    });

    bins.on("mouseout", function(d) {
      d3.select(this.parentNode).selectAll("rect").style("fill", function(d) {
        if (d == true) {
          return "transparent";
        } else {
          return z(selectedMetricIndex);
        }
        
      });
      d3.select(this.parentNode).style("fill", "transparent");
      tooltipInvalid.hide();
    });
  }
}