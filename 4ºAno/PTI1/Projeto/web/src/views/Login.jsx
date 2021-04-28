/*!

=========================================================
* Argon Dashboard React - v1.0.0
=========================================================

* Product Page: https://www.creative-tim.com/product/argon-dashboard-react
* Copyright 2019 Creative Tim (https://www.creative-tim.com)
* Licensed under MIT (https://github.com/creativetimofficial/argon-dashboard-react/blob/master/LICENSE.md)

* Coded by Creative Tim

=========================================================

* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

*/
import React from "react";
// reactstrap components
import {
  Button,
  Card,
  CardHeader,
  CardBody,
  FormGroup,
  Form,
  Input,
  InputGroupAddon,
  InputGroupText,
  InputGroup,
  Row,
  Col
} from "reactstrap";

import { Link } from 'react-router-dom';
import UserContext  from '../context/auth.js';
//192.168.1.10
//azkeryon.duckdns.org
const API = 'http://35.246.98.225:8000';
const loginEP = '/login';
const emailRegex = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

class Login extends React.Component {
  static contextType = UserContext;

  constructor(props){
    super(props);
    this.state = {
      isLoggedIn: false,
      email: '',
      password: '',
      validate: {
        emailState: '',
        passwordState: ''
      },
      formErrors: {
        email: '',
        password: ''
      }
    };
  }

  formValid = () => {
    let valid = true;
    const { validate,formErrors } = this.state;
    
    if (!this.state.email) {
      formErrors.email = 'Endereço de email é necessário';
      validate.emailState = 'invalid';
      valid = false;
    } else{
      formErrors.email = '';
      validate.emailState = 'valid';
    }
    if (!this.state.password) {
      formErrors.password = 'Palavra-passe é necessária';
      validate.passwordState = 'invalid';
      valid = false;
    } else {
      formErrors.password = '';
      validate.passwordState = 'valid';
    }

    this.setState({validate,formErrors});
    return valid;
  };

  handleInput (e) {
    let formErrors = { ...this.state.formErrors };
    let validate = { ...this.state.validate };
    const { name, value } = e.target;

    if(name === "email"){
      if(value !== "")
        formErrors.email = emailRegex.test(value)
        ? ""
        : "Endereço de email inválido";
      else
        validate.emailState = '';
    }else{
      formErrors.password = ""
      validate.passwordState = '';
    }

    this.setState({[name]: value, formErrors, validate }, () => console.log(this.state));
  }

  onSubmit = (e) => {
    e.preventDefault();
    const auth = this.context;

    if(this.formValid()){
      console.log("form valid")
      
      fetch(API + loginEP + '/' +this.state.email + '/'+ this.state.password)
      .then(res => res.json())
      .then(data =>{
          //checkLogin(data);

          console.log(data)
          auth.setAuthUser(data);
          this.setState({ isLoggedIn: true });
          this.props.history.push({
            pathname: '/admin/maps',
            state: { infoUser: data }
          })
         
      });

    }else{
      console.log(this.state.formErrors)
    }

  }

  render() {
    return (
      <>
        <Col lg="5" md="7">
          <Card className="bg-secondary shadow border-0">
            <CardHeader className="bg-transparent pb-5">
              <div className="text-muted text-center mt-2 mb-3">
                <small>Sign in with</small>
              </div>
              <div className="btn-wrapper text-center">
                <Button
                  className="btn-neutral btn-icon"
                  color="default"
                  href="#pablo"
                  onClick={e => e.preventDefault()}
                >
                  <span className="btn-inner--icon">
                    <img
                      alt="..."
                      src={require("../assets/img/icons/common/github.svg")}
                    />
                  </span>
                  <span className="btn-inner--text">Github</span>
                </Button>
                <Button
                  className="btn-neutral btn-icon"
                  color="default"
                  href="#pablo"
                  onClick={e => e.preventDefault()}
                >
                  <span className="btn-inner--icon">
                    <img
                      alt="..."
                      src={require("../assets/img/icons/common/google.svg")}
                    />
                  </span>
                  <span className="btn-inner--text">Google</span>
                </Button>
              </div>
            </CardHeader>
            <CardBody className="px-lg-5 py-lg-5">
              <div className="text-center text-muted mb-4">
                <small>Or sign in with credentials</small>
              </div>
              <Form onSubmit={this.onSubmit} noValidate>
                <FormGroup className={"mb-3 "+ (this.state.validate.emailState ==='invalid' ? 'has-danger': "") + (this.state.validate.emailState ==='valid' ? 'has-success': "")}>
                  <InputGroup className={"input-group-merge input-group-alternative " + (this.state.validate.emailState ==='invalid' ? 'is-invalid': "")} >
                    <InputGroupAddon addonType="prepend">
                      <InputGroupText>
                        <i className="ni ni-email-83 " />
                      </InputGroupText>
                    </InputGroupAddon>
                    <Input onChange={(e) => { this.handleInput(e) }}
                            placeholder="Email" type="email" name="email"
                            value={this.state.email}
                    />
                  </InputGroup>
                  <div className="invalid-feedback "> {this.state.formErrors.email}</div>
                </FormGroup>
                <FormGroup className={(this.state.validate.passwordState ==='invalid' ? 'has-danger': "") + (this.state.validate.passwordState ==='valid' ? 'has-success': "")}>
                  <InputGroup className={"input-group-alternative " + (this.state.validate.passwordState ==='invalid' ? 'is-invalid': "") }>
                    <InputGroupAddon addonType="prepend">
                      <InputGroupText>
                        <i className="ni ni-lock-circle-open" />
                      </InputGroupText>
                    </InputGroupAddon>
                    <Input onChange={(e)=> this.handleInput(e)} value={this.state.password} placeholder="Password" type="password" name="password"/>
                  </InputGroup>
                  <div className="invalid-feedback "> {this.state.formErrors.password}</div>
                </FormGroup>

                <div className="custom-control custom-control-alternative custom-checkbox">
                  <input
                    className="custom-control-input"
                    id=" customCheckLogin"
                    type="checkbox"
                  />
                  <label
                    className="custom-control-label"
                    htmlFor=" customCheckLogin"
                  >
                    <span className="text-muted">Remember me</span>
                  </label>
                </div>
                <div className="text-center">
                  <Button className="my-4" color="primary" type="submit">
                    Sign in
                  </Button>
                </div>
              </Form>
            </CardBody>
          </Card>
          <Row className="mt-3">
            <Col xs="6">
              <a
                className="text-light"
                href="#pablo"
                onClick={e => e.preventDefault()}
              >
                <small>Forgot password?</small>
              </a>
            </Col>
            <Col className="text-right" xs="6">
              <Link
                className="text-light"
                to="/auth/register"
              >
                <small>Create new account</small>
              </Link>
            </Col>
          </Row>
        </Col>
      </>
    );
  }
}

export default Login;
