(function(window, document, $, undefined) {
        "use strict";
        $(function() {
            console.log("here chart");
            if ($('#chartjs_pie_ram').length) {
                console.log("ram")
                var ctx = document.getElementById("chartjs_pie_ram").getContext('2d');
                var myChart = new Chart(ctx, {
                    type: 'pie',
                    data: {
                        labels: ["M", "T", "W", "T", "F", "S", "S"],
                        datasets: [{
                            backgroundColor: [
                               "#5969ff",
                                "#ff407b",
                                "#25d5f2",
                                "#ffc750",
                                "#2ec551",
                                "#7040fa",
                                "#ff004e"
                            ],
                            data: [10, 20, 5, 17, 28, 24, 7]
                        }]
                    },
                    options: {
                           legend: {
                        display: true,
                        position: 'right',
                        align:'center',
                        
                        labels: {
                            fontColor: '#71748d',
                            fontFamily: 'Circular Std Book',
                            fontSize: 14,
                        }
                    },

                    
                }
                });
            }

        });

})(window, document, window.jQuery);