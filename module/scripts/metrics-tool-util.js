var editButton;
var timerID = null;

function addEvaluableEntry(evalTuple) {
	var i = $(".metricCheck").length;
	$("#checksList").append("<li class='list-group-item metricCheck form-group' id='metricEvaluable" + i + "'><div class='input-group' value='" + i + "' id='container" + i + "'></div></li>");
  $("#container" + i).append("<span class='input-group-addon' value='" + i + "' id='edit"+ i +"' data-toggle='popover'>edit</span>");
  $("#container" + i).bind("keyup change input",function(){
    if (event.which != 13) {
      scheduleUpdate(this);
    }
  })
  $("#container" + i).append("<input data-toggle='tooltip' type='text' class='form-control pop metricInput' placeholder='Check' id='eval"+i+"'/>  ");
  $("#eval" + i).keypress(function(event){
    if (event.which == 13) {
      $("#eval" + i).blur();
      window.clearTimeout(timerID);
      if(metricData[0].spanningEvaluable != null) {
        if(i == 0) metricData[0].spanningEvaluable.evaluable = this.value;
        else metricData[0].evalTuples[i-1].evaluable = this.value;
      } else {
        metricData[0].evalTuples[i].evaluable = this.value;
      }
      updateMetric();
    }
  });
  var disableButtonClass = "disable";
  if(evalTuple.disabled) {
    $("#eval" + i).addClass("disabled");
    disableButtonClass = "enable";
  }

  $("#edit" + i).popover({
    html: 'true',
    trigger: 'manual',
    placement: 'auto top',
    animation: 'false',
    container: 'body',
    content: '<div class="btn-group" role="group"><button type="button" class="btn btn-danger" id="remove-eval">remove</button>'+
      '<button type="button" class="btn btn-default" id="disable-eval">' + disableButtonClass + '</button>'+
      '<button type="button" class="btn btn-default" id="comment-eval">comment</button></div>'
  }).on("click", function () {
    selectedEditEvaluable = $(this).attr("value");
    editButton = this;
    $(this).popover("toggle");
    $(".popover").on("mouseleave", function () {
        $(editButton).popover('hide');
    });
  });

  if (evalTuple != "") {
      $("#eval" + i).val(evalTuple.evaluable);
  }
}

function dataViewPopover() {
  var headers = $("div.dataTables_scrollHeadInner > table.dataTable > thead > tr > th.sorting_disabled");
  headers.addClass("popoverHeader");
  headers.attr("data-toggle", "popover");
  //'</ul>';// role="group"><button type="button" class="btn btn-danger" id="remove-eval">remove</button>'+
      //'<button type="button" class="btn" id="disable-eval">tyst[er</button>'+
      //'<button type="button" class="btn btn-warning" id="comment-eval">comment</button></div>'
  headers.popover({
    html: 'true',
    trigger: 'manual',
    placement: 'auto top',
    animation: 'false',
    container: 'body',
    title: 'Show/Hide Metric Overlay',
    content: ''
  });
  d3.selectAll("th.sorting_disabled").on("contextmenu", function () {
    d3.event.preventDefault();
    var _this = this;
    var colIdx;
    for (var i = 0; i < _this.parentNode.childNodes.length; i++) {
        if (this.parentNode.childNodes[i] == _this) {
            // -1 because we have an empty column as well
            colIdx = i - 1;
            break;
        }
    }
    var popoverColumn = overlayModel.metricColumns.filter(function(col) {
      if (col != null) {
        return col.columnName == _this.textContent;
      }
    })[0];
    var metricsArray = $.map(popoverColumn.metrics, function(value, index) {
      return [value];
    });
    var spanMetricsArray = overlayModel.spanningMetrics.filter(function(metric) {
      if(metric != null) {
        return metric.spanningColumns.indexOf(_this.textContent) >= 0;
      }
    });

    $.each(spanMetricsArray, function(i, m) {
      metricsArray.push(m);
    });

    var popoverSnippet = '';
    for(var i = 0; i < metricsArray.length; i++) {
      var checked = "checked='checked'";
      if($($("g.metrics-overlay")[colIdx]).find("g." + metricsArray[i].name).css('display') == 'none') 
        checked = "";

      if (metricsArray[i].spanningColumns != null) {
        popoverSnippet += "<div class='checkbox'><label><input " + checked + 
          " id='" + metricsArray[i].spanningColumns.indexOf(_this.textContent) + "' class='dataview-popover' type='checkbox'>" + 
          metricsArray[i].name + "</label></div>";
      } else {
        popoverSnippet += "<div class='checkbox'><label><input " + checked + 
          " id='" + colIdx + "' class='dataview-popover' type='checkbox'>" + 
          metricsArray[i].name + "</label></div>";
      }
    }
    $(this).data("bs.popover").options.content = popoverSnippet;

    $(this).popover("toggle");
    $(".popover").on("mouseleave", function () {
        $(_this).popover('hide');
    });
  });
}

