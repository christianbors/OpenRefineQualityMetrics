ExtensionBar.addExtensionMenu({
	id : "metric-doc",
	label : "Metric Doc",
    "submenu" : [
        {
            "id" : "metric-doc/CalculateMetrics",
            label: "Calculate Metrics",
            click: dialogHandler(CalculateMetrics)
        }

    ]
});


function dialogHandler(dialogConstructor) {
    var dialogArguments = Array.prototype.slice.call(arguments, 1);
    function Dialog() {
        return dialogConstructor.apply(this, dialogArguments);
    }
    Dialog.prototype = dialogConstructor.prototype;
    return function() {
        new Dialog().show();
    };
}

function forwardMetricDoc(dialogConstructor){
    var dialogArguments = Array.prototype.slice.call(arguments, 1);
    function Dialog() {
        return dialogConstructor.apply(this, dialogArguments);
    }
    Dialog.prototype = dialogConstructor.prototype;
    return function() {
        window.location = '/extension/metric-doc/index.html?project=' + theProject.id;
    };
}
