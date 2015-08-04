# Open Refine Quality Metrics Extension

## Basic Metrics

Basic metrics constitute the starting point for overview analysis of a specific data column.
For that we automatically detect the data type and calculate the respective default metric.
It can hence be further refined by appending features and checks (e.g.: quality checks, statistical analyses, etc.).

## Accumulating Metrics

What about the choice of only wanting one metric for quality control.
We provide the possibility of aggregating multiple metrics to provide one comprehensive measure that could act as an indicator for certain error cases, known data issues, ...
With this functionality arises the necessity to variably weigh the different metrics.

### Metric Granularity

Granularity level of metrics is an issue that needs to be considered. Specifically for metrics

## Connection to OpenRefine

We will provide customized Metrics in a separate configuration panel that features a small sample data table the metrics can be computed and tested on, utilizing bootstrap, d3, ... as additional libraries to facilitate development, based upon the OpenRefine Server architecture.

## User Interface

![Image of Yaktocat](https://octodex.github.com/images/yaktocat.png)

Current status
* Not yet functional UI
* basic data loading works
* OpenRefine classes can be integrated via loading js files considering the relative source path: Path to `core` directory is accomplished by `../../`. Subsequently a file located in `scripts` can be included with `../../scripts/xyz.js`.

## Uncertainty

**TODO:** Determine which uncertainty mechanisms will be incorporated