function fillMetricColor(metricName) {
  if(overlayModel.availableSpanningMetrics.indexOf(metricName) != -1) {
    return z(overlayModel.availableMetrics.length + overlayModel.availableSpanningMetrics.indexOf(metricName));
  } else if(overlayModel.availableMetrics.indexOf(metricName) != -1) {
    return z(overlayModel.availableMetrics.indexOf(metricName));
  }
  return z(overlayModel.availableMetrics.length + overlayModel.availableSpanningMetrics.length);
}

var metricChange = "none";
function updateMetric() {
  var metricShort = metricData[0];
  metricShort.dirtyIndices = [];

  var updateMetricIndex = -1;
  if (metricType[0] === "spanning") {
    updateMetricIndex = overlayModel.spanningMetrics.indexOf(metricData[0]);
  }

  $.getJSON("../../command/metric-doc/updateMetric?" + $.param(
	        {
            metric: metricShort,
            column: selectedColName[0],
            metricEvalCount: metricData[0].evalTuples.length,
	          project: theProject.id
	        }),
	      {},
	      function(response) {
	        var param = {
            project: theProject.id, 
            column: selectedColName[0],
            metric: response
          };
          $.post("../../command/metric-doc/evaluateSelectedMetric?" + $.param(param), null, function(data) {
            var oldMetric = metricData[0];
            metricData[0] = data;
            var infoText = "";
            $("#alertMetricUpdate").removeClass("alert-danger");
            $("#alertMetricUpdate").addClass("alert-info");
            if(metricChange === "concat") {
              infoText += "Concatenation changed to: " + data.concat + ", metric value changed to <strong>" + data.measure + "</strong>";
              $("#alertText").html(infoText);
              $("#alertMetricUpdate").show(); //prop("hidden", false)
            } else if (metricChange = "disableEval") {
              infoText += "Check disabled: metric value changed to <strong>" + data.measure + "</strong>.";
              $("#alertText").html(infoText);
              $("#alertMetricUpdate").show(); //prop("hidden", false)
            } else if (metricChange = "removeEval") {
              infoText += "Check removed: metric value changed to <strong>" + data.measure + "</strong>";
              $("#alertText").html(infoText);
              $("#alertMetricUpdate").show(); //prop("hidden", false)
            } else if (metricChange = "edit") {
              if(oldMetric.dirtyIndices != null && data.dirtyIndices != null) {
                if(oldMetric.dirtyIndices == null || oldMetric.dirtyIndices.length < data.dirtyIndices.length) {
                  infoText += "<strong>" + data.dirtyIndices.length + " new</strong> dirty entries have been found";
                } else if (oldMetric.dirtyIndices.length < data.dirtyIndices.length) {
                  infoText += "<strong>" + (data.dirtyIndices.length - oldMetric.dirtyIndices.length) + " new</strong> dirty entries have been found";
                } else if (oldMetric.dirtyIndices.length > data.dirtyIndices.length) {
                  infoText += "<strong>" + (oldMetric.dirtyIndices.length - data.dirtyIndices.length) + " less</strong> dirty entries have been found";
                }
                if (oldMetric.evalTuples.length < data.evalTuples.length) {
                  infoText += " by " + (data.evalTuples.length - oldMetric.evalTuples.length) + " new quality check(s).";
                } else if (oldMetric.evalTuples.length == data.evalTuples.length) {
                  infoText += " by editing the existing quality checks.";
                } else if (oldMetric.evalTuples.length > data.evalTuples.length) {
                  infoText += " by removing " + (oldMetric.evalTuples.length - data.evalTuples.length) + " quality check(s).";
                }
              } else if (data.dirtyIndices == null) {
                infoText += "Quality checks have been updated: <strong>" + oldMetric.dirtyIndices.length + " less</strong> dirty entries have been found";
              } else if(oldMetric.dirtyIndices == null) {
                infoText += "Quality checks have been updated: <strong>" + data.dirtyIndices.length + " new</strong> dirty entries have been found";
              }
              $("#alertText").html(infoText);
              $("#alertMetricUpdate").show(); //prop("hidden", false)
            }
            metricChange = "none";

            if(metricType[0] === "single") {
              overlayModel.metricColumns.filter(function(d) {
                if(d != null) {
                  return d.columnName === selectedColName[0];
                }
              })[0].metrics[data.name] = data;
            } else if (metricType[0] === "spanning") {
              overlayModel.spanningMetrics[updateMetricIndex] = data;
            }

            if(metricType[0] === "single") {
              var overviewTd = d3.select("#overviewTable tbody tr td.selected rect");
              overviewTd.attr("width", function(d) {
                if (d != null) {
                  d = data;
                  return d.measure * this.parentNode.scrollWidth;
                }
              });
            } else {
              d3.selectAll(".dataTables_scrollHead thead .span-metric-row td").data(overlayModel.spanningMetrics);
              d3.selectAll(".dataTables_scrollHead thead .span-metric-row td svg").data(overlayModel.spanningMetrics);
              d3.selectAll(".dataTables_scrollHead thead .span-metric-row td svg rect").data(overlayModel.spanningMetrics);
              d3.select(".dataTables_scrollHead thead .span-metric-row td.selected svg rect").attr("width", function(d) {
                if (d != null) {
                  d = data;
                  return d.measure * this.parentNode.scrollWidth;
                }
              });
            }

            if(metricData[0].spanningEvaluable == null) {
              var col = d3.selectAll("#overlay g.metrics-overlay").filter(function(d, i){
                if (d != null) {
                  return d.columnName == selectedColName[0];
                }
              });
              col.selectAll("g").remove();
              col.selectAll("line").remove();
              col.selectAll("rect").remove();
              var newGroups = col.selectAll("g")
                .data(function(d) {
                  if (d != null) {
                    var array = $.map(d.metrics, function(value, index) {
                      return [value];
                    });
                    return array;
                  } else {
                    return [];
                  }
                }).enter()
                .append("g")
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
                }).attr("display", function(d, i) {
                  if(d.dirtyIndices == null) return "none";
                });

              newGroups.append("rect")
                .attr("height", $(".dataTables_scrollBody").height())
                .attr("width", 12)
                .attr("fill", function(d) {
                  if (d.dirtyIndices != null) {
                    return "white";
                  } else {
                    return "transparent";
                  }
                });

              var bins = newGroups.selectAll("rect.metrics-bin")
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
                })
                .attr("height", 1)
                // .attr("y", function(d) { return overlayY(d.index); })
                .style("fill", function(d, i) {
                  var current = this.parentNode.__data__;
                  return fillMetricColor(current.name);
                });

              for (var i = 0; i < bins.length; i++) {
                if(bins[i].length > 0) {
                  bins.call(tooltipInvalid);
                  break;
                }
              }

              bins.each(function (d) {
                var y = overlayY(d.index);
                var ys = d3.select(this)
                  .attr("y", y);
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
                  tooltipInvalid.show(d)
                }
                $.each(sameRows, function(i, rowCurrent) {
                  $("#dataset").DataTable().row(rowCurrent.__data__.index).node().classList.add("hover");
                });
              });

              bins.on("mouseout", function(d) {
                $("#dataset tr").removeClass("hover");
                d3.select(this).style("fill", function(d, i) {
                  var current = this.parentNode.__data__;
                  return fillMetricColor(current.name)
                });
                tooltipInvalid.hide();
              });

              // var overlay = d3.select("#overlay").selectAll(".metrics-overlay");
              col.append("line")
                .attr("x1", function(d) {
                  var attr = 0;
                  for(var i = 0; i < this.parentNode.children.length; i++) {
                    var transf = this.parentNode.children[i].attributes["transform"];
                    if (transf != null) {
                      var transl = transf.value.match(/\d+/);
                      var offset = parseInt(transl[0]);
                      if(offset > attr) {
                        attr = offset;
                      }
                    }
                  }
                  return "-" + (attr+1);
                })
                .attr("x2", function(d) {
                  var attr = 0;
                  for(var i = 0; i < this.parentNode.children.length; i++) {
                    var transf = this.parentNode.children[i].attributes["transform"];
                    if (transf != null) {
                      var transl = transf.value.match(/\d+/);
                      var offset = parseInt(transl[0]);
                      if(offset > attr) {
                        attr = offset;
                      }
                    }
                  }
                  return "-" + (attr+1);
                })
                .attr("y1", 0)
                .attr("y2", $(".dataTables_scrollBody").height())
                .attr("stroke", "#ddd")
                .attr("stroke-width", "2")

              col.append("line")
                .attr("x1", 0)
                .attr("x2", 0)
                .attr("y1", 0)
                .attr("y2", $(".dataTables_scrollBody").height())
                .attr("stroke", "#ddd")
                .attr("stroke-width", "2")
            }
            selectedChecks = [];

            totalEvalTuples = [];
            var enabledTuples = metricData[0].evalTuples.filter(function(d) {
              return !d.disabled;
            });
            if(metricData[0].spanningEvaluable != null) {
              totalEvalTuples.push(metricData[0].spanningEvaluable);
              selectedChecks.push(metricData[0].name);
            }
            totalEvalTuples.push.apply(totalEvalTuples, enabledTuples);
            for(var j = 0; j < enabledTuples.length; j++) {
              selectedChecks.push(metricData[0].name);
            }

            // renderTableHeader();
            renderMetricOverview();
            renderSpanningMetricOverview();
            drawDatatableScrollVis();

            redrawDetailView(theProject, metricData, rowModel, overlayModel);
            updateOverlayPositions();
          });
	      });
}

