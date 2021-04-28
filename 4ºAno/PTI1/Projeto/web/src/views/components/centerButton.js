import React, { Component } from 'react'
import CustomDrawingManagerControl from './mapControl.js';
import { Button } from "reactstrap";
export default class CenterButton extends Component {
    render() {
        return (
            <CustomDrawingManagerControl position={window.google.maps.ControlPosition.RIGHT_CENTER}>
                <Button style={{padding:7, fontSize:0}} onClick={this.props.centerOnUser}><i className="material-icons">my_location</i></Button>
            </CustomDrawingManagerControl>
        )
    }
}
