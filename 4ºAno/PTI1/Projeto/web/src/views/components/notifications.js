import React, { Component } from 'react'
import { Alert } from "reactstrap";
import UserContext from '../../context/auth.js';

const typeName = ['Anónimo', 'Padrão', 'Autoritário', 'Táxi', 'Autocarro', 'Ambulância', 'Polícia', 'Reboque'];
const notUndefined = anyValue => typeof anyValue !== 'undefined';

export default class Notifications extends Component {
    static contextType = UserContext;
    state = {
        show: [true],
        notifications: [],
        lastVehicles: null
    }
    onDimiss = (index) => {
        console.log("dimiss: "+index)
        console.log(this.state)
        const notifications = this.state.notifications;
        notifications[index] = null;
        this.setState({
            notifications: notifications
          });
    }

    checkForNotifications = (typeId, id) => {
        console.log("vehicles Not"+ typeId + " "+id)
        const infoUser = JSON.parse(localStorage.getItem('infoUser'));
        let notificationsVehicles;
        let show = [];
        if( infoUser ){
            const { vehicles } = infoUser;
            console.log(vehicles)
            notificationsVehicles = vehicles.map((vehicle, index) => {
                let notify;
               
                if ((typeId === 2 && vehicle.sos > 0) || vehicle.sos === typeId){
                    notify = true;
                    if (this.state.lastVehicles){
                        console.log("last vehicles");
                        this.state.lastVehicles.every(myVehicle => {
                            console.log(myVehicle)
                            if (vehicle.id === myVehicle.id) {
                                console.log(myVehicle.id)
                                if ((typeId === 2 && myVehicle.sos > 0) || myVehicle.sos === typeId) {
                                    console.log("notify false")
                                    notify = false;
                                    return false;
                                }
                            }else{
                                return true;
                            }
                        });
                    } 
                    console.log("notify: "+notify)
                    if(notify){
                        show[index] = true;
                        return (
                            <Alert key={index} onClick={() => this.props.onClick(index)} isOpen={this.state.show[index]} toggle={()=> this.onDimiss(index)} fade={false} color="info" >
                                <h5 >{'Utilizador ' + vehicle.name + '(#' + vehicle.id + ') fez um pedido de SOS de ' + typeName[vehicle.sos] + '!'} </h5>
                            </Alert>
                        );
                    }
                        
                }
                if ((typeId === 2 || typeId === 6) && vehicle.sos === 0 && vehicle.speed > vehicle.speedLimit + 20 && vehicle.status === 0) {
                    notify = true;
                    if (this.state.lastVehicles){
                        console.log("last vehicles");
                        this.state.lastVehicles.every(myVehicle => {
                            if (vehicle.id === myVehicle.id) {
                                if ((typeId === 2 || typeId === 6) && myVehicle.sos === 0 && myVehicle.speed > myVehicle.speedLimit + 20 && myVehicle.status === 0) {
                                    console.log("notify false")
                                    notify = false;
                                    return false;
                                }
                            }else{
                                return true;
                            }
                        });
                    } 
                    if(notify) {
                        show[index] = true;
                        return (
                            <Alert key={index} onClick={() => this.props.onClick(index)} isOpen={this.state.show[index]} toggle={()=> this.onDimiss(index)} fade={false} color="info" >
                                <h5 >{'Utilizador ' + vehicle.name + ' (#' + vehicle.id + ') está a ultrapassar o limite de velocidade!'} </h5>
                                <h5 >{'Velocidade: ' + vehicle.speed + ' km/h, limite: ' + vehicle.speedLimit} </h5>
                            </Alert>
                        );
                    }
                }
                if (vehicle.status === id) {
                    notify = true;
                    if (this.state.lastVehicles){
                        console.log("last vehicles");
                        this.state.lastVehicles.every(myVehicle => {
                            if (vehicle.id === myVehicle.id) {
                                if (vehicle.status === id) {
                                    console.log("notify false")
                                    notify = false;
                                    return false;
                                }
                            }else{
                                return true;
                            }
                        });
                    }
                    if(notify){
                        show[index] = true;
                        return (
                            <Alert key={index} isOpen={this.state.show[index]} toggle={()=> this.onDimiss(index)} fade={false} color="info" >
                                <h5 >{typeName[vehicle.typeId] + '(' + vehicle.name + ') a caminho!'} </h5>
                            </Alert>
                        );
                    }
                }   
            }).filter(notUndefined);;
            //console.log("after forEach", notificationsVehicles);
            //console.log("after forEach", show);
            this.setState({
                notifications: notificationsVehicles,
                lastVehicles: vehicles
            });
        } 
    }
    componentDidMount(){
        const { typeId, id } = this.context.authSession;
        this.interval = setInterval(() => { this.checkForNotifications(typeId, id) }, 5000);
        
    }
    componentWillUnmount() {
        clearInterval(this.interval);
    }
    shouldComponentUpdate(nextProps, nextState){
        if(nextState.notifications.length === 0){
            return false;
        }
        return true;
    }
    render() {
        //console.log(this.state.notifications)
        if(this.state.notifications) {
            
        }
        
        return (
            <div className="position-relative">
                <div className="position-absolute" style={{top:'0px',right:'0px',zIndex:1}}>
                    {this.state.notifications}
                </div>
                
            </div>
        )
    }
}