function addMetricToColumn(data, index) {
  d3.event.preventDefault();
  var cellIndex = data.cellIndex;
  var popoverColumn = theProject.overlayModels.metricsOverlayModel.metricColumns.filter(function(col) {
      return col.columnName == data.name;
  })[0];
  if(columnForMetricToBeCreated == null || popoverColumn.columnName != columnForMetricToBeCreated) {
    columnForMetricToBeCreated = popoverColumn.columnName;
    metricToBeCreated = [];
  }
  $("#metricSelectMetricModal .btn").remove();
  var popoverSnippet = '';
  var avMetrics = theProject.overlayModels.metricsOverlayModel.availableMetrics;
  for(var i = 0; i < avMetrics.length; i++) {
    var cl = "btn btn-default";
    var activeMetric = popoverColumn.metrics.filter(function(metric) {
      return metric.name == avMetrics[i].name;
    });
    var checkedMetric = metricToBeCreated.filter(function(metric) {
      return metric == avMetrics[i].name;
    });
    var disabled = "",
        checked = "";
    if(activeMetric.length > 0) {
      cl += " disabled";
      disabled = " disabled";
    }
    if(checkedMetric.length > 0) {
      checked = " checked";
    }
    popoverSnippet += "<div" + disabled + " class='checkbox'><label for='" + cellIndex + "'" +
      "><input id='"+cellIndex+"' class='overview-popover'" + disabled + checked + 
      " type='checkbox'>" + avMetrics[i].name + "</label></div>";

    $("#metricSelectMetricModal").append('<button type="button" value="' + avMetrics[i].name + '" class="' + cl + '">'+ capitalizeFirstLetter(avMetrics[i].name) + '</button>');
    $("#metricSelectMetricModal > button").on("click", function() {
      $("#metricSelectMetricModal").val($(this).text());
    });
  }
  popoverSnippet += "<button type='button' class='btn' id='addMetricBtn'>Add Metrics</button>";
  var bsPopover = $(this).data("bs.popover");
  bsPopover.options.content = popoverSnippet;
  $(document).on("click", "#addMetricBtn", function(d) {
    var colSelected = $("#columnFormMetricModal option").filter(function(option) {
      return this.value == columnForMetricToBeCreated;
    }).attr("selected", true);
    var metricsSelected = $("#metricSelectMetricModal").children().filter(function(button) {
      return metricToBeCreated.indexOf(this.value) > -1;
    });
    metricsSelected.addClass("active");
    $( "#addMetricModal" ).modal("show");
  });

  var _this = this;
  $(this).popover("toggle");
  $(".popover").on("mouseleave", function () {
      $(_this).popover('hide');
  });
}

