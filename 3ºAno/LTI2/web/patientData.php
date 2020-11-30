<?php
session_start();

if (isset($_SESSION['user'])){

}else{
	
	header("Location: ./");
}

?>
<!doctype html>
<html lang="en">

<!--<head>
	<link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.8.2/css/all.css" integrity="sha384-oS3vJWv+0UjzBfQzYUhtDYW+Pj2yciDJxpsK1OYPAYjqT085Qq/1cq5FLXAZQ7Ay" crossorigin="anonymous">
	
</head>
<head>
	<title>Dashboard | Klorofil - Free Bootstrap Dashboard Template</title>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0">
	VENDOR CSS 
	<link rel="stylesheet" href="assets/vendor/bootstrap/css/bootstrap.min.css">
	<link rel="stylesheet" href="assets/vendor/font-awesome/css/font-awesome.min.css">
	<link rel="stylesheet" href="assets/vendor/linearicons/style.css">
	<link rel="stylesheet" href="assets/vendor/chartist/css/chartist-custom.css">
	 MAIN CSS 
	<link rel="stylesheet" href="assets/css/main.css">
	 GOOGLE FONTS 
	<link href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,600,700" rel="stylesheet">
	 ICONS 
	<link rel="apple-touch-icon" sizes="76x76" href="assets/img/apple-icon.png">
	<link rel="icon" type="image/png" sizes="96x96" href="assets/img/favicon.png">
</head>-->

