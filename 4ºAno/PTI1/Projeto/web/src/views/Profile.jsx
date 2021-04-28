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
  Container,
  Row,
  Col,
  Alert
} from "reactstrap";
// core components
import Header from "../components/Headers/Header.jsx";
import UserContext  from '../context/auth.js';

const typeName = ['Anónimo', 'Padrão', 'Autoritário', 'Táxi', 'Ambulância', 'Polícia', 'Reboque']
const API = "http://35.246.98.225:8000";
class Profile extends React.Component {
  static contextType = UserContext;
  choosedType = null;
  state = {
    userList: null,
    selectedUser:  null,
    options: null,
    userOptions: null,
    visible: false
  }
  updateOptions = (id) => {
    fetch(`${API}/users/${id}`)
    .then(res => res.json())
    .then(data => {
      console.log(data);
      const userList = data.users;
      let options;
      options = data.users.map((user, index) => {
        return <option key={index} value={index}> {'#' + user.id + ' ' + user.name} </option>
      })
      this.setState({userList, options});
    })
  }
  updateUserSelected = (id) => {
    let userOptions;
    const selectedUser = this.state.userList[id];
    console.log("typeId: ",selectedUser);
    const options = [1,2,3,4,5,6];
    userOptions = options.map((opt, index) => {
      return <option key={index} value={opt}> {(opt === selectedUser.typeId) ? typeName[opt] + ' (atual)' : typeName[opt]} </option>;
    })
    this.setState({selectedUser, userOptions});
  }
  componentDidMount() {
    const auth = this.context;
    const { id } = auth.authSession;
    this.updateOptions(id);
  }
  handleSelect = e => {
    console.log("value: "+e.target.value + " name: "+e.target.name);
    if(e.target.name === "allUsers"){
      const userSelected = e.target.value;
      this.updateUserSelected(userSelected);
    }else{
      this.choosedType = e.target.value;
      //console.log("user: "+this.state.selectedUser.id + " newType: "+e.target.value )
    }
  }
  submitType = (e) => {
    e.preventDefault();
    let type = this.choosedType === null ? 1 : this.choosedType; 
    console.log("user: "+this.state.selectedUser.id + " newType: "+type )
    const putMethod = {
      method: 'PUT',
      headers: {
      'Content-type': 'application/json; charset=UTF-8'
      },
      body: JSON.stringify({typeId: type}) // We send data in JSON format
    };
    fetch(`${API}/types/${this.state.selectedUser.id}`, putMethod)
    .then(response => response.json())
    .then(data =>{
      console.log(data)
      this.setState({visible: true});
    } ) // Manipulate the data retrieved back, if we want to do something with it
    .catch(err => console.log(err)) // Do something with the error
    
  }
  onDismiss = () => this.setState({visible: false});

  render() {
    return (
      <>
        <Header />
        {/* Page content */}
        <Container className="mt--7" fluid>
          <Row>
            <Col className="order-xl-1">
              <Card className="bg-secondary shadow">
                <CardHeader className="bg-white border-0">
                  <Row className="align-items-center">
                    <Col xs="8">
                      <h3 className="mb-0">A minha conta</h3>
                    </Col>
                    <Col className="text-right" xs="4">
                      <Button
                        color="danger"
                        href="/admin/maps"
                        size="sm"
                      >
                        Cancelar
                      </Button>
                    </Col>
                  </Row>
                </CardHeader>
                <CardBody>
                  <Form>
                    <Row>
                      <Col lg="6">
                        <h6 className="heading-small text-muted mb-4">
                          Lista de utilizadores
                        </h6>
                        <div className="pl-lg-4">
                          <Row>
                            <Col lg="12">
                              <FormGroup>
                                <label
                                    className="form-control-label"
                                    htmlFor="userSelect"
                                  >
                                    Selecionar utilizador
                                  </label>
                                <Input className="form-control-alternative" name="allUsers" defaultValue="0" type="select" id="userSelect" onChange={this.handleSelect}>
                                  <option value="0" hidden>Nenhum utilizador selecionado</option>
                                  {this.state.options}
                                </Input>
                              </FormGroup>
                            </Col>
                          </Row>
                        </div>
                      </Col>
                      <Col lg="6">
                        <h6 className="heading-small text-muted mb-4">
                          User information
                        </h6>
                        <div className="pl-lg-4">
                          <Row>
                            <Col lg="12">
                              <FormGroup>
                                <label
                                    className="form-control-label"
                                    htmlFor="userSelected"
                                  >
                                    Escolher novo tipo
                                  </label>
                                <Input className="form-control-alternative" name="newTypeId" defaultValue="0" type="select" id="userSelected" onChange={this.handleSelect}>
                                  {this.state.userOptions ? (
                                    this.state.userOptions
                                  ) : (
                                    <option value="0"> Nenhum utilizador selecionado </option>
                                  ) }
                                </Input>
                              </FormGroup>
                            </Col>
                          </Row>
                          <Row>
                            <Col>
                              <Row>
                                <Col className="col-9 d-flex">
                                  <Alert className="w-100 pt-2 pb-2 mb-0" color="success" isOpen={this.state.visible} toggle={this.onDismiss}>
                                    Utilizador atualizado com sucesso!
                                  </Alert>
                                </Col>
                                <Col className="col-3">
                                <Button
                                  className="float-right"
                                  color="primary"
                                  onClick={this.submitType}
                                >
                                  Alterar
                                </Button>
                                </Col>
                              </Row>
                            
                            
                            </Col>
                          </Row>
                        </div>
                      </Col>
                    </Row>
                  </Form>
                </CardBody>
              </Card>
            </Col>
          </Row>
        </Container>
      </>
    );
  }
}

export default Profile;