function wrap(text, widths) {
  text.each(function() {
    var text = d3.select(this),
        words = text.text().split(/\s+/).reverse(),
        word,
        line = [],
        lineNumber = 0,
        lineHeight = 1.1, // ems
        y = text.attr("y"),
        dy = parseFloat(text.attr("dy")),
        tspan = text.text(null).append("tspan").attr("x", 0).attr("y", y).attr("dy", dy + "em"),
        width;
    if(widths.length == 1) width = widths[0];
    else width = widths[this.__data__];

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

function detaildragresize(d) {
  //Max x on the left is x - width 
  //Max x on the right is width of screen + (dragbarw/2)
  // var dragx = Math.max(d.x + (dragbarw/2), Math.min(detailWidth, d.x + dragbarw + d3.event.dx));
  // console.log(d.x+10 + ", max of(" + detailWidth + ", " + (d.x + 20 + d3.event.dx));
  //recalculate width
  var selectedIdx = this.__data__;
  detailWidth = detailWidths[selectedIdx] + d3.event.dx;
  console.log(detailWidth);

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

  detailViewWidth = 0;
    for (var i = 0; i < detailWidths.length; i++) {
      detailViewWidth += detailWidths[i];
    }
  d3.selectAll("rect.posHighlight").attr("width", detailViewWidth);

  var svg = d3.select("#heatmap").select("svg")
    .attr("width", detailViewWidth + detailViewMargin.left + detailViewMargin.right)

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

  d3.select("g.x.axis").call(xAxis);
  var heatAxis = d3.select("g.x.axis");
  heatAxis
    .selectAll(".tick text")
    .style("font-size", 12)
    .call(wrap, detailWidths)
    .style("text-anchor", "start")
    .attr("x", 6)
    .attr("y", 6);

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
  var lines = d3.selectAll("line.separator")
    .attr("x1", function(d, i) {
      return separatorPos[i];
    })
    .attr("x2", function(d, i) {
      return separatorPos[i];
    });
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

function selectRow(d) {
  $("#dataset td").removeClass("highlight");
  var selThis = this;
  var thisSelected = this.__data__.index;

  var bodyHeight = $("#dataset tbody").height();

  var regex = /(\d+)/g;
  var nums = $(".dataTables_info").text().replace(/,/g, "").match(regex);
  var from = parseInt(nums[0]) - 1;
  var to = parseInt(nums[1]) - 1;

  if(from > thisSelected || to < thisSelected) {
    var page = Math.floor(thisSelected / pageLength);
    $('#dataset').DataTable().page(page).draw('page');
  }

  $('div.dataTables_scrollBody').animate({
    scrollTop: $("#dataset").DataTable().row((thisSelected)).node().offsetTop
  }, 500);

  $.each($("#dataset").DataTable().row((thisSelected)).node().children, function(i, td) {
    td.classList.add("highlight");
  });
  // var bodyHeight = $("#dataset tbody").height();
  // var regex = /(\d+)/g;
  // var nums = $(".dataTables_info").text().replace(/,/g, "").match(regex);
  // var from = parseInt(nums[0]) - 1;
  // var to = parseInt(nums[1]) - 1;

  // var offsetSelected = thisSelected - from;
  // var offsetTo = to - from;

  // var ratio = offsetSelected / offsetTo;

  // $('div.dataTables_scrollBody').animate({
  //   scrollTop: bodyHeight * ratio
  // }, 500);

  // var selectedRows = d3.select(this.parentNode).selectAll("rect").filter(function(r) {
  //   return d3.select(this).attr("y") === d3.select(selThis).attr("y");
  // })[0];
  // $("#dataset").DataTable().row(selectedRows[0].__data__.index).scrollTo();
  // $.each(selectedRows, function(i, rowCurrent) {
  //   $.each($("#dataset").DataTable().row(rowCurrent.__data__.index).node().children, function(i, td) {
  //     td.classList.add("highlight");
  //   });
  // });
}

function scheduleUpdate(textArea) {
    if (timerID !== null) {
        window.clearTimeout(timerID);
    }
    var self = this;
    timerID = window.setTimeout(function() { 
      var self = this;
      var area = $(textArea).children("input")[0].value;
      var expression = this.expression = $.trim($(textArea).children("input")[0].value);
      var index = -1;
      for(var i = 0, len = overlayModel.metricColumns.length; i < len; i++) {
        if (columns[i].name === metricData[0].columnName) {
          index = columns[i].cellIndex;
          break;
        }
      }
      var params = {
          project: theProject.id,
          expression: "grel:" + expression,
          cellIndex: index
      };
      
      $.post(
          "../../command/core/preview-expression?" + $.param(params), 
          {
              rowIndices: JSON.stringify(rowIndex) 
          },
          function(data) {
              if (data.code == "error") {
                $(textArea.parentNode).addClass("has-error");
                $("#alertMetricUpdate").removeClass("alert-info");
                $("#alertMetricUpdate").addClass("alert-danger");
                $("#alertText").html(data.message);
                $("#alertMetricUpdate").show(); //prop("hidden", false)
              } else if (data.code == "ok" && data.results[0].message == null) {
                $(textArea.parentNode).removeClass("has-error");
                $("#alertMetricUpdate").hide();
              } else if (data.results[0].message != null) {
                $(textArea.parentNode).addClass("has-error");
                $("#alertMetricUpdate").removeClass("alert-info");
                $("#alertMetricUpdate").addClass("alert-danger");
                $("#alertText").html(data.results[0].message);
                $("#alertMetricUpdate").show(); //prop("hidden", false)
              } else {
                $(textArea.parentNode).removeClass("has-error");
                $("#alertMetricUpdate").hide();
              }
          });
    }, 300);
};