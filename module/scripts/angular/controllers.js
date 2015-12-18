angular.module('metricDocExtension', [])
	// .directive('myRepeatDirective', function() {
	// 	return function(scope, element) {
	// 		markValues(scope, element);
	// 	}
	// });

.controller('metricDocExtensionCtrl', function ($scope, $http) {
	var nasdaqTable = dc.dataTable('.test-dc-table');

	var movies = [
        { title: "The Godfather", year: 1972, length: 175, budget: 6000000, rating: 9.1 },
        { title: "The Shawshank Redemption", year: 1994, length: 142, budget: 25000000, rating: 9.1 },
        { title: "The Lord of the Rings: The Return of the King", year: 2003, length: 251, budget: 94000000, rating: 9 },
        { title: "The Godfather: Part II", year: 1974, length: 200, budget: 13000000, rating: 8.9 },
        { title: "Shichinin no samurai", year: 1954, length: 206, budget: 500000, rating: 8.9 },
        { title: "Buono, il brutto, il cattivo, Il", year: 1966, length: 180, budget: 1200000, rating: 8.8 },
        { title: "Casablanca", year: 1942, length: 102, budget: 950000, rating: 8.8 },
        { title: "The Lord of the Rings: The Fellowship of the Ring", year: 2001, length: 208, budget: 93000000, rating: 8.8 },
        { title: "The Lord of the Rings: The Two Towers", year: 2002, length: 223, budget: 94000000, rating: 8.8 },
        { title: "Pulp Fiction", year: 1994, length: 168, budget: 8000000, rating: 8.8 }
    ];

    var ndx = crossfilter(movies);
    var all = ndx.groupAll();

    var titleDim = ndx.dimension(function(d) {
    	return d.title;
    });
    	yearDim = ndx.dimension(function(d) {
    		return d.year;
	});

	nasdaqTable
		.dimension(titleDim)
		.group(function (d) {
			return d.title;
		})
		.columns([]);

                // Un-pool objects

    $('#table').dataTable({
    	"data": movies
    });
/*    for (var r = 0; r < movies[0].length; r++) {
    	var col = movies[r];
        var rowValues = [];
      	for (var c = 0; c < movies.length; c++) {
            var cell = row[c];
            if (cell != null) {
                rowValues.push(cell.v);
            } else {
                rowValues.push('');
        	}
        }
        dataSet.push(rowValues);
        $('#dataset').dataTable( {
        	"data": rowValues;
        });
    }

                var dataCols = dataSet[0];

                if(columnStore) {
                  if(dataCols.length == columnStore.length) {
                    $('#dataset').dataTable( {
                      "data": dataSet,
                      "columns": columnStore
                    } );
                  }
                }*/

    dc.renderAll();
});