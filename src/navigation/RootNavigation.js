import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';

import Home from '../screens/Home';
import ConfigStop from '../screens/ConfigStop';
import TempoUso from '../screens/TempoUso';
import IndicarInvestimentos from '../screens/IndicarInvestimentos';
import StatusBloqueio from '../screens/StatusBloqueio';

const Stack = createStackNavigator();

export default function RootNavigation() {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen name="Home" component={Home} />
        <Stack.Screen name="ConfigStop" component={ConfigStop} />
        <Stack.Screen name="TempoUso" component={TempoUso} />
        <Stack.Screen name="IndicarInvestimentos" component={IndicarInvestimentos} />
        <Stack.Screen name="StatusBloqueio" component={StatusBloqueio} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
