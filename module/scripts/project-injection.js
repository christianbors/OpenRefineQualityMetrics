/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

// This file is added to the /project page

var QualityMetricsExtension = {};

DataTableColumnHeaderUI.extendMenu(function(column, columnHeaderUI, menu) {
  var doStatsDialog = function(response) {
    var dialog = $(DOM.loadHTML("quality-metrics", "scripts/completeness.html"));

    var elmts = DOM.bind(dialog);
    elmts.dialogHeader.text("Metrics for column \"" + column.name + "\"");

    if (response["measure"]) { elmts.dialogCompleteness.text(response["measure"]) };

    var level = DialogSystem.showDialog(dialog);

    elmts.okButton.click(function() {
      DialogSystem.dismissUntil(level - 1);
    });
  };

  var prepMetricsDialog = function() {
    params = { "column_name": column.name };
    body = {};
    updateOptions = {};
    callbacks = {
      "onDone": function(response) {
        doStatsDialog(response);
      }
    }

    Refine.postProcess(
      "quality-metrics",
      "completeness",
      params,
      body,
      updateOptions,
      callbacks
      );
  }

  MenuSystem.insertAfter(
    menu,
    [ "core/transpose" ],
    [
    {},
    {
      id: "quality-metrics/completeness",
      label: "Completeness",
      click: prepMetricsDialog
    }
    ]
    );
    MenuSystem.appendTo(
      menu, 
      [ "core/facet" ], 
      [
      {},
      {
        id: "quality-metrics/metrics-facet",
        label: "Metrics Facet",
        click: function() {
          var elmt = ui.browsingEngine._createFacetContainer();
          var config = {
                "name": column.name,
                "columnName": column.name,
                "expression": "value",
                "detailView": true,
                "constraints": "empty",
                "metrics": ['completeness']
              };
          var options = {};
          facet = new MetricsFacet(elmt, config, options);

          ui.browsingEngine._facets.push({ elmt: elmt, facet: facet });

          ui.leftPanelTabs.tabs({ active: 0 });

          Refine.update({ engineChanged: true });
        }
      }
      ]);
});

