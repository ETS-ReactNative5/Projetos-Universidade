import React from 'react';

const UserContext = React.createContext({})
const SharedContext = React.createContext({
    sosVehicle: {
        destination: null,
        id: 0
    },
    updateSosVehicle: () => {},
});

export class SharedProvider extends React.Component {
    updateSosVehicle = newSosVehicle => {
        this.setState({ sosVehicle: newSosVehicle });
    };
  
    state = {
        sosVehicle: {
            destination: null,
            id: 0
        },
        updateSosVehicle: this.updateSosVehicle,
    };
  
    render() {
        return (
        <SharedContext.Provider value={this.state}>
            {this.props.children}
        </SharedContext.Provider>
        );
    }
}

export const SharedConsumer = SharedContext.Consumer;
export const UserProvider = UserContext.Provider
export const UserConsumer = UserContext.Consumer
export default UserContext;