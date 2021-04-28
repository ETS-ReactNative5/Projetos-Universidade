import React from "react";
import { BrowserRouter, Route, Switch, Redirect } from "react-router-dom";

import "./assets/vendor/nucleo/css/nucleo.css";
import "./assets/vendor/@fortawesome/fontawesome-free/css/all.min.css";
import "./assets/scss/argon-dashboard-react.scss";

import AdminLayout from "./layouts/Admin.jsx";
import AuthLayout from "./layouts/Auth.jsx";

import { UserProvider } from './context/auth.js'

export default class App extends React.Component {
    
    constructor(props){
        super(props);
        this.getInfo = null;
        this.state = {
            isloggedIn: false,
            authSession: JSON.parse(sessionStorage.getItem('session')) || null,
            typeSession: 0,
            infoUser:{
                id: null,
                email: '',
                name: '',
                typeId: null,
                typeName: ''
            },
            sos: {
                destination: null,
                id: 0
            }
        };
        this.initialState = this.state;
    }

    doLogout = () =>{
        console.log("logout")
        sessionStorage.clear();
        localStorage.clear();
        //this.setState(this.initialState,console.log(this.state));
        
    }

    authUser = (data,newLatLng) => {
        console.log("authUser");
        let session = null;
        if(data === null){
            session = JSON.parse(sessionStorage.getItem('session'));
            session.latitude = newLatLng.lat;
            session.longitude = newLatLng.lng;
            sessionStorage.setItem('session',JSON.stringify(session));
            console.log('authUser  if',session);
        }else{
            console.log('authUser else',session);
            session = data;
            sessionStorage.setItem('session',JSON.stringify(session));
            this.setState({ isloggedIn: true, authSession: session, typeSession:session.typeId });
        }
        
    }
    setInfo = (info) => {
        console.log("APP setInfo", info)
        localStorage.setItem('infoUser',JSON.stringify(info));
        this.getInfo = info;
    }
    sosDest = (destination, id) => {
        console.log("sharedDest")
        this.setState({
            sos: {
                destination: destination,
                id:id
            }
        });
    }
    render(){
        const { authSession, typeSession, sos } = this.state;
        return (
            <UserProvider value={{ authSession: authSession, typeSession: typeSession, setAuthUser: this.authUser, getInfo: this.getInfo, setInfo: this.setInfo, sos:sos, setSosDest: this.sosDest}}>
                <BrowserRouter>
                    <Switch>
                    <Route path="/admin" render={props => <AdminLayout {...props} doLogout={this.doLogout}/>} />
                    <Route path="/auth" render={props => <AuthLayout {...props} />} />
                    <Redirect from="/" to="/admin/maps" />
                    </Switch>
                </BrowserRouter>
            </UserProvider>
        );
    }
}