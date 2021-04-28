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
// react plugin used to create google maps
import {
  withGoogleMap,
  GoogleMap,
  Marker
} from "react-google-maps";
import { compose, withProps } from "recompose"
// reactstrap components
import { Card, Container, Row, Col, Button } from "reactstrap";
// core components
import Header from "../components/Headers/Header.jsx";
import AutoSearchInput from "./components/addressSearch";
import DistanceRenderer from "./components/distanceRenderer";
import UserMarker from "./components/userMarker.js";
import { geocodeByAddress, getLatLng } from 'react-places-autocomplete';

import UserContext  from '../context/auth.js';
import CenterButton from './components/centerButton.js';
import DisplayInfo from './components/displayInfo.js';
import DisplayRouteInfo from './components/displayRouteInfo.js';
import DisplaySearch from './components/displaySearch.js';
import SOSButtons from './components/sosButtons.js';
import Notifications from './components/notifications.js'

const API_Data = "http://35.246.98.225:8000/data/";
const API_Status = "http://35.246.98.225:8000/status";
// mapTypeId={google.maps.MapTypeId.ROADMAP}
const MyGoogleMap = compose(
    withProps({
      loadingElement: <div style={{ height: `100%` }} />,
      containerElement: <div style={{ height: `80vh` }} className="map-canvas" id="map-canvas"/> ,
      mapElement: <div style={{ height: `100%`, borderRadius: "inherit" }} />,
    }),
    withGoogleMap
    )((props) =>
      <GoogleMap
        defaultZoom={12}
        defaultCenter={{ lat: 41.4535189, lng: -8.2886961 }}
        center={ props.center }
        defaultOptions={{
          scrollwheel: true,
          styles: [
            {
              featureType: "administrative",
              elementType: "labels.text.fill",
              stylers: [{ color: "#444444" }]
            },
            {
              featureType: "landscape",
              elementType: "all",
              stylers: [{ color: "#f2f2f2" }]
            },
            {
              featureType: "poi",
              elementType: "all",
              stylers: [{ visibility: "off" }]
            },
            {
              featureType: "road",
              elementType: "all",
              stylers: [{ saturation: -100 }, { lightness: 45 }]
            },
            {
              featureType: "road.highway",
              elementType: "all",
              stylers: [{ visibility: "simplified" }]
            },
            {
              featureType: "road.arterial",
              elementType: "labels.icon",
              stylers: [{ visibility: "off" }]
            },
            {
              featureType: "transit",
              elementType: "all",
              stylers: [{ visibility: "off" }]
            },
            {
              featureType: "water",
              elementType: "all",
              stylers: [{ color: "#5e72e4" }, { visibility: "on" }]
            }
          ]
        }}
      >
        
      {/*Marker for search*/ }
      { props.data.isSearchComplete &&
          <Marker
            position={{ lat: props.searchPosition.lat, lng: props.searchPosition.lng }} 
          />
      }
      {/*Marker*/}
      
      {props.userId !== 0 && ( <>
        <UserMarker id={props.userId} typeId={props.typeId} showIndex={props.showBallon} centerOnUser={props.centerOnUser} setOrigin={props.setOrigin} directionsToVehicle={props.directionsToVehicle}/>

        <CenterButton centerOnUser={props.centerOnUser}/>
        </>
      )}
      {/*direction to search destination*/}
      {props.data.calculateDestination && 
        <DistanceRenderer
            origin={{ lat: props.data.infoMarker.latitude, lng:  props.data.infoMarker.longitude }}
            destination={{ lat: props.searchPosition.lat, lng: props.searchPosition.lng }}
            isRunning={props.checkRunning}
            getOrigin={props.setOrigin}
            type={'destination'}
        />
      }
      {/*direction to vehicle destination*/
      console.log("calcDirection: "+props.calcDirections)
      } 
      {props.calcDirections && 
        <DistanceRenderer
            destination={props.coordsVehicle}
            isRunning={props.checkRunningSOS}
            getOrigin={props.setOrigin}
            type={'vehicle'}
            vehicleId={props.vehicleId}
        />
      }   
      </GoogleMap>
);

class Maps extends React.Component {
  static contextType = UserContext;
  
