import React, { Component } from 'react';
import { Row, Col, Button, Collapse} from "reactstrap";

import AutoSearchInput from './addressSearch.js';

export default class DisplaySearch extends Component {
    render() {
        return (
            <div>
                <Collapse isOpen={this.props.isOpen}>
                    <Row>
                        <Col xl="6" lg="6" xs="8" className="mb-2">
                            <AutoSearchInput 
                            searchAddress={this.props.searchAddress}
                            handleSelect={this.props.handleSearch}
                            />
                        </Col>
                        <Col xl="3" lg="4" xs="4">
                            <Button disabled={!this.props.isSearchComplete} className="" 
                                    color="primary"
                                    onClick={this.props.calculateDirection}
                                    >
                                    <i className="mr-0 mr-sm-2 ni ni-bold-right d-sm-none" style={{fontSize: 'large'}}></i> <span id={2} className="d-none d-sm-inline ">Calcular Itinerário</span>       
                            </Button>
                        </Col>
                    </Row>
                </Collapse>
               
            </div>
        )
    }
}
