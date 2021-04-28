import React from 'react';
import PlacesAutocomplete from 'react-places-autocomplete';
import { FormGroup, Input, InputGroup, InputGroupAddon, InputGroupText } from "reactstrap";

export default class AutoSearchInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      address: ''
    }
  }

  handleChange = address => {
    this.setState({ address });
  };
  handleSelect = address => {
    this.setState({ address: '' });
    this.props.handleSelect(address);
  }
  render() {
    return (
      <PlacesAutocomplete
        value={this.state.address}
        onChange={this.handleChange}
        onSelect={this.handleSelect}
      >
        {({ getInputProps, suggestions, getSuggestionItemProps, loading }) => (
          <div>
            <FormGroup className="m-0">
                <InputGroup className="input-group-alternative">
                <InputGroupAddon addonType="prepend">
                    <InputGroupText>
                    <i className="ni ni-square-pin" />
                    </InputGroupText>
                </InputGroupAddon>
                <Input
                    {...getInputProps({
                        placeholder: 'Search Places ...',
                        className: 'form-control-alternative location-search-input ',
                    })}
                />
                </InputGroup>
            </FormGroup>
            <div className="autocomplete-dropdown-container" style={{zIndex:'1',position:'absolute',width:`84%`}}>
              {suggestions.map(suggestion => {
                const className = suggestion.active
                  ? 'suggestion-item--active'
                  : 'suggestion-item';
                // inline style for demonstration purpose
                const style = suggestion.active
                  ? { backgroundColor: '#fafafa', cursor: 'pointer' }
                  : { backgroundColor: '#ffffff', cursor: 'pointer' };
                return (
                  <div
                    {...getSuggestionItemProps(suggestion, {
                      className,
                      style,
                    })}
                  >
                    <strong>
                      {suggestion.formattedSuggestion.mainText}
                    </strong>{', '}
                    <small>
                      {suggestion.formattedSuggestion.secondaryText}
                    </small>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </PlacesAutocomplete>
    );
  }
}