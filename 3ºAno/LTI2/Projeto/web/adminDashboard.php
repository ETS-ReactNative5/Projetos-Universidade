<?php
session_start();

if (isset($_SESSION['user'])){
	
}else{	
	header("Location: ./");
}

?>
<!doctype html>
<html lang="en">

<head>
	<title> Dashboard | SMAF.G7</title>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0">
	<!-- VENDOR CSS -->
	<link rel="stylesheet" href="assets/vendor/bootstrap/css/bootstrap.min.css">
	<link rel="stylesheet" href="assets/vendor/font-awesome/css/font-awesome.min.css">
	<link rel="stylesheet" href="assets/vendor/linearicons/style.css">
	<link rel="stylesheet" href="assets/vendor/chartist/css/chartist-custom.css">
	<link rel="stylesheet" href="assets/vendor/chartjs/Chart.css">
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
			<a href="adminDashboard.php"><img src="assets/img/logo-dark.png" alt="Klorofil Logo" class="img-responsive logo"></a>
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
					<li><a href="#"><span><?php echo $_SESSION['user'] ?></span></a></li>
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
			<div id="sidebar-scroll" class="sidebar-scroll">
				<nav>
					<ul class="nav" id="originalNavBar">
						<li><a id="btnAdminDash" href="adminDashboard.php" class="active"><i class="lnr lnr-home"></i> <span>Lista de utilizadores</span></a></li>
						
						<li><a id="btnAddnewUser" style="cursor: pointer;" class=""><i class="lnr lnr-plus-circle"></i> <span>Registar utilizador</span></a></li>

						<li><a id="btnStats" style="cursor: pointer;" class=""><i class="fas fa-chart-bar"></i> <span>Estatisticas gerais</span></a></li>

						<div id="sideBarPatient"></div>
						
						<li><a id="btnEditPatient" style="cursor: pointer;display:none" class=""><i class="fas fa-user-edit"></i> <span>Editar paciente</span></a></li>
						
					</ul>
				</nav>
			</div>
		</div>
		<!-- END LEFT SIDEBAR -->
		<!-- MAIN -->
		<div class="main" id="content">
			<!-- MAIN CONTENT -->
			<div class="main-content" id="mainContent">
				<div class="container-fluid">
					<div id="allPatients">	
					</div>
				</div>
			<!-- END MAIN CONTENT -->
			</div>
			<!--PROFILE CONTENT-->
			<div class="main-content" id="profileContent" style="display:none;">
				<div class="container-fluid">
					<div class="panel panel-profile">
						<div class="clearfix">
							<!-- LEFT COLUMN -->
							<div class="panel-heading">
								<h3 class="panel-title">Editar administrador</h3>
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
	<script src="assets/vendor/chartjs/Chart.js"></script>
	<script src="https://smtpjs.com/v3/smtp.js"></script>
	<script>

	var updatePatientTable;
	var myPatient;

	function loadPatientData(myPatientUsername){
		$('#content').load('userDashboard.php #patientContent',function(data){
				$(function() {
					$("#panel-patient").slimScroll({
						position: "right",
						overflow: "hidden",
						width: "auto", 
						height: "430px", 
						allowPageScroll: true, 
						alwaysVisible: true     
					});
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
					var url = "http://your-server-name/api.php/?Q=7&username='"+myPatientUsername+"'";

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

							var messageUrl = "http://your-server-name/api.php/?Q=7&username='"+myPatientUsername+"'";
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
							
							var newURL = "http://your-server-name/api.php/?Q=7&username='"+myPatientUsername+"'&tstamp='"+timeStamp+"'";
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
							updateMessageTable("http://your-server-name/api.php/?Q=7&username='"+myPatientUsername+"'",false,true);
						}
						/*else{
							console.log("ta vazio");
							var stand_url = "http://your-server-name/api.php/?Q=7&username='"+myPatientUsername+"'";
							isTimeEmpty = false;
							console.log("func nodata: "+isTimeEmpty)
							updateMessageTable(stand_url,isTimeEmpty);
							
						}*/
					});


					function updateMessageTable(messageUrl,updateBar,reset){
						
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

									if(reset)
									$('#myTable tbody').empty();
									
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
						
						var newURL = "http://your-server-name/api.php/?Q=7&username='"+myPatientUsername+"'&tstamp='"+myData[myDataLength-1].tstamp+"'";
						console.log("http://your-server-name/api.php/?Q=7&username='"+myPatientUsername+"'&tstamp='"+myData[myDataLength-1].tstamp+"'")
						
						updateMessageTable(newURL,true);
						}
					,2500);

					});
			});
	}

	function loadSidebarPatient(myPatientUsername){
		$('#sideBarPatient').load('patientData.php #sideBarPatientContent',function(data){
			$(function() {
				$.ajax({
					url : "http://your-server-name/api.php/?Q=6&username='"+myPatientUsername+"'",
					type : 'GET',
					dataType:'json',
					success : function(data) {
						console.log(data)
						console.log(data.name)
						myPatient = data;
						var patientAge = Math.floor((new Date()-new Date(data.birth)) / (365.25 * 24 * 60 * 60 * 1000));
						console.log(patientAge);
						if(data.gender == "M") $('#choosegender i').addClass('fas fa-mars gendericon-mars');
						if(data.gender == "F") $('#choosegender i').addClass('fas fa-venus gendericon-venus');
						$('#patientName').text(data.name);
						$('#patientUsername').text(myPatientUsername + "#" + data.isu);
						$('#patientArea').text("@"+data.area);
						$('#patientService').text(data.service);
						$('#patientImage').attr('src',data.image);
						$('#patientAge').text(patientAge);  
						$('#patientHeight').text(data.height);  
						$('#patientWeight').text(data.weight);  

						
					},
					error : function(request,error){
						alert("Request: "+JSON.stringify(request));
					}
				});
			});
		});
	}

	function editUsers(myPatientUsername,checkUser,$files,newAdmin){
		console.log("entrou: "+checkUser);
		if(checkUser){
			if ($files != null) {
				console.log("entrou: "+checkUser);
				// Reject big files not working
				if ($files[0].size > $(this).data("max-size") * 1024) {
					console.log("Please select a smaller file");
					return false;
				}
				// Begin file upload
				console.log("Uploading file to Imgur..");
				// Replace ctrlq with your own API key
				var apiUrl = 'https://api.imgur.com/3/image';
				var apiKey = 'your-api';
				var settings = {
					async: false,
					crossDomain: true,
					processData: false,
					contentType: false,
					type: 'POST',
					url: apiUrl,
					headers: {
					Authorization: 'Client-ID ' + apiKey,
					Accept: 'application/json'
					},
					mimeType: 'multipart/form-data'
				};
				var formData = new FormData();
				formData.append("image", $files[0]);
				settings.data = formData;
				// Response contains stringified JSON
				// Image URL available at response.data.link
				$.ajax(settings).done(function(response) {
					console.log(JSON.parse(response).data.link);
					image =  JSON.parse(response).data.link;
					$('#output').attr('src', JSON.parse(response).data.link);
				});
			}			
			var name = $('#name').val();
			//var image = $('#output').attr('src');
			var birthdate = $('#birthdate').val();
			var gender = $('#selectGender').val();
			var height = $('#height').val();
			var weight = $('#weight').val();
			var image = $('#output').attr('src');
			console.log("name: "+$('#name').val());
			//console.log("image: "+$('#output').attr('src'));
			console.log("birthdate: "+$('#birthdate').val());
			console.log("gender: "+$('#selectGender').find(":selected").text());
			console.log("height: "+$('#height').val());
			console.log("weight: "+$('#weight').val());

			console.log("http://your-server-name/api.php/?Q=14&username='" + myPatientUsername + "'&name='" + name + "'&image='" + image + "'&birth='" + birthdate +"'&gender='" + gender + "'&high=" + height + "&weigth=" + weight)
			$.ajax({
				url : "http://your-server-name/api.php/?Q=14&username='" + myPatientUsername + "'&name='" + name + "'&image='" + image + "'&birth='" + birthdate +"'&gender='" + gender + "'&high=" + height + "&weigth=" + weight,
				type : 'GET',
				success : function(data) {         
					console.log("user atualizado");
					$('#btnEditPatient').removeClass("active");
					loadSidebarPatient(myPatientUsername)
					loadPatientData(myPatientUsername);
				},
				error : function(request,error){
					console.log(error);
				}
			});
		}else{
			var password = $('#password').val();
			console.log(JSON.stringify(newAdmin));
			
			$.ajax({
				url : "http://your-server-name/api.php/?Q=15",
				type : 'POST',
				data : newAdmin,
				success : function(data) {         
					console.log("admin atualizado");
					window.location.href = './adminDashboard.php';
				},
				error : function(request,error){
					console.log(error);
				}
			});
		}
		
	}

	function constructUsersList(categoryArea, chunks, index){
		$("#allPatients").append('<div class="panel">\
									<div class="panel-heading">\
										<h3 class="panel-title">'+categoryArea+'</h3>\
										<div id="btnCollapse" class="right" data-value="'+categoryArea+'Group">\
											<button type="button" class="btn-toggle-collapse"><i class="lnr lnr-chevron-up"></i></button>\
										</div>\
									</div>\
									<div id="'+categoryArea+'Group">\
									</div>\
								</div>');

		$("#"+categoryArea+"Group").append('<div id="'+categoryArea+'" class="row removeMarginRow">');

		allPatients = chunks[index].users;
		for (var j=0; j<allPatients.length; j++) {  // iter in chunk
			//console.log(j, chunks[i][j]); // show item j of the chunk number i
			var patient = chunks[index][j];
			if (index != 0) $("#"+categoryArea).append('<div class="col-md-4" ><div id="seePatient" class="cardAdmin clearfix" data-value="'+allPatients[j].username.split("@")[0]+'"><div class="avatarAdminDiv"><img class="imgAdmin" src='+allPatients[j].image+' alt=""></div><div class="InfoUserAdmin"><h4 id="header">'+allPatients[j].username.split("@")[0]+'#'+allPatients[j].isu+'</h4><h4 id="header"><b>'+allPatients[j].name+'</b></h4><h5 id="header">@'+allPatients[j].area+'</h5><h4 id="status">OFFLINE</h4></div></div>');
			//<img class="imgEdit" src='+imgEdit+' alt="" style="height: 24px;">
			else $("#"+categoryArea).append('<div class="col-md-2" ><div id="seeAdmin" class="cardAdmin clearfix" data-value="'+allPatients[j].username.split("@")[0]+'"><div><h4 id="header">'+allPatients[j].username.split("@")[0]+'#'+allPatients[j].isu+'</h4></div></div>');
			//$("#allPatients").append('<div class="col-md-4" ><div id="seePatient" class="cardAdmin" data-value="'+chunks[i][j].username+'"><h3 >Card 1</h3><p id="header">'+chunks[i][j].name+'</p><p>Some text</p></div></div>');
			console.log("width: "+$('.imgAdmin').width())
			$('.imgAdmin').css({'height':$('.imgAdmin').width()+'px'});  
			
			}
		$('#allPatients').append('</div>');
		//console.log(document.getElementById("allPatients").childNodes)
	}

	$(function() {

		var access = <?php echo  $_SESSION['isu']; ?>;
		var myServices = <?php echo $_SESSION['services'] ?>;
		var allServices = [];
		var myPatientUsername;
		var allPatients;
		var myAdmin;
		var rgbs = [
				'rgba(255, 99, 132, 0.5)',
				'rgba(54, 162, 235, 0.2)',
				'rgba(255, 206, 86, 0.2)',
				'rgba(75, 192, 192, 0.2)',
				'rgba(245, 183, 179, 1)',
				'rgba(22, 162, 55, 1)'
		]
		$("#sidebar-scroll").slimScroll({ height: '100%'});

		$.ajax({
			url : 'http://your-server-name/api.php/?Q=12',
			type : 'GET',
			dataType:'json',
			success : function(data) {
				var index = 0;
				console.log(data);
				var chunks = [];
				var map = {};
				var all = true;
				for (var i = 0; i<data.length;i++) {
					//console.log(data[i]);

					if(data[i].service != 'Administração'){
							allServices.push(data[i].service);
							chunks.push(data[i]);
					}else{
						chunks.push(data[i]);
					}
						
					
					//console.log(data)
				}
				if(access != 0){
					 index=1;
					 all=false;
				}

				for (index; index<chunks.length; index++) { // iter among chunks
					var categoryArea = chunks[index].service;
					console.log(myServices)
					if(!all){
						myServices.forEach(service => {
							if(service == categoryArea){
								console.log("js: "+categoryArea)
								console.log("php: "+myServices)
								constructUsersList(categoryArea, chunks, index);	
							}
						});
					}else{
						constructUsersList(categoryArea, chunks, index);
					}
				}	
			},
			error : function(request,error){
				alert("Request: "+JSON.stringify(request));
			}
		});

		var statusUpdate = setInterval(function(){
		
			var usersCondition = {
				users: []
			};
			$('#allPatients #seePatient').each(function(index){
				usersCondition.users.push({ 
					"user" : $(this).data("value")
				});
			});
			$.ajax({
				url : "http://your-server-name/api.php/?Q=13",
				type : 'POST',
				data : usersCondition,
				success : function(data) {
					var users = JSON.parse(data);
					for(var i = 0;i<users.length;i++){
						$('[data-value="'+users[i].username+'"]').find('#status').text([users[i].message]);	
					}	
				},error : function(data,error){
					alert("Request:");
				}
			})
			
		},5000);
		
		$('#allPatients').on('click', '#btnCollapse',function(){
			console.log($(this).data("value"));
			var group = $(this).data("value");
			console.log(group)
			$("#"+group).collapse("toggle");

		})

		$("#allPatients").on('click', '#seeAdmin', function(){
			var myAdminAccess = [];
			var selectedServices = [];
			clearInterval(statusUpdate);
			console.log($(this).data("value"));
			myAdminUsername = $(this).data("value");
			$('#content').load('patientData.php #admin-content',function(data){
				$(function(){
					console.log("allServs: "+allServices);
					$.ajax({
						url : "http://your-server-name/api.php/?Q=6&username='"+myAdminUsername+"'",
						type : 'GET',
						dataType:'json',
						success : function(data) {
							
							myAdmin = data;
							myAdminAccess = data.access;
							selectedServices = myAdminAccess;

							console.log("admin access: "+myAdminAccess);
							$('#SelectService').append($("<option></option>")
												.attr("value","0")
												.attr("selected","selected")
												.text(myAdmin.service))
												.attr('disabled', true);
							$('#SelectISu').append($("<option></option>")
												.attr("value","0")
												.attr("selected","selected")
												.text(myAdmin.isu)
												.attr('disabled', true));
							$('#username').val(myAdminUsername+"@smafg7.pt").attr('disabled', true);
							$('#password').attr('disabled', true);

							allServices.forEach(service => {
								if(myAdminAccess.includes(service)){
									$('#checkBoxServices').append('<label class="fancy-checkbox">\
																		<input id='+service+' type="checkbox" checked>\
																		<span>'+service+'</span>\
																		</label>')
								}else{
									$('#checkBoxServices').append('<label class="fancy-checkbox">\
																		<input id='+service+' type="checkbox">\
																		<span>'+service+'</span>\
																		</label>')
								}
								
							});
							
						},
						error : function(request,error){
							alert("Request: "+JSON.stringify(request));
						}
					});
					
					$("#checkBoxServices").on('click',"input[type=checkbox]",function () {
						if($(this).is(':checked')){
							console.log("checked")
							selectedServices.push(this.id)
						}else{
							console.log("!checked")
							selectedServices.splice(selectedServices.indexOf(this.id),1)
						}	
							

						console.log(selectedServices)
					});
					$('#btnEdit').click(function(){
						var admin = {
							username: "'"+myAdminUsername+"'",
							services: selectedServices
						}
						editUsers(myAdminUsername,false,0,admin); //username, tipo de user, ficheiro de imagem)
					});

					$('#btnDelete').click(function(){
						console.log("http://your-server-name/api.php/?Q=16&username='"+myAdminUsername+"'")
						$.ajax({
							url : "http://your-server-name/api.php/?Q=16&username='"+myAdminUsername+"'",
							type : 'GET',
							success : function(data) {
								window.location.href = './adminDashboard.php';
							},
							error : function(request,error){
								alert("Request: "+JSON.stringify(request));
							}
						});
					})
				})
			});
		});

		$("#allPatients").on('click', '#seePatient', function(){
			clearInterval(statusUpdate);
			myPatientUsername = $(this).data("value");
			$('#btnEditPatient').css('display', 'block');
			
			loadSidebarPatient(myPatientUsername);
			console.log("myfunc");
			loadPatientData(myPatientUsername);
			
		});

		$('#btnStats').click(function(){
			clearInterval(statusUpdate);
			clearInterval(updatePatientTable);
			$('#content').load('stats.php #stats-content',function(data){

				$(function() {
					var ctx = document.getElementById("myChart");
					console.log("loaded")
					var myChart = new Chart(ctx, {
						type: 'pie',
						data: {
								labels: [],
								datasets: [{
									label: '# of Tomatoes',
									data: [],
									backgroundColor: [/*
											'rgba(255, 99, 132, 0.5)',
											'rgba(54, 162, 235, 0.2)',
											'rgba(255, 206, 86, 0.2)',
											'rgba(75, 192, 192, 0.2)'*/
									],
									borderColor: [/*
											'rgba(255,99,132,1)',
											'rgba(54, 162, 235, 1)',
											'rgba(255, 206, 86, 1)',
											'rgba(75, 192, 192, 1)'*/
									],
									borderWidth: 2
								}]
						},
						options: {
								cutoutPercentage: 40,
								responsive: false,

						}
					});

					$.ajax({
						url : "http://your-server-name/api.php/?Q=18",
						type : 'GET',
						data : 'text',
						success : function(answ) {
							var i =0;		
							var total = 0;
							var result=[];
							var perc=0;

							setTimeout(function() {
								
								var json_data = JSON.parse(answ);

								for(var j in json_data){
									if(access!=0){
										myServices.forEach(service => {
											if(service == j){
												result.push([service, json_data [service]]);
												total = total + +json_data [service];
											}
										});
									}else{
										result.push([j, json_data [j]]);
										total = total + +json_data [j];
									}
								}

								console.log("aqui")
								$.each(JSON.parse(answ), function(key, value){
									if(access!=0){
										myServices.forEach(service => {
											if(service == key){
												perc = Math.round( ((+value / total )*100 * 10) / 10 );
												myChart.data.labels.push(service+"("+perc+"%)");
												myChart.data.datasets.forEach((dataset) => {
														dataset.data.push(value);
														dataset.backgroundColor.push(rgbs[i]);
														dataset.borderColor.push(rgbs[i]);
														dataset.borderWidth=2;
														i++;
												});
												myChart.update();
											}
										});
									}else{
										perc = Math.round( ((+value / total )*100 * 10) / 10 );
										myChart.data.labels.push(key+"("+perc+"%)");
										myChart.data.datasets.forEach((dataset) => {
												dataset.data.push(value);
												dataset.backgroundColor.push(rgbs[i]);
												dataset.borderColor.push(rgbs[i]);
												dataset.borderWidth=2;
												i++;
										});
										myChart.update();
									}
								
								});
							}, 500);
						}
					});

					var updateValuesPie = setInterval(function(){
						console.log("ad")
						var result = [];	
						var total=0;
						$.ajax({
							url : "http://your-server-name/api.php/?Q=18",
							type : 'GET',
							data : 'text',
							success : function(data) {

								var json_data = JSON.parse(data);
								console.log("update piechart: "+json_data);
								for(var i in json_data){
									if(access!=0){
										myServices.forEach(service => {
											if(service == i){
												result.push([service, json_data [service]]);
												total = total + +json_data [service];
											} 
										});
									}else{
										result.push([i, json_data [i]]);
										total = total + +json_data [i];
									}
									
								}
								var tamanho = result.length;

								for(var i=0;i<tamanho;i++){
									myChart.data.labels.pop();
									myChart.data.datasets.forEach((dataset) => {
										dataset.data.pop();
									});
								}
								
								myChart.update(0);
								var perc=0;
								
								for(var i=0;i<tamanho;i++){
									perc =Math.round( ((+result[i][1]) / total )*100 * 10) / 10 ;
								
									myChart.data.labels.push(result[i][0]+"("+perc+"%)");
									myChart.data.datasets.forEach((dataset) => {
											dataset.data.push(result[i][1]);
									});
								}
								myChart.update(0);
							}
						})
						
					},5000);
					

					/*horizontal bar*/
					var horizontalCtx = document.getElementById("myHorizontalChart");
					var myBarChart = new Chart(horizontalCtx, {
						type: 'bar',
						data: {
							labels: [],
													
							datasets: [
							{
								label:'Queda',
								data:[],
								backgroundColor: '#d8443f',
							},{
								label:'Agitado',
								data:[],
								backgroundColor: '#ff0700'
							},{
								label:'Correr',
								data:[],
								backgroundColor: '#e4cb10'
							},{
								label:'Andar',
								data:[],
								backgroundColor: '#50da19'
							},{
								label:'Parado',
								data:[],
								backgroundColor: '#4cc51b'
							},{
								label:'Deitado',
								data:[],
								backgroundColor: '#398e17'
							}]
						},
						options: {
							responsive: false,
							layout: {
								padding: 10
							},
							tooltips: {
								mode: 'index'
							},
							scales: {
								xAxes: [{
									stacked: true,
									categoryPercentage: 1.0,
            						barPercentage: 0.75
								}],
								yAxes: [{
									ticks: {
										beginAtZero: true
									},
									stacked: true,
									barThickness : 75,
									categoryPercentage: 0.1
								}]
							}
						}
					});
					var status = ['QUEDA','AGITADO','CORRER','ANDAR','PARADO','DEITADO']
					$.ajax({
						url : "http://your-server-name/api.php/?Q=17",
						type : 'GET',
						dataType:'text',
						success : function(data) {
							var aux = 0;
							var index = 0;
							var data_json = $.parseJSON(data);
							console.log(data_json)
							for(var key in data_json){
								if(access != 0){
									myServices.forEach(service => {
										if(service == key) myBarChart.data.labels.push(key);
									});
									
								}else{
									myBarChart.data.labels.push(key);
								}
								for(var i=0;i < 6;i++){
									console.log(status[i]);
									if(access != 0){ 
										//console.log("key: "+key)
										//myBarChart.data.datasets[i].data[index] = data_json[key][status[i]];
										myServices.forEach(service => {
											console.log("before includes: "+service)
											if(service == key){ 
												console.log("includes: "+service)
												//console.log("key: "+key)
												console.log(data_json[service][status[i]])
												myBarChart.data.datasets[i].data[aux] = data_json[service][status[i]];
												console.log("depois: "+myBarChart.data.datasets[i].data[aux])
											}
											aux++;
										});
										aux = 0;	
										
									}else{
										myBarChart.data.datasets[i].data[index] = data_json[key][status[i]];
									}
								}
								index++;

							}
							myBarChart.update();

						}
					});
					var updateHorizontalChart = setInterval(function(){
						$.ajax({
						url : "http://your-server-name/api.php/?Q=17",
						type : 'GET',
						dataType:'text',
						success : function(data) {
							var aux = 0;
							var index = 0;
							var data_json = $.parseJSON(data);
							console.log(data_json)

							for(var key in myBarChart.data.datasets){
								console.log("key"+key);
								myBarChart.data.datasets[key].data=[];
								console.log("data: "+myBarChart.data.datasets[key].data)
							}
							for(var key in data_json){
								if(access != 0){
									myServices.forEach(service => {
										if(!myBarChart.data.labels.includes(service)) myBarChart.data.labels.push(service);
									});
								}else{
									if(!myBarChart.data.labels.includes(key)) myBarChart.data.labels.push(key);
								}
								for(var i=0;i < 6;i++){
									console.log(status[i]);
									if(access != 0){ 
										//console.log("key: "+key)
										//myBarChart.data.datasets[i].data[index] = data_json[key][status[i]];
										myServices.forEach(service => {
											console.log("before includes: "+service)
											if(service == key){ 
												console.log("includes: "+service)
												//console.log("key: "+key)
												console.log(data_json[service][status[i]])
												myBarChart.data.datasets[i].data[aux] = data_json[service][status[i]];
												console.log("depois: "+myBarChart.data.datasets[i].data[aux])
											}
											aux++;
										});
										aux = 0;	
										
									}else{
										myBarChart.data.datasets[i].data[index] = data_json[key][status[i]];
									}
								}
								index++;

							}
							myBarChart.update();
					}
						});
					}, 5000);
				});
     
			});

			$('#sideBarPatient').empty();
			$('#sideBarPatient').next('li').remove();

			$('#btnAdminDash').removeClass("active");
			$('#btnAddnewUser').removeClass("active");
			$('#btnStats').addClass("active");

		});
		
		$('#btnAddnewUser').click(function(){
			clearInterval(statusUpdate);
			clearInterval(updatePatientTable);

			$('#sideBarPatient').empty();
			$('#sideBarPatient').next('li').remove();

			$('#btnAdminDash').removeClass("active");
			$('#btnStats').removeClass("active");
			
			$('#btnAddnewUser').addClass("active");


			$('#content').load('page-profile.php #profile-content',function(data){
				$(function() {

						var $files;
						var selectServices = [];
						var services = [];
						$.ajax({
							url : 'http://your-server-name/api.php/?Q=9',
							type : 'GET',
							dataType:'json',
							success : function(data) {
								for(var i = 0;i<data.length;i++){
									//selectServices = data;
									if(access != 0){
										if(data[i].name != "Administração"){
											myServices.forEach(service => {
												if(service == data[i].name){
													selectServices[i] = data[i].users;
													services.push(data[i].name);
													console.log(selectServices[i])
													$('#SelectService').append($("<option></option>").attr("value",i).text(data[i].name));
												}
											})
										}
									}else{
										selectServices[i] = data[i].users;
										console.log(selectServices[i])
										$('#SelectService').append($("<option></option>").attr("value",i).text(data[i].name));
										if(data[i].name != "Administração"){
											$('#checkBoxServices').append('<label class="fancy-checkbox">\
																		<input id='+data[i].name+' type="checkbox">\
																		<span>'+data[i].name+'</span>\
																		</label>')
										}
									}
									
								}              
								
							},
							error : function(request,error){
								alert("Request: "+JSON.stringify(request));
							}
						});

						$('#SelectService').on('change', function() {
							if($(this).find("option:selected").text() == "Administração"){
								//change
								$('#profileRight').hide();
								$('#profileRightAdmin').show();
								$('#profileLeft').css('position','unset')
								$('#SelectISu').attr('disabled', true);
								$('#profileRight :input').attr('disabled', true);
								$('#ImgUpload').css("background-color", "#eeeeee");
								$('#upload').css("background-color", "#eeeeee");
							}else{
								$('#profileRightAdmin').hide();
								$('#profileRight').show();
								$('#profileLeft').css('position','absolute')
								$('#SelectISu').attr('disabled', false);
								$('#profileRight :input').attr('disabled', false);
								$('#ImgUpload').css("background-color", "#00AAFF");
								$('#upload').css("background-color", "#fdfdfd");

							}
							
							$('#SelectISu option').remove();
							$.each(selectServices[this.value], function(key, value) {
								$('#SelectISu')
									.append($("<option></option>")
									.attr("value",key)
									.text(value.isu));
							});
						});
						console.log("servs: "+services)
						var selectedServices = [];
						$("#checkBoxServices").on('click',"input[type=checkbox]",function () {
							if($(this).is(':checked')){
								console.log("checked")
								selectedServices.push(this.id)
							}else{
								console.log("!checked")
								selectedServices.splice(selectedServices.indexOf(this.id),1)
							}	
								

							console.log(selectedServices)
						});

						$("#selectImage").change(function () {
							$files = $(this).get(0).files;
							if (this.files && this.files[0]) {
								$("#file-name").text(this.files[0].name);
								var reader = new FileReader();
								reader.onload = function(e) {
									$('#output').attr('src', e.target.result);

								}
								reader.readAsDataURL(this.files[0]);
								
							}
						});
						function imageIsLoaded(e) {
							$('#output').attr('src', e.target.result);
						};
						

						$('#btnRegister').click(function(){
							var user = true;
							/*if(!$('#registerForm')[0].checkValidity()){
								
								console.log("not valid")
								document.getElementById('username').setCustomValidity('Your custom validation message comes here');
							}*/
							var service = $('#SelectService').find(":selected").text()
							var isu = $('#SelectISu').find(":selected").text();
							var mail = $('#username').val();
							var mailtemp = mail;
							var username  = mailtemp.split("@")[0];
							var password = $('#password').val();
							var name = $('#name').val();
							var image = $('#output').attr('src');
							var birthdate = $('#birthdate').val();
							var gender = $('#selectGender').val();
							var height = $('#height').val();
							var weight = $('#weight').val();
							var str = "SMAF.G7";
							var link = str.link("http://your-server-name/lti/");
							Email.send({
								Host : "smtp.elasticemail.com",
								Username : "your-email",
								Password : "your-password",
								To : mail,
								From : "mal@mal.com",
								Subject : "SMAF.G7 | Registo" ,
								Body : "Bem-vindo(a) ao SMAF.G7, "+name+", \n\n A sua palavra-passe é "+password+". Pode alterar a sua palavra-passe mais tarde.\n Pode aceder à sua monitorização, clique aqui "+link+""
								}).then(
									message => console.log(message)
							);
							if(service == "Administração"){		
								var data = {
									mail : "'"+mail+"'",
									username :"'"+username+"'",
									password : "'"+password+"'",
									services : selectedServices
								}
								console.log("admin:"+ JSON.stringify(data));
								console.log("http://your-server-name/api.php/?Q=10&username='" + username + "'&password='" + password+"'")
								$.ajax({
									url : "http://your-server-name/api.php/?Q=19",
									type : 'POST',
									data: data,
									success : function(data) {         
										console.log("user registado:"+data);
										window.location.href = './adminDashboard.php';
									},
									error : function(request,error){
										console.log(error);
									}
								});
							}else{
								if(image != "assets/img/avatar.png")
									if ($files.length) {
										// Reject big files not working
										if ($files[0].size > $(this).data("max-size") * 1024) {
											console.log("Please select a smaller file");
											return false;
										}
										// Begin file upload
										console.log("Uploading file to Imgur..");
										// Replace ctrlq with your own API key
										var apiUrl = 'https://api.imgur.com/3/image';
										var apiKey = 'your-api-key';
										var settings = {
											async: false,
											crossDomain: true,
											processData: false,
											contentType: false,
											type: 'POST',
											url: apiUrl,
											headers: {
											Authorization: 'Client-ID ' + apiKey,
											Accept: 'application/json'
											},
											mimeType: 'multipart/form-data'
										};
										var formData = new FormData();
										formData.append("image", $files[0]);
										settings.data = formData;
										// Response contains stringified JSON
										// Image URL available at response.data.link
										$.ajax(settings).done(function(response) {
											console.log(JSON.parse(response).data.link);
											image =  JSON.parse(response).data.link;
											$('#output').attr('src', JSON.parse(response).data.link);
										});
									}
								console.log("http://your-server-name/api.php/?Q=10&iseName='" + service + "'&isu=" + isu + "&mail='" + mail +"'&username='" + username + "'&password='" + password +"'&name='" + name + "'&image='" + image + "'&birth='" + birthdate +"'&gender='" + gender + "'&high=" + height + "&weigth=" + weight)
								$.ajax({
									url : "http://your-server-name/api.php/?Q=10&iseName='" + service + "'&isu=" + isu + "&mail='" + mail +"'&username='" + username + "'&password='" + password +"'&name='" + name + "'&image='" + image + "'&birth='" + birthdate +"'&gender='" + gender + "'&high=" + height + "&weigth=" + weight,
									type : 'GET',
									success : function(data) {         
										console.log("user registado");
										window.location.href = './adminDashboard.php';
									},
									error : function(request,error){
										console.log(error);
									}
								});
							}	
						
					})
						
				});
			});
					
		});

		$('#btnEditPatient').click(function(){
			clearInterval(updatePatientTable);
			console.log("edit")
			console.log(myPatient)
			$('#btnEditPatient').addClass("active");
			$('#btnAdminDash').removeClass("active");
			$('#content').load('patientData.php #editProfilePatient',function(data){
					$(function() {
						var $files;
						var oldImage = myPatient.image;
						$('#btnCancel').on('click',function(){
							console.log("cancel")
							$('#btnEditPatient').removeClass("active");
							loadPatientData(myPatientUsername);
						})

						$('#SelectService').append($("<option></option>")
												.attr("value","0")
												.attr("selected","selected")
												.text(myPatient.service));
						$('#SelectISu').append($("<option></option>")
												.attr("value","0")
												.attr("selected","selected")
												.text(myPatient.isu));
						$('#SelectISu').attr('disabled', true);
						$('#SelectService').attr('disabled', true);
						$('#username').val(myPatientUsername);
						$('#name').val(myPatient.name);
						$('#output').attr('src', myPatient.image);
						$('#birthdate').val(myPatient.birth);
						$("#selectGender option[value='"+myPatient.gender+"']").prop('selected', true);
						$('#height').val(myPatient.height);
						$('#weight').val(myPatient.weight);

						$("#selectImage").change(function () {
							$files = $(this).get(0).files;
							if (this.files && this.files[0]) {
								$("#file-name").text(this.files[0].name);
								var reader = new FileReader();
								reader.onload = function(e) {
									$('#output').attr('src', e.target.result);

								}
								reader.readAsDataURL(this.files[0]);
								
							}
						});
						console.log()
						$('#btnEdit').click(function(){
							var checkUser = true;
							editUsers(myPatientUsername,checkUser,$files);
						});
						$('#btnDelete').click(function(){
								console.log("http://your-server-name/api.php/?Q=16&username='"+myPatientUsername+"'")
							$.ajax({
								url : "http://your-server-name/api.php/?Q=16&username='"+myPatientUsername+"'",
								type : 'GET',
								success : function(data) {
										window.location.href = './adminDashboard.php';
								},
								error : function(request,error){
										alert("Request: "+JSON.stringify(request));
								}
							});
						})
					});
				
			});
		})

		$('#btnAdminDash').click(function(){
			$('#btnAddnewUser').removeClass("active");
		});

		$('#btnProfile').click(function(){
			console.log("profile")

			console.log("profile2")

				$('#profileContent').show();
				$('#mainContent').hide();
				console.log(myAdmin)
				console.log($( "#SelectService option:selected" ).text())
				$('#SelectService').append($("<option></option>")
									.attr("value","0")
									.attr("selected","selected")
									.text("<?php echo $_SESSION['service'] ?>"))
									.attr('disabled', true);
				$('#SelectISu').append($("<option></option>")
									.attr("value","0")
									.text("<?php echo $_SESSION['isu'] ?>")
									.attr("selected","selected")
									.attr('disabled', true));
				$('#username').val("<?php echo $_SESSION['user']; ?>").attr('disabled', true);
			})	
			$('#btnCancel').click(function(){
				$('#profileContent').hide();
				$('#mainContent').show();
			})
			$('#btnEdit').click(function(){
				
				var password = $('#password').val();

                console.log("http://your-server-name/api.php/?Q=15&username='<?php echo$_SESSION['user'] ?>'&password='" + password + "'")
                $.ajax({
                        url : "http://your-server-name/api.php/?Q=15&username='<?php echo$_SESSION['user'] ?>'&password='" + password + "'",
                        type : 'GET',
                        success : function(data) {
							console.log("pass atualizada");
							$('#profileContent').hide();
							$('#mainContent').show();
                        },
                        error : function(request,error){
							console.log(error);
                        }
                });
			})
	});
	</script>
</body>

</html>