<body>
	<!-- MAIN CONTENT -->
	<div class="main-content" id="patientContent">
		<div class="container-fluid">			
			<div class="row">
				<div class="col-md-6">
					<!-- RECENT PURCHASES -->
					<div class="panel panel-scrolling">
						<div class="panel-heading">
							<h3 class="panel-title">Recent Purchases</h3>
							<div class="right">
								<button type="button" class="btn-toggle-collapse"><i class="lnr lnr-chevron-up"></i></button>
								<button type="button" class="btn-remove"><i class="lnr lnr-cross"></i></button>
							</div>
						</div>
						<div id="panel-patient" class="panel-body no-padding">
							<table class="table table-striped" id="myTable">
								<thead>
									<tr>
										<th>Date &amp; Time</th>
										<th>Status</th>
									</tr>
								</thead>
								<tbody id="myTable">
									
								</tbody>
							</table>
						</div>
						<div class="panel-footer">
							<div class="row">
								<div class="col-md-6"><span class="panel-note"><i class="fa fa-clock-o"></i> Last 24 hours</span></div>
								<div class="col-md-6 text-right"><a id="update" class="btn btn-primary">View All Purchases</a></div>
							</div>
						</div>
					</div>
					<!-- END RECENT PURCHASES -->	
				</div>
				<div class="col-md-6">
					<!-- MULTI CHARTS -->
					<div class="panel">
						<div class="panel-heading">
							<h3 class="panel-title">Bar Chart</h3>
							<div class="right">
								<button type="button" class="btn-toggle-collapse"><i class="lnr lnr-chevron-up"></i></button>
								<button type="button" class="btn-remove"><i class="lnr lnr-cross"></i></button>
							</div>
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
	<!--END MAIN CONTENT-->

	<!-- SIDE CONTENT -->
	<div class="card" id="sideBarPatientContent">
		<div class="details" style = "margin-bottom: 0;">
			<span style="margin-top: 0px;" id="patientService"> </span>
			<span id="patientArea" style="font-size:18px;color: #aeb7c2"></span>
		</div>
		<p id="patientUsername" style="font-size:21px;color: #eaeaea;margin-top: 0;"></p>
		<img id="patientImage" src="" alt="profile">
		<div class="details" style = "margin-bottom: 0;">
			<span id="choosegender" class="input-group-addon patientData"> <span style="font-size:25px;;margin-right: 3px;" id="patientName"></span> <i style="font-size:25px;" class=""></i></span>
		</div>
		
		<hr class="remove-hr">
		<span class="input-group-addon patientData"><i class="far fa-calendar-alt" style="color:#00AAFF;"></i> <span style="font-size:18px;top:0px" id="patientAge"> </span> anos</span>
		<hr class="remove-hr">
		<span class="input-group-addon patientData"><i class="fas fa-ruler-vertical" style="color:#00AAFF;"></i> <span style="font-size:18px;top:0px" id="patientHeight">  </span> m </span>
		<!--<p class ="type2" ><span style="font-size:18px;top:0px" id="patientHeight">  </span> m</p>-->
		<hr class="remove-hr">
		<span class="input-group-addon patientData"><i class="fas fa-weight" style="color:#00AAFF;"></i> <span style="font-size:18px;top:0px" id="patientWeight">  </span> kg</p></span>
	</div>
	<!--END SIDE CONTENT -->

	<!-- EDIT PATIENT CONTENT -->
	<div class="main-content" id="editProfilePatient">
		<div class="container-fluid">
			<div class="panel panel-profile">
				<div class="clearfix">
						<div class="panel-heading">
								<h3 class="panel-title">Editar dados do paciente</h3>
						</div>
						<!-- PROFILE DETAIL -->
						<div class="panel-body">
							<div class="input-group">
								<span class="input-group-addon"><i class="fas fa-id-card"></i></span>
								<input id="name" class="form-control" placeholder="Nome completo" type="text">
							</div>
							<br>
							<!-- PROFILE HEADER -->
							<div class="profile-header">
								<div class="overlay"></div>
								<div class="profile-main">
									<img id="output" src="assets/img/avatar.png" class="img-circle previewImg" alt="Avatar">
									<br>
									<div id="upload" class="input-group centerContent">
										<span class="input-group-addon"><i class="fas fa-cloud-upload-alt"></i></span>
										<label id="ImgUpload" for="selectImage" class="custom-file-upload">Selecionar imagem</label>
										<input id="selectImage" type="file" accept="image/*"/>
										<label id="file-name" class="fileLabel" style="color:black;"></label>
									</div>
								</div>
							</div>
							<br>
							<div class="input-group">
								<span class="input-group-addon"><i class="far fa-calendar-alt"></i></span>
								<input id="birthdate" class="form-control" type="date">
							</div>
							<br>
							<div class="select">
								<span class="input-group-addon"><i class="fas fa-venus-mars"></i></span>
								<select id="selectGender" class="form-control" >
									<option value="" selected disabled hidden>Selecionar sexo</option>
									<option value="M">Masculino</option>
									<option value="F">Feminino</option>
								</select>
							</div>
							<br>
							<div class="select">
								<div class="input-group optionHeight">
									<span class="input-group-addon"><i class="fas fa-ruler-vertical"></i></span>
									<input id="height" class="form-control" placeholder="Altura" type="number" step="0.01" pattern="[0-9]+([\.][0-9]+)?" min="0" formnovalidate>
									<span class="input-group-addon">m</span>
								</div>
								<div class="input-group optionWeight">
									<span class="input-group-addon"><i class="fas fa-weight"></i></span>
									<input id="weight" class="form-control" placeholder="Peso" type="number" step="0.01" pattern="[0-9]+([\.][0-9]+)?" min="0" formnovalidate>
									<span class="input-group-addon">kg</span>
								</div>
							</div>
							<br>
						</div>
						<!-- END PROFILE DETAIL -->
				</div>
				<div class="panel-footer profile-footer" >
					<div class="profileFooter" style="display: inline-block;">
						<button id="btnCancel" style=" float: left;margin-top: 5px; width: 25%;" type="submit" class="btnProfile btn btn-primary btn-block">Cancelar</button>
						<button id="btnDelete" style=" float: right; width: 25%;" type="submit" class="btnProfile btn btn-danger btn-block">Apagar</button>
						<button id="btnEdit" style=" float: right; width: 25%;margin-right: 5px;" type="submit" class="btnProfile btn btn-primary btn-block">Concluir</button>
					</div>
				</div>
			</div>
		</div>
	</div>
	<!-- END EDIT PATIENT CONTENT -->

	<div class="main-content" id="admin-content">
		<div class="container-fluid">
			<div class="panel panel-profile">
				<div class="clearfix">
					<!-- LEFT COLUMN -->
					<div id="profileLeft" class="profile-left"  style="position: inherit; width: 70%;">
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
					</div>
					<div id="profileRight" class="profile-right" style="width:30%;">
						<div class="panel-heading">
							<h3 class="panel-title">Serviços monitorizados</h3>
						</div>
						<div id="checkBoxServices" class="panel-body">
									
						</div>
					</div>
					<div class="panel-footer profile-footer" >
						<div class="profileFooter" style="display: inline-block;">
							<button id="btnDelete" style=" float: right; width: 25%;" type="submit" class="btnProfile btn btn-danger btn-block">Apagar</button>
							<button id="btnEdit" style="float: right; width: 25%; margin-top: 0px;margin-right: 5px;" type="submit" class="btnProfile btn btn-primary btn-block">Concluir</button>
						</div>
					</div>
					<!-- END LEFT COLUMN -->
				</div>
			</div>
		</div>
	</div>
	<!-- Javascript -->
	<script src="https://code.jquery.com/jquery-3.4.1.min.js"></script>
	<script src="assets/vendor/bootstrap/js/bootstrap.min.js"></script>
	<script src="assets/vendor/jquery-slimscroll/jquery.slimscroll.min.js"></script>
	<script src="assets/vendor/jquery.easy-pie-chart/jquery.easypiechart.min.js"></script>
	<script src="assets/scripts/klorofil-common.js"></script>
</body>

</html>