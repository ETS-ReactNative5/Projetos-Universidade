import React from "react";
import { Marker,InfoWindow } from "react-google-maps";
import UserContext from '../../context/auth.js';
import Icons, { minePins, allPins, afkPins, sosPins, statusPins, speedsterPins } from '../../utils/icons.js';
import { Button } from 'reactstrap';

const typeName = ['Anónimo', 'Padrão', 'Autoritário', 'Táxi', 'Ambulância', 'Polícia', 'Reboque'];
const afkTime = 60;
const API = "http://35.246.98.225:8000/data/";

const MyInfoWindow = (props) => {
    console.log("typeId: "+ props.typeId)
    let speedLimitText, sosText, chaseBtn, sosBtn;
    if (props.typeId === 2 || props.typeId === 6)
        speedLimitText = <h4 >Velocidade máxima: {props.vehicle.speedLimit} <span className="text-muted"> Km/h</span></h4>;

    if (props.typeId === 2 && props.vehicle.sos > 0)
        sosText = <h4>SOS: {typeName[props.vehicle.sos]}</h4>;
    
    if (props.typeId === 6 && props.vehicle.speed > props.vehicle.speedLimit + 20) {
        chaseBtn =  <Button color="primary" size="sm" onClick={() => props.onClickDirections({lat:props.vehicle.latitude, lng: props.vehicle.longitude}, props.vehicle.id, 0)}>Perseguir</Button>;
    }
    if (props.typeId === props.vehicle.sos) {
        sosBtn =  <Button   color="primary" disabled={props.disabled} size="sm"
                            onClick={() => {
                                            props.onClickDirections({lat:props.vehicle.latitude, lng: props.vehicle.longitude}, props.vehicle.id, 1)
                                            }}>
                    Socorrer
                </Button>;
    }
    return (
        <InfoWindow onCloseClick={props.onCloseClick}>
            <div>
                <h3>{props.vehicle.name}</h3>
                <h4 >{props.vehicle.speed} <span className="text-muted"> Km/h</span> </h4>
                {speedLimitText}
                {sosText}
                {chaseBtn}
                {sosBtn}
                {/* <Button color="primary" size="sm" onClick={() => props.onClickDirections({lat:props.vehicle.latitude, lng: props.vehicle.longitude}, props.vehicle.id, 1)}>Socorrer</Button> */}
            </div>
        </InfoWindow>
    );
}
export default class UserMarker extends React.Component{
    static contextType = UserContext;
    state = {
        usersMarker: [],
        showIndex: null,
        isOpen: false,
        disabled: false,
        myIcon: Icons['mine'+this.context.authSession.typeId] 
    }
    disableBtn = (value) => {
        this.setState({disabled: !this.state.disabled});
    }
    
    updateMarkers(id){
        const auth = this.context;
        fetch(API+ id)
        .then(res => res.json())
        .then(response =>{
            console.log(response)
            const newLatLng = {lat: response.latitude, lng: response.longitude}
            //auth.setAuthUser(null, {lat:response.latitude, lng: response.longitude});
            console.log("setOrigin")
            auth.setInfo(response);
            this.props.setOrigin( false, newLatLng );
            this.setState({ usersMarker: response});
            console.log("center UserMarker: ", newLatLng);
            //this.props.centerOnUser(newLatLng);
        })
    }

    showInfo = ( index ) => {
        console.log("STATE",this.state);
        console.log(index)
        this.setState({
            showIndex: index,
            isOpen:true,
        });

    }
    componentDidMount(){
        console.log("here")
        console.log("pins: ", minePins)
        this.timerMarkers = setInterval(() =>{ this.updateMarkers(this.props.id)}, 5000)
        this.updateMarkers(this.props.id);
        
    }
    componentWillUnmount(){
        console.log("unmount")
        clearInterval(this.timerMarkers);
      }
    render(){
        const {latitude, longitude, lastTimestamp, vehicles} = this.state.usersMarker;
        const { typeId } = this.context.authSession;
        if(!latitude && !longitude) return (<></>);
        return(<>
        
            <Marker
                position={{ lat: latitude, lng: longitude }}
                icon={{
                    url: lastTimestamp >= Date.now()/1000 - afkTime ? minePins[typeId - 1] : afkPins[typeId - 1],
                    scaledSize: new window.google.maps.Size(40, 40)
                }}
            />
            {vehicles.map((vehicle, index)=>{
                switch (typeId) {
                    case 1:
                        if (vehicle.status > 0)
                            return(<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url: statusPins[vehicle.typeId - 3],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle}
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        break;
                    case 2:
                        if (vehicle.sos > 0){
                            return(<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url: sosPins[vehicle.typeId - 1],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        //else if (vehicle.status > 0 && (vehicle.typeId === 5 || vehicle.typeId === 6)){
                        else if (vehicle.status > 0){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url: statusPins[vehicle.typeId - 3],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle}
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn}  
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        } 
                        else if (vehicle.speed > vehicle.speedLimit + 20){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  speedsterPins[vehicle.typeId - 1],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle}
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn}  
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        else if (vehicle.status > 1){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  statusPins[vehicle.typeId - 3],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle}
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn}  
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        else{
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  allPins[vehicle.typeId - 1],
                                    scaledSize: new window.google.maps.Size(20, 20)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        break;
                    case 3:
                        if (vehicle.status > 0) {
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  statusPins[vehicle.typeId - 3],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        break; 
                    case 4:
                        if (vehicle.status > 0) {
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  statusPins[vehicle.typeId - 3],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        break; 
                    case 5:
                        if (vehicle.sos === 5) {
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  sosPins[vehicle.typeId - 1],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        else if (vehicle.status > 0){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  statusPins[vehicle.typeId - 3],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        break; 
                    case 6:
                        if (vehicle.sos === 6){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  sosPins[vehicle.typeId - 1],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        else if (vehicle.status > 0 && (vehicle.typeId === 5 || vehicle.typeId === 6)){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url: statusPins[vehicle.typeId - 3],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        else if (vehicle.speed > vehicle.speedLimit + 20){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  speedsterPins[vehicle.typeId - 1],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        else if (vehicle.status > 1){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  statusPins[vehicle.typeId - 3],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        break; 
                            
                    case 7:
                        if (vehicle.sos === 7){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  sosPins[vehicle.typeId - 1],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        else if (vehicle.status > 0){
                            return (<Marker
                                key={index}
                                position={{ lat: vehicle.latitude, lng: vehicle.longitude }}
                                icon={{
                                    url:  statusPins[vehicle.typeId - 3],
                                    scaledSize: new window.google.maps.Size(40, 40)
                                }}
                                onClick={() => this.showInfo(index)}
                            >
                                {((this.state.isOpen && this.state.showIndex === index) || this.props.showIndex === index) && 
                                    <MyInfoWindow   onCloseClick={()=>{ this.setState({isOpen:false})}}
                                                    typeId={this.props.typeId}
                                                    myId={this.props.id} 
                                                    vehicle={vehicle} 
                                                    disabled={this.state.disabled}
                                                    disableBtn={this.disableBtn} 
                                                    onClickDirections={this.props.directionsToVehicle}
                                    />
                                }
                            </Marker>
                            );
                        }
                        break;
                        default:
                            break;
                }
            })}
        </>
        );
    }
}