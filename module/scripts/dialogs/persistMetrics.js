function PersistMetrics() {
}

PersistMetrics.prototype = {
    init: function () {
        var self = this;
        Refine.postProcess('metric-doc', 'persistMetrics', {}, {}, {}, {});
    },
    show: function () {
    	this.init();
    },
    hide: function () {}
};