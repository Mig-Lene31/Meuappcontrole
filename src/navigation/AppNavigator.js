import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import AuthScreen from '../screens/AuthScreen';
import HomeScreen from '../screens/HomeScreen';
import SettingsScreen from '../screens/SettingsScreen';

const Stack = createStackNavigator();

export default function AppNavigator(){
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Auth" screenOptions={{headerShown:true}}>
        <Stack.Screen name="Auth" component={AuthScreen} options={{headerShown:false}}/>
        <Stack.Screen name="Home" component={HomeScreen}/>
        <Stack.Screen name="Settings" component={SettingsScreen}/>
      </Stack.Navigator>
    </NavigationContainer>
  );
}
