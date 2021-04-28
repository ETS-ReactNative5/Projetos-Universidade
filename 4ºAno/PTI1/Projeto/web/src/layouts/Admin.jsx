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
import { Route, Switch } from "react-router-dom";
// reactstrap components
import { Container } from "reactstrap";
// core components
import AdminNavbar from "../components/Navbars/AdminNavbar.jsx";
import AdminFooter from "../components/Footers/AdminFooter.jsx";

import routes from "../routes.js";

import UserContext, {SharedProvider}  from '../context/auth.js';

class Admin extends React.Component {
  static contextType = UserContext;

  constructor(props){
    super(props);
    this.state = {
      
    }
  }
  componentDidUpdate(e) {
    document.documentElement.scrollTop = 0;
    document.scrollingElement.scrollTop = 0;
    this.refs.mainContent.scrollTop = 0;
  }
  componentDidMount() {
    const auth = this.context;
    if(!auth.authSession || (auth.authSession && auth.authSession.typeId === 1 )){
      console.log("admin")
      this.props.history.push({
        pathname: '/admin/maps'
      })
    }
  }
  getRoutes = (routes, infoUser, isLoggedIn) => {
    
    return routes.map((prop, key) => {
      
      if (prop.layout === "/admin") {
        
        return (
          <Route
            path={prop.layout + prop.path}
            key={key}
            render={props => <prop.component {...props} infoUser={infoUser} loggedIn={isLoggedIn}/>}
          />
        );
      } else {
        return null;
      }
    });
  };
  getBrandText = path => {
    for (let i = 0; i < routes.length; i++) {
      if (
        this.props.location.pathname.indexOf(
          routes[i].layout + routes[i].path
        ) !== -1
      ) {
        return routes[i].name;
      }
    }
    return "Brand";
  };

  render() {
    const auth = this.context;
    if(auth.authSession){
      console.log(auth.authSession)
    }else{
      console.log("nothing!")
    }
    let infoUser = null;
    let isLoggedIn = false;
    try{
      infoUser = this.state.userLogin.infoUser;
      isLoggedIn = true;
    }catch( e ){ }
    console.log(infoUser + " " + isLoggedIn)
    return (
      <SharedProvider>
        
        <div className="main-content" ref="mainContent">
          <AdminNavbar
            {...this.props}
            brandText={this.getBrandText(this.props.location.pathname)}
            doLogout={this.props.doLogout}
          />
          <Switch>{this.getRoutes(routes, infoUser, isLoggedIn)}</Switch>
          <Container fluid>
            <AdminFooter />
          </Container>
        </div>
      </SharedProvider>
    );
  }
}

export default Admin;