  constructor(props){
    super(props);
    this.mySession = this.context;
    this.myOrigin={ lat: null, lng: null};
    this.state = {
      userId:0,
      isOpen: [false,false,false],
      isSearchComplete: false,
      searchAddress: '',
      calculateDestination: false,
      searchPosition: {
        lat: null,
        lng: null
      },
      calcDirections: false,
      vehicleId: null,
      coordsVehicle: {
        lat: null,
        lng: null
      },
      loadMaps: false,
      data: [],
      center: {
        lat: 41.4535189,
        lng: -8.2886961
      },
      showBallon: null
    }
  }

  setOrigin = ( isGet, newOrigin ) =>{
    console.log("newOrigin:",newOrigin)
    if (isGet) {
      return this.myOrigin;
    } 

    this.myOrigin = newOrigin;
  }

  componentDidMount(){
    const auth = this.context;
    console.log(auth);
    console.log(auth.authSession)
    this.mySession = auth.authSession;
    if(auth.authSession){
      console.log("didmount")
      const { id } = auth.authSession;
      fetch(API_Data + id)
      .then(res => res.json())
      .then(response =>{
        console.log(response)
        auth.setInfo(response);
        let center = {lat: response.latitude, lng: response.longitude};
        this.setState({ userId: id, loadMaps:true, center: center, data: response });
      })
    }else{
      this.setState({loadMaps: true});
    }
  }
  handleSearch = address => {
    geocodeByAddress(address)
      .then(results => getLatLng(results[0]))
      .then(latLng => {
          console.log(latLng)
          this.setState({
          isSearchComplete: true,
          searchPosition: latLng,
          center: latLng
        })
      })
      .catch(error => console.error('Error', error));
  }

  calculateDirection = e  =>{
    console.log(e);
    if(e)
      e.preventDefault();
    const { isOpen } = this.state;
    isOpen[1] = false;
    console.log("calculate");
    if(this.state.calculateDestination){
      isOpen[2] = false;
      this.setState({
        calculateDestination: false,
        isSearchComplete: false,
        isOpen
      });
    }else{
      this.setState({
        calculateDestination: true
      });
    }
  }
  calculateDirectionToVehicle = (coordsVehicle, vehicleId, type) => {
    console.log("direction to vehicle: ",coordsVehicle + " vehicleId: "+vehicleId)
    const {setSosDest} = this.context;
    let data;
    if(type === 0){
      data = {status: 1}
    }else{
      data = {status: vehicleId}
    }
    const putMethod = {
      method: 'PUT',
      headers: {
       'Content-type': 'application/json; charset=UTF-8'
      },
      body: JSON.stringify(data)
    };
    console.log("FETCHINGGGGGGGGGG"); 
    
    fetch(API_Status+'/'+this.state.userId, putMethod)
    .then(response => response.json())
    .then(data =>{
      console.log("response status")
      setSosDest(coordsVehicle, vehicleId);
      this.setState({
        calcDirections: true,
        vehicleId: vehicleId,
        coordsVehicle: coordsVehicle
      });
    }) 
    .catch(err => console.log(err)) // Do something with the error
   
  }
  
  checkRunning = ()=> {
    console.log("check")
    let φA = this.myOrigin.lat / (180 / Math.PI);
    let θA = this.myOrigin.lng / (180 / Math.PI);
    let φB = this.state.searchPosition.lat / (180 / Math.PI);
    let θB = this.state.searchPosition.lng / (180 / Math.PI);
    let R = 6371e3;
    let distance = Math.acos(Math.sin(φA) * Math.sin(φB) + Math.cos(φA) * Math.cos(φB) * Math.cos(θB - θA)) * R;
    console.log(distance);
    if(distance < 20 && this.state.calculateDestination){
      this.calculateDirection();
       
    }else{
      return this.state.calculateDestination;
    }
  }
  checkRunningSOS = () => {
    console.log("checkRunningSOS");
    return this.state.calcDirections
  }
  centerOnUser = () =>{
    //testar com o myOrigin
    this.setState({
      center: this.myOrigin
    })
  }

