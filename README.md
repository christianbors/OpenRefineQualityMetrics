# Open Refine Quality Metrics Extension

_MetricDoc_ is an interactive visual exploration environment for assessing data quality.

During data pre-processing, analysts spend a significant part of their time and effort profiling the quality of the data along with cleansing and transforming the data for further analysis. 
While quality metrics -- ranging from general to domain-specific measures -- support assessment of the quality of a dataset, there are hardly any approaches to visually support the analyst in customizing and applying such metrics.
Yet, visual approaches could facilitate users' involvement in data quality assessment.
We present _MetricDoc_, an interactive environment for assessing data quality that provides customizable, reusable quality metrics in combination with immediate visual feedback.
Moreover, we provide an overview visualization of these quality metrics
along with error visualizations that facilitate interactive navigation of the data to determine the causes of quality issues present in the data.
In this paper we describe the architecture, design, and evaluation of _MetricDoc_ which underwent several design cycles, including heuristic evaluation and expert reviews as well as a focus group with data quality, human-computer interaction, and visual analytics experts.

## Architecture
![MetricDoc Architecture](https://github.com/christianbors/OpenRefineQualityMetrics/blob/master/suppl/architecture.png)

A schematic overview of the environment's architecture illustrates the interconnection of different data models, representations as well as interactions.
The data structures in MetricDoc extend OpenRefine's column data representation, adding data quality information in form of quality metrics.
Similarily, the data structures of other data profiling or wrangling tools can be extended, irregardless of the storage-approach employed (e.g., column-wise, row-wise, tuple-wise).
Additionally, server-side data quality operations (calculations, setup procedures, etc.) ensure proper project persistence and data management.

## Connection to OpenRefine

We provide customized Metrics in a separate configuration panel that features a small sample data table the metrics can be computed and tested on, utilizing bootstrap, d3, ... as additional libraries to facilitate development, based upon the OpenRefine Server architecture.

## User Interface

![Custom Metrics Extension](https://github.com/christianbors/OpenRefineQualityMetrics/blob/master/suppl/metricdoc_teaser.png)

The environment consists of the 
(a) _quality metrics overview_ 
(b) the metric information view and 
(c) customization tabs 
(d) the _metric detail view_ and
(e) the tabular _raw data view_ enhanced with _error distribution_ heatmaps 
(f) mouseover tooltips provide detail information on 
(g) metrics and (h) data errors,
(j) metric distribution heatmaps can be enabled and disabled individually.

## Setup

Please follow OpenRefine extension install guide for adding MetricDoc to your OpenRefine environment.

Then install bower and run `bower install`
After running the server and adding Metrics to the dataset (dropdown at columns - Add Metrics), the extension is available at 127.0.0.1:3333/extension/metric-doc/?project=*_projectId_*

Libraries utilized

* DataTables [1.10.7](https://www.datatables.net/download/#DataTables) ([with Bootstrap Theme](https://github.com/DataTables/Plugins/tree/master/integration/bootstrap/3))
* Bootstrap [3.3.5](http://getbootstrap.com/customize/)
* jQuery [2.1.4](https://jquery.com/download/)
* jQuery Sortable [0.9.13](http://johnny.github.com/jquery-sortable/)
