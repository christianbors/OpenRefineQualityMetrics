function CalculateMetrics() {
}

var columnsReformat = [];
var inputFormatArray = [];
CalculateMetrics.prototype = {
    init: function () {
        var self = this;
        this.dialogElement = $(DOM.loadHTML("metric-doc", "scripts/dialogs/calculateMetrics.html"));
        var controls = DOM.bind(this.dialogElement);
        controls.close.click(this.extraBound("hide"));
        Refine.postProcess('metric-doc', 'evaluateMetrics', {}, {}, {}, {});
        // controls.reformatDates.click(function () {
        //     self.reformat();
        // });

        // $(".column-header").each(function () {
        //     if(!($(this).attr("title")==undefined)) {
        //         columnsReformat.push($(this).attr("title"));
        //     }
        // });
    },
    show: function () {
        this.init();
        this.dialogLevel = DialogSystem.showDialog(this.dialogElement);
        // columnsReformat.forEach(function (entry) {
        //     $("#reformatDateSelect").append("<option value='" + entry + "'>" + entry + "</option>");
        // });
        // $('#addInputDateFormat').click(function(){
        //     var inputDateFormat = $('#inputDateFormat').val();
        //     inputFormatArray.push(inputDateFormat);
        //     $('#inputDateFormatList').append('<li>' + inputDateFormat + '</li>');
        // });

    },

    hide: function () {
        DialogSystem.dismissUntil(this.dialogLevel - 1);
    },

    reformat: function () {
        var data = {  };

        // data["inputFormats"] = inputFormatArray;
        // data["outputFormat"] = $('#outputDateFormat').val();
        // data["column"] = $("#reformatDateSelect option:selected" ).text();
        // Refine.postProcess('timebench-extension', 'apply-reformation', data, {},
        //     { rowsChanged: true, modelsChanged: true });
        // this.hide();
        // columnsReformat = [];
        // inputFormatArray = [];
    }
};
