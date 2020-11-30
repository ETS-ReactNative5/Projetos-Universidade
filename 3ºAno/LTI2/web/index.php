<?php
session_start();
$error = false;
if (isset($_POST['user'])){
	//abrir mysql e fazer query
	$conn = mysqli_connect("localhost","root","lti");
	mysqli_select_db($conn , "WebDB"); 
	
	$_POST['user'] = $conn->real_escape_string($_POST['user']);
	$_POST['pass'] = $conn->real_escape_string($_POST['pass']);

	$result = mysqli_query ($conn,"select * from user where mail='".$_POST['user']."' and password = '".$_POST['pass']."' limit 1");
	if(mysqli_num_rows($result) > 0){
		$data = mysqli_fetch_assoc($result);
		
		$service = mysqli_query ($conn,"select name from service where ise=".$data['ise']);
		$service_name = mysqli_fetch_assoc($service)['name'];
		$_SESSION['service'] = $service_name;
		$_SESSION['user'] = $data['username'];
		$_SESSION['isu'] = $data['isu'];
		$_SESSION['access'] = $data['ise'];
		
		if($_SESSION['access'] != 0){
			header("Location: ./userDashboard.php");
		}else{
			$services = array();
			$all_serv_ises = mysqli_query ($conn,"select ise from access where isu=".$data['isu']);
			while($row = mysqli_fetch_assoc($all_serv_ises)){
				$serv_name_ = mysqli_query ($conn,"select name from service where ise=".$row['ise']);
				$serv_name = mysqli_fetch_assoc($serv_name_);
				if($serv_name['name']!='Administração')
					array_push($services, $serv_name['name']);
			}
			$_SESSION['services'] = json_encode($services);
			//echo json_encode($services);
			
			//die();
			header("Location: ./adminDashboard.php");
		}
		//header("Location: ./dashboard.php");
		//die();
	}else {
		$error = true;
	}
}
 
?>

<html>
<html lang="en" class="fullscreen-bg">

<head>
	<title>SMAF.G7 | Iniciar sessão</title>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0">
	<!-- VENDOR CSS -->
	<link rel="stylesheet" href="assets/css/bootstrap.min.css">
	<link rel="stylesheet" href="assets/vendor/font-awesome/css/font-awesome.min.css">
	<link rel="stylesheet" href="assets/vendor/linearicons/style.css">
	<!-- MAIN CSS -->
	<link rel="stylesheet" href="assets/css/main.css">
	<!-- GOOGLE FONTS -->
	<link href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,600,700" rel="stylesheet">
	<!-- ICONS -->
	<link rel="apple-touch-icon" sizes="76x76" href="assets/img/apple-icon.png">
	<link rel="icon" type="image/png" sizes="96x96" href="assets/img/favicon.png">
	<script src="http://code.jquery.com/jquery-3.4.1.min.js" ></script>
</head>

<body>
	<!-- WRAPPER -->
	<div id="wrapper">
		<div class="vertical-align-wrap">
			<div class="vertical-align-middle">
				<div class="auth-box ">
					<div class="left">
						<div class="content">
						  <div class="header">
							<div class="logo text-center"><img src="assets/img/logo-dark.png" alt="Klorofil Logo"></div>
							<p class="lead">Iniciar sessão</p>
						  </div>
						  <form class="form-auth-small" method="post" action="./" role = "form">
							<div class="form-group">
							  <label for="signin-email" class="control-label sr-only">Nome de utilizador</label> 
							  <input type="email" value="<?php if (isset($_POST['user'])) { echo htmlspecialchars($_POST['user'], ENT_QUOTES);}?>" name="user" class="form-control" id="userID" placeholder="E-mail" required oninvalid="applyValidationMail(this)" oninput="setCustomValidity('')" >
							</div>
							<div class="form-group">
							  <label for="signin-password" class="control-label sr-only">Palavra-passe</label>
							  <input type="password" name="pass" class="form-control" id="password" placeholder="Palavra passe" required oninvalid="applyValidationPass(this)" oninput="setCustomValidity('')" >
							</div>
							<div class="form-group clearfix">
							  <label class="fancy-checkbox element-left">
								<input type="checkbox">
								<span>Lembrar-me</span> </label>
							</div>

							<?php if($error){ ?>
								<div id="wrongPass" style="display:block;" class="alert alert-danger alert-dismissible" role="alert">
									<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
									<i class="fa fa-times-circle"></i> Palavra passe incorreta!
								</div>
							<?php } ?>
							<div id="resetPass" style="display:none;" class="alert alert-success alert-dismissible" role="alert">
								<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
								<i class="fa fa-check-circle"></i> A palavra passe foi enviada para o seu endereço de e-mail.
							</div>
							<button id="loginBtn" type="submit" class="btn btn-primary btn-lg btn-block">Iniciar sessão</button>
							<div class="bottom">
								<span  class="helper-text"><i class="fa fa-lock"></i> <a id="forgot" href="#">Esqueci-me da palavra passe</a></span>
							</div>
						  </form>
						</div>
					</div>
			      </div>
			</div>
		</div>
	</div>
	<!-- END WRAPPER -->
</body>
<script src="https://smtpjs.com/v3/smtp.js"></script>
<script>
	$('#forgot').click(function(){
		console.log("<?php echo $_POST['user'];?>")
		$.ajax({
			Method:"get",
			url:"http://your-server-name/api.php/?Q=21&username='<?php echo $_POST['user'];?>'",
			dataType : "text",
			success : function(result) {
				$('#wrongPass').css('display','none');
				$('#resetPass').css('display','block');
				Email.send({
					Host : "smtp.elasticemail.com",
					Username : "your-email",
					Password : "your-password",
					To : "<?php echo $_POST['user'];?>",
					From : "mail@mail.com",
					Subject : "SMAF.G7 | Reposição da palavra passe" ,
					Body : "Ex.mo/a utilizador,<br/>A sua palavra-passe é "+ result +".<br/>Atenciosamente,<br/>SMAFG7"
					}).then(
						message => console.log(message)
				);
			}
		});
	});
    function applyValidationMail(x){
        var msg = "Introduza o seu e-mail"
        x.setCustomValidity(msg);
    }
	function applyValidationPass(x){
		var msg = "Introduza a sua palavra passe"
		x.setCustomValidity(msg);
	}
</script>
</html>
