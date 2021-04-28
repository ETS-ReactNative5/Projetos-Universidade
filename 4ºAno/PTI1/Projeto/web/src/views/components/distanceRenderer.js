import React from "react";
import { Polyline } from "react-google-maps";
import decodePolyline from 'decode-google-map-polyline';
import UserContext  from '../../context/auth.js'
import axios from 'axios';

const trafficColor = ['#01b20d','#edbf26','#ff2d2d'];
const API_Traffic = 'http://35.246.98.225:8000/traffic/';



export default class DistanceRender extends React.Component {
    static contextType = UserContext;
    _isMounted = false;
    state = {
       directions: [],
       error: null,
       isRunning: this.props.isRunning
    };
    
    

    updateDirections(origin, destination) {
        console.log("updateDirections");
        let lines=[];
        const directionsService = new window.google.maps.DirectionsService();
        directionsService.route(
            {
            origin: origin,
            destination: destination,
            travelMode: window.google.maps.TravelMode.DRIVING,
            provideRouteAlternatives: false,
            drivingOptions: {
                departureTime: new Date(Date.now()),  // for the time N milliseconds from now.
                trafficModel: 'optimistic'
              }
            },
            (result, status) => {
                console.log("STATUS", status)
                if (status === window.google.maps.DirectionsStatus.OK) {
                    console.log(result)
                    
                    let points = result.routes[0].legs[0].steps;
                    let middlePoints = [];
                    points.map( (path,index) =>{
                        let polyline = decodePolyline(path.polyline.points);
                        lines.push({ key: index, distance: path.distance.text, polyline });
                        let middlePoint = polyline[Math.floor(polyline.length/2)];
                        middlePoints.push({ latitude: middlePoint.lat,
                                            longitude: middlePoint.lng,
                                            routeLength: path.distance.value,
                                            routeDuration: path.duration.value
                                        });
                    });         
                    
                    

                    const getTraffic = () => {
                        axios.get(API_Traffic + JSON.stringify(middlePoints),{timeout:2000})
                        .then(response => {/* handle the response */
                            console.log("Result: ",response)
                            const res = response.data;
                            const routeInfo = { address: result.routes[0].legs[0].end_address,
                                distance: result.routes[0].legs[0].distance.value,
                                duration: result.routes[0].legs[0].duration_in_traffic.value,
                                delay: res.delay 
                            };
                            localStorage.setItem('routeInfo',JSON.stringify(routeInfo))
                            let newLines = lines.map((line,index)=>{
                                return ( {...line, traffic: res.traffic[index]} );
                            });
                            
                            if (this._isMounted) {
                                this.setState({
                                    directions: newLines
                                });
                            }
                            console.log( this.props.isRunning())
                            if (this.props.isRunning()) {
                                const origin = this.props.getOrigin(true);
                                console.log("type: "+this.props.type+ " vehicleId: "+this.props.vehicleId);
                                if( this.props.type === 'vehicle'){
                                    setTimeout(() => {
                                        const { vehicles } = JSON.parse(localStorage.getItem('infoUser'));
                                        const vehicle = vehicles.find(v => v.id === this.props.vehicleId);
                                        this.updateDirections(origin , {lat: vehicle.latitude, lng: vehicle.longitude});
                                    }, 2000);
                                    
                                }
                                if( this.props.type === 'destination')
                                    setTimeout(() => {
                                        this.updateDirections(origin , destination);
                                    }, 2000);
                                    
                            }
                        })
                        .catch(error => {
                            console.error('timeout exceeded');
                            console.log("ERRRRRRRRRRRRRRRRRRRRROO")
                            if (this.props.isRunning()) {
                                const origin = this.props.getOrigin(true);
                                console.log("type: "+this.props.type+ " vehicleId: "+this.props.vehicleId);
                                if( this.props.type === 'vehicle'){
                                    setTimeout(() => {
                                        const { vehicles } = JSON.parse(localStorage.getItem('infoUser'));
                                        const vehicle = vehicles.find(v => v.id === this.props.vehicleId);
                                        this.updateDirections(origin , {lat: vehicle.latitude, lng: vehicle.longitude});
                                    }, 2000);
                                    
                                }
                                if( this.props.type === 'destination')
                                    setTimeout(() => {
                                        this.updateDirections(origin , destination);
                                    }, 2000);
                                    
                            }
                
                        });
                    }
                    getTraffic();
                    /*fetch(API_Traffic + JSON.stringify(middlePoints))
                    .then(response => response.json())
                    .then(res => {
                        console.log("Result: ",res)
                        const routeInfo = { address: result.routes[0].legs[0].end_address,
                            distance: result.routes[0].legs[0].distance.text,
                            duration: result.routes[0].legs[0].duration_in_traffic.value,
                            delay: res.delay 
                        };
                        localStorage.setItem('routeInfo',JSON.stringify(routeInfo))
                        let newLines = lines.map((line,index)=>{
                            return ( {...line, traffic: res.traffic[index]} );
                        });
                       
                        if (this._isMounted) {
                            this.setState({
                                directions: newLines
                            });
                        }
                        console.log( this.props.isRunning())
                        if (this.props.isRunning()) {
                            const origin = this.props.getOrigin(true);
                            console.log("type: "+this.props.type+ " vehicleId: "+this.props.vehicleId);
                            if( this.props.type === 'vehicle'){
                                setTimeout(() => {
                                    const { vehicles } = JSON.parse(localStorage.getItem('infoUser'));
                                    const vehicle = vehicles.find(v => v.id === this.props.vehicleId);
                                    this.updateDirections(origin , {lat: vehicle.latitude, lng: vehicle.longitude});
                                }, 2000);
                                
                            }
                            if( this.props.type === 'destination')
                                setTimeout(() => {
                                    this.updateDirections(origin , destination);
                                }, 2000);
                                
                        }
                    });*/
                }else if(status === window.google.maps.DirectionsStatus.OVER_QUERY_LIMIT){
                    setTimeout(3000)
                }else {
                this.setState({ error: result });
                }
            }
        );
    }
    componentDidMount() {
        this._isMounted = true;
        const { destination } = this.props;
        const origin = this.props.getOrigin(true);
        console.log("origin distanceRenderer: ",origin);
        console.log("destination distanceRenderer: ",destination);
        this.updateDirections( origin, destination)
    }
    componentWillUnmount(){
        console.log("unmount distance")
        this._isMounted = false;
    }
    render() {
        const { directions } = this.state;
        if (this.state.error) {
            return <h1>{this.state.error}</h1>;
        }
        return (<>
        {directions.map(
            (line,index) => {
              return(<Polyline
                key={index}
                path={line.polyline}
                geodesic={true}
                options={{
                    strokeColor: line.traffic > 2 ? trafficColor[2] : trafficColor[line.traffic],
                    strokeOpacity: 1,
                    strokeWeight: 3,
                }}
            />)}
          ) }</>);
        // return <DirectionsRenderer directions={this.state.directions}></DirectionsRenderer>;
    }
}

