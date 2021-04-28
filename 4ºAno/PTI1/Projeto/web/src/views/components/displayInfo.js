import React, { Component } from 'react'
import { Card, CardBody, Row, CardTitle, Col, Collapse} from "reactstrap";
import { speedLimits } from '../../utils/icons.js';

const traffic = ['Leve', 'Moderado', 'Congestionado'];

export default class DisplayInfo extends Component {
    state = {
        infoUser : null,
        isOpen: false,
    }
    updateInfo = () => {
        const infoUser = JSON.parse(localStorage.getItem('infoUser'));
        //console.log("updateInfo",infoUser.speed)
        this.setState({
            infoUser: infoUser,
        })

    }
    componentDidMount() {
        this.timerUpdateInfo = setInterval(this.updateInfo, 2000)
        this.updateInfo();
    }
    componentWillUnmount() {
        clearInterval(this.timerUpdateInfo);
    }

    // toggleInfo = () => this.setState({ isOpen: !this.state.isOpen });

    render() {
        const { infoUser } = this.state;
        
        if (!infoUser) return <></>;
        const isOffline = (Date.now()/1000 - infoUser.lastTimestamp > 60);
        console.log(isOffline)
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
                                    {infoUser.routeName}
                                    </span>
                                    <CardTitle
                                    tag="h5"
                                    className="text-uppercase text-muted mb-0"
                                    >
                                    {infoUser.localityName}
                                    </CardTitle>
                                </Col>
                                <Col>
                                {isOffline ? (
                                    <>
                                        <CardTitle
                                            tag="h5"
                                            className="text-uppercase text-muted mb-0"
                                            >
                                            Inativo à
                                        </CardTitle>
                                        <span className="h2 font-weight-bold mb-0">
                                            {parseInt((Date.now()/1000 - infoUser.lastTimestamp)/60)}
                                        </span>
                                        <span className="h5 font-weight-bold text-muted mb-0">
                                            {" minutos"}
                                        </span>
                                    </>
                                ):(
                                    <Row>
                                        <Col className="col-6">
                                            <CardTitle
                                                tag="h5"
                                                className="text-uppercase text-muted mb-0"
                                                >
                                                Velocidade
                                            </CardTitle>
                                            <span className="h2 font-weight-bold mb-0">
                                                {infoUser.speed}
                                            </span>
                                            <span className="h5 font-weight-bold text-muted mb-0">
                                                {" Km/h"}
                                            </span>
                                        </Col>
                                        <Col className="col-6">
                                            <img width='36%' src={ speedLimits[parseInt(infoUser.speedLimit/10) -1]} alt="SL" />
                                        </Col>
                                    </Row>
                                )}
                                    
                                </Col>
                                <Col>
                                    <CardTitle
                                    tag="h5"
                                    className="text-uppercase text-muted mb-0"
                                    >
                                    Trânsito
                                    </CardTitle>
                                    <span className="h2 font-weight-bold mb-0">
                                    {infoUser.traffic > 2 ? traffic[2]: traffic[infoUser.traffic]}
                                    </span>
                                </Col>
                                </Row>
                            </CardBody>
                            </Card>
                        </Col>
                    </Row>
                </Collapse>
            </div>
          </div>
        )
    }
}
