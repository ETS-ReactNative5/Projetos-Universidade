import React, { Component } from 'react'
import { Dropdown, DropdownToggle, DropdownMenu, DropdownItem } from 'reactstrap';

const API = "http://35.246.98.225:8000/sos";

export default class SOSButtons extends Component {
    state = {
        dropdownOpen: false,
        disabled: true,
    }

    toggle = () => this.setState({dropdownOpen: !this.state.dropdownOpen});

    handleSOS = (e) => {
        console.log("SOS: "+this.props.userId+" SOS type: "+e.target.id)

        let data = {
          sos: e.target.id
        }
        this.setState({
          disabled: !this.state.disabled,
        });
        const putMethod = {
          method: 'PUT',
          headers: {
           'Content-type': 'application/json; charset=UTF-8'
          },
          body: JSON.stringify(data) // We send data in JSON format
         };
         
         fetch(API+'/'+this.props.userId, putMethod)
         .then(response => response.json())
         .then(data => console.log(data)) // Manipulate the data retrieved back, if we want to do something with it
         .catch(err => console.log(err)) // Do something with the error
    }
    render() {
      const { disabled } = this.state;
        return (
            <Dropdown className="float-right" isOpen={this.state.dropdownOpen} toggle={this.toggle}>
              <DropdownToggle caret color="primary">
                Pedido SOS
                </DropdownToggle>
              <DropdownMenu right>
                <DropdownItem id={0} onClick={this.handleSOS} disabled={disabled}>Cancelar</DropdownItem>
                <DropdownItem divider/>
                <DropdownItem id={5} onClick={this.handleSOS}>Ambulância</DropdownItem>
                <DropdownItem id={6} onClick={this.handleSOS}>Polícia</DropdownItem>
                <DropdownItem id={7} onClick={this.handleSOS}>Reboque</DropdownItem>
              </DropdownMenu>
            </Dropdown>
          );
    }
}
