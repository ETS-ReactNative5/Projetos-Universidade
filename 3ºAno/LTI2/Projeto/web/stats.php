<!DOCTYPE html>
<html>
<body>
	<!-- MAIN CONTENT -->
 	<div class="main-content" id="stats-content">
				<div class="container-fluid">			
					<div class="row">
						<div class="col-md-6">
							<!-- RECENT PURCHASES -->
							<div class="panel panel-scrolling">
								<div class="panel-heading"><h3 class="panel-title"> Pacientes por serviço</h3>
								</div>
								<div id="panel-patient" style="margin-bottom: 5%;">
                                        <canvas class="centerChart" id="myChart" width="400" height="400">
                                    </canvas>
								</div>
							</div>
							<!-- END RECENT PURCHASES -->	
						</div>
                        <!-- DIREITA -->
						<div class="col-md-6">
							<div class="panel panel-scrolling">
								<div class="panel-heading"><h3 class="panel-title"> Registos comportamentais</h3>
								</div>
								<div id="panel-patient" style="margin-bottom: 5%;">
                                        <canvas class="centerChart"  id="myHorizontalChart" width="400" height="400">
                                    </canvas>
								</div>
							</div>
						</div>
                        <!-- END-->
					</div>
				</div>
			</div>
    
    <script src="https://code.jquery.com/jquery-3.4.1.min.js"></script>
	<script src="assets/vendor/bootstrap/js/bootstrap.min.js"></script>
	<script src="assets/vendor/jquery-slimscroll/jquery.slimscroll.min.js"></script>
	<script src="assets/vendor/jquery.easy-pie-chart/jquery.easypiechart.min.js"></script>
	<script src="assets/scripts/klorofil-common.js"></script>
    <script src="assets/vendor/chartjs/chart.js"></script>

<script>

        $(document).ready( function() {
                   
                    var ctx = document.getElementById("myChart");
                    var myChart = new Chart(ctx, {
                    type: 'pie',
                    data: {
                        labels: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN'],
                        datasets: [{
                        label: '# of Tomatoes',
                        data: [12, 19, 3, 5],
                        backgroundColor: [
                            'rgba(255, 99, 132, 0.5)',
                            'rgba(54, 162, 235, 0.2)',
                            'rgba(255, 206, 86, 0.2)',
                            'rgba(75, 192, 192, 0.2)'
                        ],
                        borderColor: [
                            'rgba(255,99,132,1)',
                            'rgba(54, 162, 235, 1)',
                            'rgba(255, 206, 86, 1)',
                            'rgba(75, 192, 192, 1)'
                        ],
                        borderWidth: 2
                        }]
                    },
                    options: {
                        cutoutPercentage: 40,
                        responsive: false,

                    }
                    });

                    function addData(chart, label, data) {
                        chart.data.labels.push(label);
                        chart.data.datasets.forEach((dataset) => {
                            dataset.data.push(data);
                        });
                        chart.update();
                    }

                    
                });
     
</script>

</body>
</html>