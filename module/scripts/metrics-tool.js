$(document).ready(function() {
  //$('#demo').html( '<table cellpadding="0" cellspacing="0" border="0" class="display" id="example"></table>' );

  var dataSet = [];
  var columns = [];

  var params = {
    project: 2421247403318
  };

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

  $.getJSON("../../command/core/get-columns-info?" + $.param(params),function(data) {
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
  });

  $.post(
    "../../command/core/get-rows?" + $.param({ project: 2421247403318, start: 0, limit: 100 }) + "&callback=?",
    [],
    function(data) {
      var rowModel = data;

      // Un-pool objects
      for (var r = 0; r < data.rows.length; r++) {
        var row = data.rows[r];
        var rowValues = [];
        for (var c = 0; c < row.cells.length; c++) {
          var cell = row.cells[c];
          rowValues.push(cell.v);
        }
        dataSet.push(rowValues);
      }

      var dataCols = dataSet[0];

      if(columns) {
        if(dataCols.length == columns.length) {
          $('#dataset').dataTable( {
            "data": dataSet,
            "columns": columns
          } );
        }
      }
    },
    "jsonp"
  );

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

  $.post("../../command/custom-quality-metrics/completeness", params, function(response) {
    var dialog = $(DOM.loadHTML("custom-quality-metrics", "../../scripts/completeness.html"));

    var elmts = DOM.bind(dialog);
    elmts.dialogHeader.text("Metrics for column \"" + params.column_name + "\"");

    if (response["measure"]) { elmts.dialogCompleteness.text(response["measure"]) };

    // var level = DialogSystem.showDialog(dialog);
    // elmts.okButton.click(function() {
    //   DialogSystem.dismissUntil(level - 1);
    // });
  });
}