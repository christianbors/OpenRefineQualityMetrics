DataTableCellUI.prototype._render = function() {
  var self = this;
  var cell = this._cell;

  var divContent = $('<div/>')
  .addClass("data-table-cell-content");

  var editLink = $('<a href="javascript:{}">&nbsp;</a>')
  .addClass("data-table-cell-edit")
  .attr("title", $.i18n._('core-views')["edit-cell"])
  .appendTo(divContent)
  .click(function() { self._startEdit(this); });

  $(this._td).empty()
  .unbind()
  .mouseenter(function() { editLink.css("visibility", "visible"); })
  .mouseleave(function() { editLink.css("visibility", "hidden"); });

  // $.ajax({
  //   async: false,
  //   type: "POST",
  //   url: "command/core/set-preference?" + $.param({ 
  //     name: "reconciliation.standardServices" 
  //   }),
  //   data: { "value" : JSON.stringify(ReconciliationManager.standardServices) },
  //   success: function(data) {
  //     if (f) { f(); }
  //   },
  //   dataType: "json"
  // });

  if (!cell || ("v" in cell && cell.v === null)) {
    $('<span>').html("&nbsp;").appendTo(divContent);
  } else if ("e" in cell) {
    $('<span>').addClass("data-table-error").text(cell.e).appendTo(divContent);
  } else if (!("r" in cell) || !cell.r) {
    if (typeof cell.v !== "string" || "t" in cell) {
      if (typeof cell.v == "number") {
        divContent.addClass("data-table-cell-content-numeric");
      }
      $('<span>')
      .addClass("data-table-value-nonstring")
      .text(cell.v)
      .appendTo(divContent);
    } else if (URL.looksLikeUrl(cell.v)) {
      $('<a>')
      .text(cell.v)
      .attr("href", cell.v)
      .attr("target", "_blank")
      .appendTo(divContent);
    } else {
      $('<span>')
      .text(cell.v)
      .appendTo(divContent);
    }
  } else {
    var r = cell.r;
    var service = (r.service) ? ReconciliationManager.getServiceFromUrl(r.service) : null;

    if (r.j == "new") {
      $('<span>').text(cell.v).appendTo(divContent);
      $('<span>').addClass("data-table-recon-new").text("new").appendTo(divContent);

      $('<a href="javascript:{}"></a>')
      .text($.i18n._('core-views')["choose-match"])
      .addClass("data-table-recon-action")
      .appendTo(divContent).click(function(evt) {
        self._doRematch();
      });
    } else if (r.j == "matched" && "m" in r && r.m !== null) {
      var match = cell.r.m;
      var a = $('<a></a>')
      .text(match.name)
      .attr("target", "_blank")
      .appendTo(divContent);

      if (service && (service.view) && (service.view.url)) {
        a.attr("href", encodeURI(service.view.url.replace("{{id}}", match.id)));
      } else if (ReconciliationManager.isFreebaseIdOrMid(r.identifierSpace)) {
        a.attr("href", "http://www.freebase.com/view" + match.id);
      }

      $('<span> </span>').appendTo(divContent);
      $('<a href="javascript:{}"></a>')
      .text($.i18n._('core-views')["choose-match"])
      .addClass("data-table-recon-action")
      .appendTo(divContent)
      .click(function(evt) {
        self._doRematch();
      });
    } else {
      $('<span>').text(cell.v).appendTo(divContent);

      if (this._dataTableView._showRecon) {
        var ul = $('<div></div>').addClass("data-table-recon-candidates").appendTo(divContent);
        if ("c" in r && r.c.length > 0) {
          var candidates = r.c;
          var renderCandidate = function(candidate, index) {
            var li = $('<div></div>').addClass("data-table-recon-candidate").appendTo(ul);

            $('<a href="javascript:{}">&nbsp;</a>')
            .addClass("data-table-recon-match-similar")
            .attr("title", $.i18n._('core-views')["match-all-cells"])
            .appendTo(li).click(function(evt) {
              self._doMatchTopicToSimilarCells(candidate);
            });

            $('<a href="javascript:{}">&nbsp;</a>')
            .addClass("data-table-recon-match")
            .attr("title", $.i18n._('core-views')["match-this-cell"] )
            .appendTo(li).click(function(evt) {
              self._doMatchTopicToOneCell(candidate);
            });

            var a = $('<a></a>')
            .addClass("data-table-recon-topic")
            .attr("target", "_blank")
            .text(_.unescape(candidate.name))
            .appendTo(li);

            if ((service) && (service.view) && (service.view.url)) {
              a.attr("href", encodeURI(service.view.url.replace("{{id}}", candidate.id)));
            } else if (ReconciliationManager.isFreebaseIdOrMid(r.identifierSpace)) {
              a.attr("href", "http://www.freebase.com/view" + candidate.id);
            }

            var preview = null;
            if ((service) && (service.preview) 
                && service.preview.url.indexOf("http://www.freebase.com/widget/topic") < 0) {
              preview = service.preview;
            } else if (ReconciliationManager.isFreebaseIdOrMid(r.identifierSpace)) {
              preview = DataTableCellUI.internalPreview;
            }
            if (preview) {
              a.click(function(evt) {
                if (!evt.metaKey && !evt.ctrlKey) {
                  self._previewCandidateTopic(candidate, this, preview);
                  evt.preventDefault();
                  return false;
                }
              });
            }

            var score;
            if (candidate.score < 1) {
              score = Math.round(candidate.score * 1000) / 1000;
            } else {
              score = Math.round(candidate.score);
            }
            $('<span></span>').addClass("data-table-recon-score").text("(" + score + ")").appendTo(li);
          };

          for (var i = 0; i < candidates.length; i++) {
            renderCandidate(candidates[i], i);
          }
        }

        var liNew = $('<div></div>').addClass("data-table-recon-candidate").appendTo(ul);
        $('<a href="javascript:{}">&nbsp;</a>')
        .addClass("data-table-recon-match-similar")
        .attr("title", $.i18n._('core-views')["create-topic-cells"])
        .appendTo(liNew).click(function(evt) {
          self._doMatchNewTopicToSimilarCells();
        });

        $('<a href="javascript:{}">&nbsp;</a>')
        .addClass("data-table-recon-match")
        .attr("title", $.i18n._('core-views')["create-topic-cell"])
        .appendTo(liNew).click(function(evt) {
          self._doMatchNewTopicToOneCell();
        });

        $('<span>').text($.i18n._('core-views')["create-topic"]).appendTo(liNew);

        var suggestOptions;
        var addSuggest = false;
        if ((service) && (service.suggest) && (service.suggest.entity)) {
          suggestOptions = service.suggest.entity;
          addSuggest = true;
        } else if (ReconciliationManager.isFreebaseIdOrMid(r.identifierSpace)) {
          addSuggest = true;
        }

        var extraChoices = $('<div>').addClass("data-table-recon-extra").appendTo(divContent);
        if (addSuggest) {
          $('<a href="javascript:{}"></a>')
          .click(function(evt) {
            self._searchForMatch(suggestOptions);
            return false;
          })
          .text($.i18n._('core-views')["search-match"])
          .appendTo(extraChoices);
        }
      }
    }
  }

  divContent.appendTo(this._td);
};