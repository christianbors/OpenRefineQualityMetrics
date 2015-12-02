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
      "scripts/project-injection.js",
      "scripts/facets/metrics-facet.js"
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

  var OR = Packages.com.google.refine.operations.OperationRegistry;
  OR.registerOperation(module, "metricsExtension", Packages.com.google.refine.metricsExtension.operations.MetricsExtensionOperation);

  var FCR = Packages.com.google.refine.grel.ControlFunctionRegistry;
  FCR.registerFunction("completeness", new Packages.com.google.refine.metricsExtension.expr.Completeness());

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
