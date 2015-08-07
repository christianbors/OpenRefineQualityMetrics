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
  RS.registerCommand(module, "completeness", new Packages.com.google.refine.metricsExtension.commands.ColumnMetricEvaluation(
    new Packages.com.google.refine.metricsExtension.model.metrics.column.Completeness()));
  RS.registerCommand(module, "compute-custom-facets", new Packages.com.google.refine.metricsExtension.commands.browsing.CustomComputeFacetsCommand());

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

    send(request, response, "index.vt", context);
  }
}

function send(request, response, template, context) {
  butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