  toggleInfo = e =>{
    console.log("ID: ", e.target)
    const { isOpen } = this.state;
    isOpen[e.target.id] = !isOpen[e.target.id];
    console.log("TOGGLEINFO: ",isOpen)
    this.setState({ isOpen });
  }
  notificationClick = ( vehicle ) => {
    console.log(vehicle);
    this.setState({
      showBallon: vehicle,
    });
  }
  componentDidUpdate(prevProps, prevState, snapshot){
    console.log("DidUpdate: ",prevState);
    console.log(this.state)
    const { sos } = this.context;
    console.log(sos);
    if(this.state.calcDirections && sos.id === 0){
      console.log("cancel sos");
      this.setState({
        calcDirections: false,
      });
    }
  }
  render() {
    const { userId, isOpen} = this.state;
    const { sos } = this.context;
    console.log(sos)
    console.log("mySession: ",this.mySession)
    console.log("calc: "+this.state.calcDirections)
    console.log("isOpen: ", this.state.isOpen)
    const json = {
      anonymous: !this.props.loggedIn,
      isSearchComplete: this.state.isSearchComplete,
      calculateDestination: this.state.calculateDestination,
      mapPosition: this.state.mapPosition,
      searchPosition: this.state.searchPosition,
      infoMarker: this.state.data,
      polMarkers: this.state.polylineMarkers
    }
    return (
      <>
        <Header />
        {/* Page content */}
        <Container className="mt-md--8 mt--7" fluid>
          
          {userId === 0 ? (
              <AutoSearchInput 
                handleSelect={this.handleSeach}
              />
            ) : (<>
                <Notifications onClick={this.notificationClick} />
                <Row>    
                  <Col xl="3" lg="3" xs="2">
                    <Button color="primary" id={0} onClick={this.toggleInfo} style={{ marginBottom: '1rem' }}> <i className="mr-0 mr-sm-2 fas fa-info-circle align-middle  " style={{fontSize: 'large'}}></i> <span id={0} className="d-none d-sm-inline">Mostrar Detalhes</span></Button>  
                  </Col>
                  <Col xl="3" lg="3" xs="2">
                    <Button color={!this.state.calculateDestination ? "primary" : "danger"} id={1} onClick={!this.state.calculateDestination ? this.toggleInfo : this.calculateDirection} style={{ marginBottom: '1rem' }}>{!this.state.calculateDestination ? <> <i className="mr-0 mr-sm-2 fas fa-map-marked-alt align-middle" style={{fontSize: 'large'}}></i> <span id={1} className="d-none d-sm-inline">Introduzir destinatário</span></> :<> <i className="mr-0 mr-sm-2 fas fa-window-close align-middle" style={{fontSize: 'large'}}></i> <span className="d-none d-sm-inline">Cancelar Itinerário</span></> }</Button>  
                  </Col>
                  <Col xl="3" lg="3" xs="2">
                    <Button color="primary" id={2} className={(this.state.calculateDestination || this.state.calcDirections) ? 'd-block' : 'd-none'} onClick={this.toggleInfo} style={{ marginBottom: '1rem' }}><> <i className="mr-0 mr-sm-2 fas fa-route align-middle" style={{fontSize: 'large'}}></i> <span id={2} className="d-none d-sm-inline ">Detalhes de percurso</span></></Button>
                  </Col>
                  <Col xl="3" lg="3" xs="6">
                    <SOSButtons userId={userId}/>
                  </Col> 
                </Row>
                <DisplayInfo isOpen={isOpen[0]} />
                <DisplaySearch isOpen={isOpen[1]} searchAddress={this.state.searchAddress} handleSearch={this.handleSearch} isSearchComplete={this.state.isSearchComplete} calculateDestination={this.state.calculateDestination} calculateDirection={this.calculateDirection}/>
                <DisplayRouteInfo isOpen={isOpen[2]} destination={this.state.calculateDestination} vehicle={this.state.calcDirections} vehicleId={this.state.vehicleId}/>
              </>
          )}
          
          <Row>
            <div className="col mt-2">
              <Card className="shadow border-0">
                {this.state.loadMaps ? (
                  <MyGoogleMap
                    data={json}
                    center={this.state.center}
                    userId={userId}
                    typeId={this.mySession ? this.mySession.typeId: 0}
                    searchPosition={this.state.searchPosition}
                    checkRunning={this.checkRunning}
                    checkRunningSOS={this.checkRunningSOS}
                    centerOnUser={this.centerOnUser}
                    setOrigin={this.setOrigin}
                    calcDirections={this.state.calcDirections}
                    vehicleId={this.state.vehicleId}
                    coordsVehicle={this.state.coordsVehicle}
                    directionsToVehicle={this.calculateDirectionToVehicle}
                    showBallon = {this.state.showBallon}
                  /> )
                  : null
                }
                
              </Card>
            </div>
          </Row>
          
        </Container>
      </>
    );
  }
}

export default Maps;
