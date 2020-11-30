<?php
session_start();

if (isset($_SESSION['user'])){

}else{
	
	header("Location: ./");
}

?>
<!doctype html>
<html lang="en">


<body>
				<div class="main-content" id="profile-content">
					<div class="container-fluid">
						<div class="panel panel-profile">
							<div class="clearfix">
								<!-- LEFT COLUMN -->
								<div id="profileLeft" class="profile-left">
									<div class="panel-heading">
										<h3 class="panel-title">Registar utilizador</h3>
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
											<input id="username" class="form-control" placeholder="E-mail" type="text" required="true">
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
								<!-- END LEFT COLUMN -->
								<!-- RIGHT COLUMN -->
								<div id="profileRight" class="profile-right" >
									<div class="panel-heading">
											<h3 class="panel-title">Dados do paciente</h3>
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
								<!-- END RIGHT COLUMN -->
								<!-- RIGHT COLUMN ADMIN-->
								<div id="profileRightAdmin" class="profile-right" style="display:none;">
									<div class="panel-heading">
											<h3 class="panel-title">Serviços monitorizados</h3>
									</div>
									<!-- PROFILE DETAIL -->
									<div id="checkBoxServices" class="panel-body">
										
										<br>
									</div>
									<!-- END PROFILE DETAIL -->
								</div>
								<!-- END RIGHT COLUMN ADMIN-->
							</div>
							<div class="panel-footer profile-footer">
								<div class="profileFooter">
									<button id="btnRegister" type="submit" class="btnProfile btn btn-primary btn-block">Registar</button>
									</div>
								<!--<button type="submit" style="float:right;width:50%;" class="btn btn-primary btn-lg btn-block">Register</button>
								<button type="submit" style="float:left;width:50%;" class="btn btn-primary btn-lg btn-block">Cancel</button>-->
							</div>
						</div>
						
					</div>
				</div>
				<!--END MAIN CONTENT-->
			<!--</div>
		 END MAIN
		<div class="clearfix"></div>
		<footer>
			<div class="container-fluid">
				<p class="copyright">&copy; 2017 <a href="https://www.themeineed.com" target="_blank">Theme I Need</a>. All Rights Reserved.</p>
			</div>
		</footer>
	</div>-->
	<!-- END WRAPPER -->

	<!-- Javascript -->
	<script src="https://code.jquery.com/jquery-3.4.1.min.js"></script>
	<script src="assets/vendor/bootstrap/js/bootstrap.min.js"></script>
	<script src="assets/vendor/jquery-slimscroll/jquery.slimscroll.min.js"></script>
	<script src="assets/vendor/jquery.easy-pie-chart/jquery.easypiechart.min.js"></script>
	<script src="assets/scripts/klorofil-common.js"></script>
	<script>

	$(function() {
		var selectServices = [];
		
		$.ajax({
			url : 'http://your-server-name/api.php/?Q=9',
			type : 'GET',
			dataType:'json',
			success : function(data) {
				for(var i = 0;i<data.length;i++){
					//selectServices = data;
					selectServices[i] = data[i].users;
					console.log(selectServices[i])
					$('#SelectService').append($("<option></option>").attr("value",i).text(data[i].name));

					//$('#SelectISu').append($("<option></option>").attr("value",i).text(data[i].isu));
				}              
				
			},
			error : function(request,error){
				alert("Request: "+JSON.stringify(request));
			}
		});

		$('#SelectService').on('change', function() {
			if($(this).find("option:selected").text() == "Administração"){
				$('#SelectISu').attr('disabled', true);
				$('#profileRight :input').attr('disabled', true);
				$('#ImgUpload').css("background-color", "#eeeeee");
				$('#upload').css("background-color", "#eeeeee");
			}else{
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

		$("#selectImage").change(function () {
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

		$('#btnEdit').click(function(){
			console.log("select");
			console.log($('#SelectService').find(":selected").text());
		})
		
	});
	</script>
</body>

</html>