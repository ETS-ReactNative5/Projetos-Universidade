    import React, { Component } from 'react'
import { Card, CardBody, Row, CardTitle, Col, Collapse} from "reactstrap";

export default class DisplayRouteInfo extends Component {
    state = {
        routeInfo : null,
        isOpen: false,
        vehicleObj: null,
        isVehicle: false,
    }
    updateInfo = () => {
        const routeInfo = JSON.parse(localStorage.getItem('routeInfo'));
        console.log(routeInfo);
        if(routeInfo)
            this.setState({
                routeInfo: routeInfo,
            })

    }
    componentDidMount() {
        this.timerUpdateRouteInfo = setInterval(this.updateInfo, 2000)
        this.updateInfo();
    }
    componentWillUnmount(){
        console.log("unmount route")
        clearInterval(this.timerUpdateRouteInfo);
    }
    componentDidUpdate(prevState){
        console.log("Update: ",prevState);
        const { vehicle, vehicleId } = this.props;
        console.log("now: "+vehicle+" prev: "+prevState.vehicle);
        if( this.props.vehicle !== prevState.vehicle)
            if(vehicle){
                
                const { vehicles } = JSON.parse(localStorage.getItem('infoUser'));
                const vehicleObj = vehicles.find(v => v.id === vehicleId);
                console.log("DidMount: ",vehicleObj) 
                this.setState({vehicleObj: vehicleObj, vehicle: this.props.vehicle});
            }
    }
    render() {
        const { routeInfo } = this.state;
        const { destination, vehicle } = this.props;
        console.log("RouteInfo: d- "+destination + " v- "+ vehicle);
        console.log(this.state)
        if (!routeInfo) return <></>;
        
        if(destination)
            return (
                <div className="header-body">
                {/* Card stats */}
                <div>
                    <Collapse isOpen={this.props.isOpen}>
                        <Row>
                            <Col lg="12" xl="12">
                                <Card className="card-stats mb-4 mb-xl-0">
                                <CardBody>
                                    <Row>
                                    <Col>
                                        <span className="h2 font-weight-bold mb-0">
                                        {routeInfo.address}
                                        </span>
                                        <CardTitle
                                        tag="h5"
                                        className="text-uppercase text-muted mb-0"
                                        >
                                        {}
                                        </CardTitle>
                                    </Col>
                                    <Col>
                                        <CardTitle
                                        tag="h5"
                                        className="text-uppercase text-muted mb-0"
                                        >
                                        Distância
                                        </CardTitle>
                                        <span className="h2 font-weight-bold mb-0">
                                        {(routeInfo.distance/1000).toFixed(1)}
                                        </span>
                                        <span className="h5 font-weight-bold text-muted mb-0"> km</span>
                                    </Col>
                                    <Col>
                                        <CardTitle
                                        tag="h5"
                                        className="text-uppercase text-muted mb-0"
                                        >
                                        Tempo de viagem
                                        </CardTitle>
                                        <span className="h2 font-weight-bold mb-0">
                                        {((routeInfo.duration+routeInfo.delay)/60).toFixed(0)}
                                        </span>
                                        <span className="h5 font-weight-bold text-muted mb-0"> min</span>
                                    </Col>
                                    </Row>
                                </CardBody>
                                </Card>
                            </Col>
                        </Row>
                    </Collapse>
                </div>
            </div>
            );
        
        console.log("vehicle: ", this.state.vehicleObj)
        if(vehicle)
            return(
                <div className="header-body">
                {/* Card stats */}
                <div>
                    <Collapse isOpen={this.props.isOpen}>
                        <Row>
                            <Col lg="12" xl="12">
                                <Card className="card-stats mb-4 mb-xl-0">
                                <CardBody>
                                    <Row>
                                    <Col>
                                        <span className="h2 font-weight-bold mb-0">
                                        {this.state.vehicleObj ? this.state.vehicleObj.name : ''}
                                        </span>
                                        <CardTitle
                                        tag="h5"
                                        className="text-uppercase text-muted mb-0"
                                        >
                                        {routeInfo.address}
                                        </CardTitle>
                                    </Col>
                                    <Col>
                                        <CardTitle
                                        tag="h5"
                                        className="text-uppercase text-muted mb-0"
                                        >
                                        Distância
                                        </CardTitle>
                                        <span className="h2 font-weight-bold mb-0">
                                        {(routeInfo.distance/1000).toFixed(1)}
                                        </span>
                                        <span className="h5 font-weight-bold text-muted mb-0"> km</span>
                                    </Col>
                                    <Col>
                                        <CardTitle
                                        tag="h5"
                                        className="text-uppercase text-muted mb-0"
                                        >
                                        Tempo de viagem
                                        </CardTitle>
                                        <span className="h2 font-weight-bold mb-0">
                                        {((routeInfo.duration+routeInfo.delay)/60).toFixed(0)}
                                        </span>
                                        <span className="h5 font-weight-bold text-muted mb-0"> min</span>
                                    </Col>
                                    </Row>
                                </CardBody>
                                </Card>
                            </Col>
                        </Row>
                    </Collapse>
                </div>
            </div>
            );
        
        return(<></>)
    }
}
