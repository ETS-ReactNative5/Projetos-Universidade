<?php
	session_start();

	if (isset($_SESSION['user'])){
		$conn = mysqli_connect("server-IP","username","password");
		mysqli_select_db($conn , "DB");

		$ids = mysqli_query($conn,"select ise,isu from user where username ='".$_SESSION['user']."'");
			if($ids!=null){
				$myArray = array();
				$tempArray = array();
				
				while($row = $ids->fetch_object()) {
						$tempArray = $row;
						array_push($myArray, $tempArray);
					}
				
				$json_encoded = json_encode($myArray);
				$array = json_decode($json_encoded,true);
				
				$ise =  $array[0]["ise"];
				$isu =  $array[0]["isu"];
				$service = mysqli_query($conn,"select name from service where ise = ".$ise);
				$service_name = mysqli_fetch_assoc($service);
				$name = json_decode(json_encode($service_name),true);
				$sname = $name['name'];

				$area = mysqli_query($conn,"select area from user where username ='".$_SESSION['user']."'");
				$area_name = (mysqli_fetch_assoc($area))['area'];

				$result =  mysqli_query($conn,"select * from pacient where isu = '".$isu."' and ise = '".$ise."' limit 1");
			
			if(mysqli_num_rows($result) > 0){
				$data = mysqli_fetch_assoc($result);
				$_SESSION['name'] = $data['name'];
				$_SESSION['service'] = $sname;
				$_SESSION['height'] = $data['height'];
				$_SESSION['weight'] = $data['weight'];
				$_SESSION['image'] = $data['image'];
				$_SESSION['area'] = $area_name;
				$_SESSION['isu'] = $isu;
				$_SESSION['ise'] = $isu;
				$date = new DateTime($data['birth']);
				$now = new DateTime();
				$interval = $now->diff($date);
				$_SESSION['age'] = $interval->y;
				if( strcmp($data['gender'], "F") == 0 )  $_SESSION['gender'] = "venus";
				if( strcmp($data['gender'], "M") == 0 )  $_SESSION['gender'] = "mars";
			}
		}	
	}else{
		header("Location: ./");
	}
?>

<!doctype html>
<html lang="en">

<head>
	<title>Dashboard | Klorofil - Free Bootstrap Dashboard Template</title>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0">
	<!-- VENDOR CSS -->
	<link rel="stylesheet" href="assets/vendor/bootstrap/css/bootstrap.min.css">
	<link rel="stylesheet" href="assets/vendor/font-awesome/css/font-awesome.min.css">
	<link rel="stylesheet" href="assets/vendor/linearicons/style.css">
	<link rel="stylesheet" href="assets/vendor/chartist/css/chartist-custom.css">
	<!-- MAIN CSS -->
	<link rel="stylesheet" href="assets/css/main.css">
	<!-- GOOGLE FONTS -->
	<link href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,600,700" rel="stylesheet">
	<!-- ICONS -->
	<link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.8.2/css/all.css" integrity="sha384-oS3vJWv+0UjzBfQzYUhtDYW+Pj2yciDJxpsK1OYPAYjqT085Qq/1cq5FLXAZQ7Ay" crossorigin="anonymous">
	<link rel="apple-touch-icon" sizes="76x76" href="assets/img/apple-icon.png">
	<link rel="icon" type="image/png" sizes="96x96" href="assets/img/favicon.png">
</head>

