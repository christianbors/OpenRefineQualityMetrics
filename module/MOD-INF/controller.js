var html = "text/html";
var encoding = "UTF-8";
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

/*
 * Function invoked to initialize the extension.
 */
function init() {
  // Packages.java.lang.System.err.println("Initializing quality metrics extension");
  // Packages.java.lang.System.err.println(module.getMountPoint());

  // Script files to inject into /project page
  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/dialogs/calculateMetricsDialog.js",
      "scripts/dialogs/persistMetrics.js",
      "scripts/project-injection.js",
      "scripts/facets/metrics-facet.js",
      "scripts/menus.js"
    ]
  );

  // Style files to inject into /project page
  ClientSideResourceManager.addPaths(
    "project/styles",
    module,
    [
      "styles/project-injection.less"
    ]
  );

  var RS = Packages.com.google.refine.RefineServlet;
  RS.registerClassMapping(
    "com.google.refine.operations.MetricsExtensionOperation$MetricsProjectChange",
    "com.google.refine.metricsExtension.operations.MetricsExtensionOperation$MetricsProjectChange");
  RS.cacheClass(Packages.com.google.refine.metricsExtension.operations.MetricsExtensionOperation$MetricsProjectChange);

  RS.registerCommand(module, "metricsOverlayModel", new Packages.com.google.refine.metricsExtension.commands.MetricsExtensionCommand);
  RS.registerCommand(module, "evaluateMetrics", new Packages.com.google.refine.metricsExtension.commands.EvaluateMetricsCommand);
  RS.registerCommand(module, "evaluateSelectedMetric", new Packages.com.google.refine.metricsExtension.commands.EvaluateSelectedMetricCommand);
  RS.registerCommand(module, "evaluateDataTypes", new Packages.com.google.refine.metricsExtension.commands.EvaluateDataTypesCommand);
  RS.registerCommand(module, "persistMetrics", new Packages.com.google.refine.metricsExtension.commands.PersistMetricsCommand);
  RS.registerCommand(module, "getMetricsOverlayModel", new Packages.com.google.refine.metricsExtension.commands.GetMetricsOverlayModelCommand);
  RS.registerCommand(module, "createMetric", new Packages.com.google.refine.metricsExtension.commands.CreateMetricCommand);
  RS.registerCommand(module, "updateMetric", new Packages.com.google.refine.metricsExtension.commands.UpdateMetricCommand);
  RS.registerCommand(module, "mergeMetric", new Packages.com.google.refine.metricsExtension.commands.MergeMetricCommand);
  RS.registerCommand(module, "duplicateMetric", new Packages.com.google.refine.metricsExtension.commands.DuplicateMetricCommand);
  RS.registerCommand(module, "deleteMetric", new Packages.com.google.refine.metricsExtension.commands.DeleteMetricCommand);

  var OR = Packages.com.google.refine.operations.OperationRegistry;
  OR.registerOperation(module, "metricsExtension", Packages.com.google.refine.metricsExtension.operations.MetricsExtensionOperation);
  OR.registerOperation(module, "persistMetrics", Packages.com.google.refine.metricsExtension.operations.PersistMetricsOperation);

  var FCR = Packages.com.google.refine.grel.ControlFunctionRegistry;
  FCR.registerFunction("uniqueness", new Packages.com.google.refine.metricsExtension.expr.Uniqueness());
  FCR.registerFunction("completeness", new Packages.com.google.refine.metricsExtension.expr.Completeness());
  FCR.registerFunction("validity", new Packages.com.google.refine.metricsExtension.expr.Validity());
  FCR.registerFunction("dateInterval", new Packages.com.google.refine.metricsExtension.expr.DateInterval());
  FCR.registerFunction("plausibility", new Packages.com.google.refine.metricsExtension.expr.Plausibility());

  Packages.com.google.refine.model.Project.
    registerOverlayModel("metricsOverlayModel", Packages.com.google.refine.metricsExtension.model.MetricsOverlayModel);
}

/*
 * Function invoked to handle each request in a custom way.
 */
function process(path, request, response) {
  // Analyze path and handle this request yourself.

  if (path == "/" || path == "") {
    var context = {};
    // here's how to pass things into the .vt templates
    context.someList = ["Superior","Michigan","Huron","Erie","Ontario"];
    context.someString = "foo";
    context.someInt = 3;

    send(request, response, "index.html", context);
  }
}

function send(request, response, template, context) {
  butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
