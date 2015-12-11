$(document).ready(function() {
  //$('#demo').html( '<table cellpadding="0" cellspacing="0" border="0" class="display" id="example"></table>' );

  var dataSet = [];
  var columnStore = [];

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
                      "columns": columnStore
                    } );
                  }
                }
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
  Sortable.create(completenessList, {});

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