<body>
	<!-- WRAPPER -->
	<div id="wrapper">
		<!-- NAVBAR -->
		<nav class="navbar navbar-default navbar-fixed-top">
			<div class="brand">
				<a href="userDashboard.php"><img src="assets/img/logo-dark.png" alt="Klorofil Logo" class="img-responsive logo"></a>
			</div>
			<div class="container-fluid">
				<div class="navbar-btn">
					<button type="button" class="btn-toggle-fullwidth"><i class="lnr lnr-arrow-left-circle"></i></button>
				</div>
				</form>
				<div id="navbar-menu">
					<ul class="nav navbar-nav navbar-right">
						<li><a style="cursor:pointer;" id="btnProfile"><i class="lnr lnr-user"></i> <span>Editar palavra-passe</span></a></li>
						<li><a href="logout.php"><i class="lnr lnr-exit"></i> <span>Terminar sessão</span></a></li>
						<li><a href="#"><img src="<?php echo $_SESSION['image'] ?>" class="img-circle" alt="Avatar"> <span><?php echo $_SESSION['user'] ?></span></a></li>
						<!-- <li>
							<a class="update-pro" href="https://www.themeineed.com/downloads/klorofil-pro-bootstrap-admin-dashboard-template/?utm_source=klorofil&utm_medium=template&utm_campaign=KlorofilPro" title="Upgrade to Pro" target="_blank"><i class="fa fa-rocket"></i> <span>UPGRADE TO PRO</span></a>
						</li> -->
					</ul>
				</div>
			</div>
		</nav>
		<!-- END NAVBAR -->
		<!-- LEFT SIDEBAR -->
		<div id="sidebar-nav" class="sidebar">
			<div class="sidebar-scroll">
				<nav>
					<ul class="nav">
						<div class="card" id="sideBarPatientContent">
							<div class="details" style = "margin-bottom: 0;">
								<span style="margin-top: 0px;"> <?php echo $_SESSION['service'] ?> </span>
								<span id="patientArea" style="font-size:18px;color: #aeb7c2"> @<?php echo $_SESSION['area'] ?> </span>
							</div>
							<p id="patientUsername" style="font-size:21px;color: #eaeaea;margin-top: 0;"><?php echo $_SESSION['user'] ?>#<?php echo $_SESSION['isu'] ?></p>
							<img id="patientImage" src="<?php echo $_SESSION['image'] ?>" alt="profile">
							<div class="details" style = "margin-bottom: 0;">
								<span id="choosegender" class="input-group-addon patientData"> <span style="font-size:25px;;margin-right: 3px;" id="patientName"> <?php echo $_SESSION['name'] ?> </span> <i style="font-size:25px;" class="fas fa-<?php echo $_SESSION['gender']?> gendericon-<?php echo $_SESSION['gender']?>"></i></span>
								
								
							</div>
							
							<hr class="remove-hr">
							<span class="input-group-addon patientData"><i class="far fa-calendar-alt" style="color:#00AAFF;"></i> <span style="font-size:18px;top:0px" id="patientAge"> <?php echo $_SESSION['age'] ?> </span> anos</span>
							<hr class="remove-hr">
							<span class="input-group-addon patientData"><i class="fas fa-ruler-vertical" style="color:#00AAFF;"></i> <span style="font-size:18px;top:0px" id="patientHeight"> <?php echo $_SESSION['height'] ?> </span> m </span>
							<!--<p class ="type2" ><span style="font-size:18px;top:0px" id="patientHeight">  </span> m</p>-->
							<hr class="remove-hr">
							<span class="input-group-addon patientData"><i class="fas fa-weight" style="color:#00AAFF;"></i> <span style="font-size:18px;top:0px" id="patientWeight"> <?php echo $_SESSION['weight'] ?> </span> kg</p></span>
						</div>
					</ul>
				</nav>
			</div>
		</div>
		<!-- END LEFT SIDEBAR -->
		<!-- MAIN -->
		<div class="main">
			<!-- MAIN CONTENT -->
			<div class="main-content" id="patientContent">
				<div class="container-fluid">			
					<div class="row">
						<div class="col-md-6">
							<!-- RECENT PURCHASES -->
							<div class="panel panel-scrolling">
								<div class="panel-heading"><h3 class="panel-title"> Registos comportamentais</h3>
								</div>
								<div id="panel-patient"  class="panel-body no-padding">
									<table class="table table-striped" id="myTable">
										<thead>
											<tr>
												<th>Data e hora</th>
												<th>Comportamento</th>
											</tr>
										</thead>
										<tbody id="myTable">
											
										</tbody>
									</table>
								</div>
								<div class="panel-footer">
									<div class="row">
										<div class="col-md-6"><input id="dateSearch" class="form-control" type="datetime-local" step="1"></div>
										<div class="col-md-6 text-right"><a id="update" class="btn btn-primary">Procurar</a></div>
									</div>
								</div>
							</div>
							<!-- END RECENT PURCHASES -->	
						</div>
						<div class="col-md-6">
							<!-- MULTI CHARTS -->
							<div class="panel">
								<div class="panel-heading"> <h3 class="panel-title"> Gráfico de barras</h3>
								</div>
								<div class="panel-body">
									<div id="bar-chart" class="ct-chart bar-style"></div>
								</div>
							</div>
							<!-- END MULTI CHARTS -->
						</div>
					</div>
				</div>
			</div>
			<!-- END MAIN CONTENT -->

			<!--PROFILE CONTENT-->
			<div class="main-content" id="profileContent" style="display:none;">
				<div class="container-fluid">
					<div class="panel panel-profile">
						<div class="clearfix">
							<!-- LEFT COLUMN -->
							<div class="panel-heading">
								<h3 class="panel-title">Editar palavra-passe</h3>
							</div>
							<div class="panel-body">
								<form id="registerForm">
									<div class="select">
										<span class="input-group-addon"><i class="fas fa-tasks"></i></span>
										<select id="SelectService" class="form-control optionService" >
											<option value="" selected disabled hidden>Selecionar serviço</option>
											
										</select>
										<select id="SelectISu" class="form-control optionISu">
											<option value="" selected disabled hidden>#ISu</option>
										</select>
									</div>
									<br>
									<div class="input-group">
										<span class="input-group-addon"><i class="fas fa-user"></i></span>
										<input id="username" class="form-control" placeholder="Nome de Utilizador" type="text" required="true">
									</div>
									<br>
									<div class="input-group">
										<span class="input-group-addon"><i class="fas fa-lock"></i></span>
										<input id="password" class="form-control" placeholder="Palavra-passe" type="password" required="true">
									</div>
									<br>
								</form>
							</div>
							<div class="panel-footer profile-footer" >
								<div class="profileFooter" style="display: inline-block;">
								<button id="btnCancel" style=" float: left; width: 25%; margin-top: 0px" type="submit" class="btnProfile btn btn-primary btn-block">Cancelar</button>
									<button id="btnEdit" style=" float: right; width: 25%; margin-top: 0px" type="submit" class="btnProfile btn btn-primary btn-block">Concluir</button>
								</div>
							</div>
							<!-- END LEFT COLUMN -->
						</div>
					</div>
				</div>
			</div>
			<!-- END PROFILE CONTENT -->
		</div>
		<!-- END MAIN -->
		<div class="clearfix"></div>
		<footer>
			<div class="container-fluid">
				<p class="copyright">&copy; 2017 <a href="https://www.themeineed.com" target="_blank">Theme I Need</a>. All Rights Reserved.</p>
			</div>
		</footer>
	</div>
	<!-- END WRAPPER -->
	<!-- Javascript -->
	<script src="assets/vendor/jquery/jquery.min.js"></script>
	<script src="assets/vendor/bootstrap/js/bootstrap.min.js"></script>
	<script src="assets/vendor/jquery-slimscroll/jquery.slimscroll.min.js"></script>
	<script src="assets/vendor/jquery.easy-pie-chart/jquery.easypiechart.min.js"></script>
	<script src="assets/vendor/chartist/js/chartist.min.js"></script>
	<script src="assets/scripts/klorofil-common.js"></script>
	<script>
		$(document).ready(function(){

					//bar charts
					var lastLabel="nada";
					var wasclicked=false;
					var isTimeEmpty = true;
					var barChartData = {
						labels: ['Deitado', 'Parado', 'Andar', 'Correr', 'Agitado', 'Queda'],
						series: [
							[0, 0, 0, 0, 0, 0],
						]
					};
					var options = {
						height: "300px",
						axisX: {
							showGrid: false
						},
					};
					var myChart = new Chartist.Bar('#bar-chart', barChartData, options);
					var barData = [0, 0, 0, 0, 0, 0];
					var myData;
					var myDataLength;
					var url = "http://your-server-name/api.php/?Q=7&username='<?php echo $_SESSION['user'] ?>'";

					updateMessageTable(url,true);

					$('#bar-chart').on('click', '.ct-chart-bar .ct-series-a line, .ct-chart-bar .ct-series-b line, .ct-chart-bar .ct-series-c line', function(evt) {
					var index = $(this).index();
					var label = $(this).closest('.ct-chart-bar').find('.ct-labels foreignObject:nth-child('+(index+1)+') span').text();
					var value = $(this).attr('ct:value');
					showOnList(index, label, value);
					});

					$('#bar-chart').on('mouseover', '.ct-chart-bar .ct-series-a line, .ct-chart-bar .ct-series-b line, .ct-chart-bar .ct-series-c line', function(evt) {
						$('#bar-chart .ct-chart-bar .ct-series-a line, .ct-chart-bar .ct-series-b line, .ct-chart-bar .ct-series-c line').css('cursor', 'pointer');
					});

					function showOnList(index, label, value) {

						if(lastLabel !== label){ // se clicar duplamente numa barra , nada acontece

							lastLabel = label;

							var messageUrl = "http://your-server-name/api.php/?Q=7&username='<?php echo $_SESSION['user'] ?>'";
						var $pedido = $.ajax({
							type:'GET',
							url: messageUrl,
							dataType:'text'
						}).done(function(data) {
								myData = JSON.parse(data);
								console.log(myData[0].tstamp)
								myDataLength = Object.keys(myData).length;
								$('#myTable tbody').empty();
								//preencher a lista com todos os campos
								for(var i=0; i<myDataLength; i++){

									if($('#myTable tbody tr').length == 0)	$('#myTable > tbody').append('<tr><td>'+myData[i].tstamp+'</td><td><span class="label label-success">'+myData[i].type+'</span></td></tr>');
									else{
										if(myData[i].type == "AGITADO" || myData[i].type == "QUEDA"){
											$('#myTable > tbody > tr:first').before('<tr id=><td>'+myData[i].tstamp+'</td><td><span class="label label-danger">'+myData[i].type+'</span></td></tr>');
										}else if(myData[i].type == "CORRER"){
											$('#myTable > tbody > tr:first').before('<tr><td>'+myData[i].tstamp+'</td><td><span class="label label-warning">'+myData[i].type+'</span></td></tr>');
										}else{
											$('#myTable > tbody > tr:first').before('<tr><td>'+myData[i].tstamp+'</td><td><span class="label label-success">'+myData[i].type+'</span></td></tr>');
										}
									}
								}

								var state;
								label = label.toUpperCase();
								//remover todos as mensagens que nao sao desejadas
								for(var i=0;i<$('#myTable tbody tr').length ;i++){

									state=($('#myTable tr:eq('+i+') td:eq(1)').text());
									if(state===label){}else{
										$("#myTable tr:eq("+i+")").remove();
										i--;
									}
								}

								// ao carregar na barra limitar o numero de estados
								while(($('#myTable tbody tr').length > 50 )){ 
											$('#myTable > tbody > tr:last').remove();
										}
						});
						}
					}

					$( "#update" ).click(function() {
						lastLabel="nada";
				
						var date;
						var time;

						$('#update').attr("disabled", true);

						var dateControl = document.querySelector('input[type="datetime-local"]');
						console.log("date control : "+dateControl);
						date=dateControl.value.split('T')[0];
						time=dateControl.value.split('T')[1];
						var timeStamp = date + " "+time;

						if(timeStamp !== ' undefined'){
							clearInterval(updatePatientTable);
							console.log('timestamp:|'+ timeStamp+'|');
							
							var newURL = "http://your-server-name/api.php/?Q=7&username='<?php echo $_SESSION['user'] ?>'&tstamp='"+timeStamp+"'";
							console.log("URL : "+newURL);

							var $pedido = $.ajax({
								type:'GET',
								url: newURL,
								dataType:'text'
							}).done(function(data) {

									myData = JSON.parse(data);

									console.log(myData[0].tstamp)
									myDataLength = Object.keys(myData).length;
									
									$('#myTable tbody').empty();

									console.log("list size "+$('#myTable tbody tr').length);

									for(var i=0; i<myDataLength; i++){
										if($('#myTable tbody tr').length == 0)	$('#myTable > tbody').append('<tr><td>'+myData[i].tstamp+'</td><td><span class="label label-success">'+myData[i].type+'</span></td></tr>');
										else{
											if(myData[i].type == "AGITADO" || myData[i].type == "QUEDA"){
												$('#myTable > tbody > tr:first').before('<tr id=><td>'+myData[i].tstamp+'</td><td><span class="label label-danger">'+myData[i].type+'</span></td></tr>');
											}else if(myData[i].type == "CORRER"){
												$('#myTable > tbody > tr:first').before('<tr><td>'+myData[i].tstamp+'</td><td><span class="label label-warning">'+myData[i].type+'</span></td></tr>');
											}else{
												$('#myTable > tbody > tr:first').before('<tr><td>'+myData[i].tstamp+'</td><td><span class="label label-success">'+myData[i].type+'</span></td></tr>');
											}
										}
									}
									$('#update').attr("disabled", false);
							});

						}
						else{
							$('#update').attr("disabled", false);
							updateMessageTable("http://your-server-name/api.php/?Q=7&username='<?php echo $_SESSION['user'] ?>'",false);
						}
						
					});


					function updateMessageTable(messageUrl,updateBar){
						
						var $pedido = $.ajax({
							type:'GET',
							url: messageUrl,
							dataType:'text'
						}).done(function(data) {
					    	myData = JSON.parse(data);
								console.log(myData[0].tstamp)
								myDataLength = Object.keys(myData).length;
											
									myData = JSON.parse(data);
									console.log( "n reverteu");
									
								for(var i=0; i<myDataLength; i++){
									if($('#myTable tbody tr').length == 0)	$('#myTable > tbody').append('<tr><td>'+myData[i].tstamp+'</td><td><span class="label label-success">'+myData[i].type+'</span></td></tr>');
									else{
										if(myData[i].type == "AGITADO" || myData[i].type == "QUEDA"){
											$('#myTable > tbody > tr:first').before('<tr id=><td>'+myData[i].tstamp+'</td><td><span class="label label-danger">'+myData[i].type+'</span></td></tr>');
										}else if(myData[i].type == "CORRER"){
											$('#myTable > tbody > tr:first').before('<tr><td>'+myData[i].tstamp+'</td><td><span class="label label-warning">'+myData[i].type+'</span></td></tr>');
										}else{
											$('#myTable > tbody > tr:first').before('<tr><td>'+myData[i].tstamp+'</td><td><span class="label label-success">'+myData[i].type+'</span></td></tr>');
										}
									}
									
									if(updateBar){
									switch (myData[i].type) {
										case "DEITADO":
											barData[0] +=1;
											break;
										case "PARADO":
											barData[1] +=1;
											break;
										case "ANDAR":
											barData[2] +=1;
											break;
										case "CORRER":
											barData[3] +=1;
											break;
										case "AGITADO":
											barData[4] +=1;
											break;
										case "QUEDA":
											barData[5] +=1;
											break;
										default:
											break;
									}
									}
								
								
								}
								isTimeEmpty = false;
								console.log(barData)
								
								barChartData.series[0] = barData;
								myChart.update(barChartData);
								$('#update').attr("disabled", false);

								
						});
					}
					
					updatePatientTable = setInterval(function(){
						
						var newURL = "http://your-server-name/api.php/?Q=7&username='<?php echo $_SESSION['user'] ?>'&tstamp='"+myData[myDataLength-1].tstamp+"'";
						console.log("http://your-server-name/api.php/?Q=7&username='<?php echo $_SESSION['user'] ?>'&tstamp='"+myData[myDataLength-1].tstamp+"'")
						
						updateMessageTable(newURL,true);
						}
					,2500);

				
			
			$('#btnProfile').click(function(){
				$('#profileContent').show();
				$('#patientContent').hide();
				console.log($( "#SelectService option:selected" ).text())
				$('#SelectService').append($("<option></option>")
									.attr("value","0")
									.attr("selected","selected")
									.text("<?php echo$_SESSION['service']; ?>"))
									.attr('disabled', true);
				$('#SelectISu').append($("<option></option>")
									.attr("value","0")
									.text("<?php echo $_SESSION['isu']; ?>")
									.attr("selected","selected")
									.attr('disabled', true));
				$('#username').val("<?php echo $_SESSION['user']; ?>").attr('disabled', true);
			})	
			$('#btnCancel').click(function(){
				$('#profileContent').hide();
				$('#patientContent').show();
			})
			$('#btnEdit').click(function(){
				var password = $('#password').val();

                console.log("http://your-server-name/api.php/?Q=20&username='<?php echo$_SESSION['user'] ?>'&password='" + password + "'")
                $.ajax({
                        url : "http://your-server-name/api.php/?Q=20&username='<?php echo$_SESSION['user'] ?>'&password='" + password + "'",
                        type : 'GET',
                        success : function(data) {
							console.log("pass atualizada");
							$('#profileContent').hide();
							$('#patientContent').show();
                        },
                        error : function(request,error){
							console.log(error);
                        }
                });
			})
			/*function updateUserTable() {
				console.log(myDataLength)
				for(var i=0; i<myDataLength; i++)
				$('#myTable > tbody > tr:first').before('<tr><td></td>'+myData.tstamp+'</td><td><span class="label label-success">'+myData.type+'</span></td></tr>');
			}

			$("#loginBtn").click(function(e){
				e.preventDefault();
				console.log("table")
				$('#myTable > tbody > tr:first').before('<tr><td>Oct 21, 2016</td><td><span class="label label-success">COMPLETED</span></td></tr>');
			});*/

			
			/*
			// line chart
			options = {
				height: "300px",
				showPoint: true,
				axisX: {
					showGrid: false
				},
				lineSmooth: false,
			};

			new Chartist.Line('#line-chart', data, options);


			// visits trend charts
			data = {
				labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
				series: [{
					name: 'series-real',
					data: [200, 380, 350, 320, 410, 450, 570, 400, 555, 620, 750, 900],
				}, {
					name: 'series-projection',
					data: [240, 350, 360, 380, 400, 450, 480, 523, 555, 600, 700, 800],
				}]
			};

			options = {
				fullWidth: true,
				lineSmooth: false,
				height: "270px",
				low: 0,
				high: 'auto',
				series: {
					'series-projection': {
						showArea: true,
						showPoint: false,
						showLine: false
					},
				},
				axisX: {
					showGrid: false,

				},
				axisY: {
					showGrid: false,
					onlyInteger: true,
					offset: 0,
				},
				chartPadding: {
					left: 20,
					right: 20
				}
			};

			new Chartist.Line('#demo-area-chart', data, options);


			// visits chart
			data = {
				labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
				series: [
					[6384, 6342, 5437, 2764, 3958, 5068, 7654]
				]
			};

			options = {
				height: 300,
				axisX: {
					showGrid: false
				},
			};

			new Chartist.Bar('#visits-chart', data, options);
			*/

			// real-time pie chart
			/*
			var sysLoad = $('#system-load').easyPieChart({
				size: 130,
				barColor: function(percent) {
					return "rgb(" + Math.round(200 * percent / 100) + ", " + Math.round(200 * (1.1 - percent / 100)) + ", 0)";
				},
				trackColor: 'rgba(245, 245, 245, 0.8)',
				scaleColor: false,
				lineWidth: 5,
				lineCap: "square",
				animate: 800
			});

			var updateInterval = 3000; // in milliseconds

			setInterval(function() {
				var randomVal;
				randomVal = getRandomInt(0, 100);

				sysLoad.data('easyPieChart').update(randomVal);
				sysLoad.find('.percent').text(randomVal);
			}, updateInterval);

			function getRandomInt(min, max) {
				return Math.floor(Math.random() * (max - min + 1)) + min;
			}*/

		});
	</script>
</body>

</html>
