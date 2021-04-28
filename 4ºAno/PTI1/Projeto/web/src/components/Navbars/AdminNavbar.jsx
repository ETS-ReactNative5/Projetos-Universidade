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
import { Link } from "react-router-dom";
// reactstrap components
import {
  DropdownMenu,
  DropdownItem,
  UncontrolledDropdown,
  DropdownToggle,
  Navbar,
  NavItem,
  NavLink,
  Nav,
  Container,
  Media
} from "reactstrap";

import UserContext  from '../../context/auth.js';

const API = "http://35.246.98.225:8000/status";

const StatusDropdown = (props) => {
  if (props.typeId === 3 || props.typeId === 4) {
    console.log("status: "+props.myStatus);
    
    return (<>
        <DropdownItem className={props.myStatus === 0 ? "bg-light" : ""} id={0} onClick={props.handleStatus}>Ocupado / Off-duty</DropdownItem>
        <DropdownItem className={props.myStatus === 1 ? "bg-light" : ""} id={1} onClick={props.handleStatus}>Livre</DropdownItem>
      </>
    );
  }else{
    console.log("status: "+( props.myStatus === 0 )+(props.myStatus === 0 ? "bg-light" : ""));
      return (<>
        <DropdownItem className={props.myStatus === 0 ? "bg-light" : ""} id={0} onClick={props.handleStatus}>Sem marcha de emergência</DropdownItem>
        <DropdownItem className={props.myStatus >= 1 ? "bg-light" : ""} id={1} onClick={props.handleStatus}>Em marcha de emergência</DropdownItem>
      </>
      );
  }
}
const VehiclesDropdown = (props) => {
  if (props.typeId !== 3 && props.typeId !== 4){
    const infoUser = JSON.parse(localStorage.getItem('infoUser'));
    if(infoUser != null){ 
      console.log("vehicles not null");
      const { vehicles } = infoUser;
      console.log(vehicles)
      return (
        vehicles.map((vehicle, index) =>{
          if(vehicle.sos === props.typeId){
            return <DropdownItem key={index} className={props.myStatus === vehicle.id ? "bg-light" : ""} onClick={()=>props.handleSos(vehicle)}>{'#' + vehicle.id + ' ' + vehicle.name} </DropdownItem>
          }
            
        }
        ));
    }
  }
  return (<></>);
}
class AdminNavbar extends React.Component {
  static contextType = UserContext;
  state = {
    status: -1,
  }
  componentDidMount(){
    console.log("navbar ");
    const auth = this.context;
    if(auth.authSession){
      console.log("status: "+ auth.authSession.status);
      this.setState({status: auth.authSession.status});
    }

  }
  componentWillUnmount(){
    const doLogout = this.props.doLogout;
    doLogout();
  }
  fetchStatus = (data,id) => {
    const putMethod = {
      method: 'PUT',
      headers: {
       'Content-type': 'application/json; charset=UTF-8'
      },
      body: JSON.stringify(data) // We send data in JSON format
     };
     
     fetch(API+'/'+id, putMethod)
     .then(response => response.json())
     .then(data => console.log(data)) // Manipulate the data retrieved back, if we want to do something with it
     .catch(err => console.log(err)) // Do something with the error
  }
  handleStatus = (e) => {
    const {setSosDest} = this.context;
    console.log("id: "+ e.target.id);
    const { id } = this.context.authSession;
    let data = {status: e.target.id};
    this.fetchStatus(data, id);
    this.setState({
      status: e.target.id
    },()=> console.log(this.state.status));
    
      setSosDest(null, Number(e.target.id));
  }
  handleSosDest = (vehicle) => {
    const { id } = this.context.authSession;
    const {setSosDest} = this.context;
    const destination = { lat: vehicle.latitude, lng: vehicle.longitude};
    let data = {status: vehicle.id}
    this.fetchStatus(data,id);
    this.setState({
      status: data.status
    });
    setSosDest(destination, vehicle.id);
  }
  render() {
    const auth = this.context;
    return (
      <>
        <Navbar className="navbar-top navbar-dark" id="navbar-main">
          <Container fluid>
            <Link
              className="h4 mb-0 text-white text-uppercase d-inline-block"
              to="/"
            >
              {this.props.brandText}
            </Link>
            {!auth.authSession ? (
            <Nav className="d-flex list-unstyled justify-content-start" >
              <NavItem>
                <NavLink
                  className="text-white nav-link-icon"
                  to="/auth/register"
                  tag={Link}
                >
                  <i className="ni ni-circle-08" />
                  <span className="nav-link-inner--text">Register</span>
                </NavLink>
              </NavItem>
              <NavItem>
                <NavLink
                  className="text-white nav-link-icon"
                  to="/auth/login"
                  tag={Link}
                >
                  <i className="ni ni-key-25" />
                  <span className="nav-link-inner--text">Login</span>
                </NavLink>
              </NavItem>
            </Nav>) : ( 
              <Nav className="d-flex list-unstyled justify-content-start align-items-center"> 
                  {auth.authSession.typeId > 2 ? (
                  <UncontrolledDropdown nav>
                    <DropdownToggle className="pr-0 text-white" nav>
                      <Media className="align-items-center">
                        
                        <Media className="ml-2 d-block">
                          <span className="mb-0 text-sm font-weight-bold">
                            {auth.authSession.name}
                          </span>
                        </Media>
                      </Media>
                    </DropdownToggle>
                    <DropdownMenu className="dropdown-menu-arrow" right>
                      <DropdownItem className="noti-title" header tag="div">
                        <h6 className="text-overflow m-0">O meu estado</h6>
                      </DropdownItem>
                      <StatusDropdown typeId={auth.authSession.typeId} myStatus={this.state.status} handleStatus={this.handleStatus}/>
                      <DropdownItem divider />
                      <DropdownItem className="noti-title" header tag="div">
                        <h6 className="text-overflow m-0">Pedidos de SOS</h6>
                      </DropdownItem>
                      <VehiclesDropdown typeId={auth.authSession.typeId} myStatus={this.state.status} handleSos={this.handleSosDest}/>
                    </DropdownMenu>
                  </UncontrolledDropdown>) 
                  :(<>
                    { auth.authSession.typeId === 2 ? (
                      <>
                      <div className="pr-0 text-white">
                        <Media className="align-items-center">
                        
                        <Media className="ml-2 d-block">
                          <span className="mb-0 text-sm font-weight-bold">
                            {auth.authSession.name}
                          </span>
                        </Media>
                      </Media>
                      </div>
                      <NavItem>
                        <NavLink
                          className="text-white nav-link-icon"
                          to="/admin/profile"
                          tag={Link}
                        >
                          <i className="fas fa-users" />
                          <span className="nav-link-inner--text">Alterar Perfis</span>
                        </NavLink>
                      </NavItem>
                      </>
                    ) : (
                      <div className="pr-0 text-white">
                        <Media className="align-items-center">
                          
                          <Media className="ml-2 d-block">
                            <span className="mb-0 text-sm font-weight-bold">
                              {auth.authSession.name}
                            </span>
                          </Media>
                        </Media>
                      </div>
                    )}  
                    </>
                  )
                }
                <NavItem>
                  <NavLink
                    className="text-white nav-link-icon"
                    to="/auth/login"
                    tag={Link}
                  >
                    <i className="fa fa-sign-out-alt" />
                    <span className="nav-link-inner--text">Logout</span>
                  </NavLink>
                </NavItem>
              </Nav>
            )}
            
          </Container>
        </Navbar>
      </>
    );
  }
}

export default AdminNavbar;
