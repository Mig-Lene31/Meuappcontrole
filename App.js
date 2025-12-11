import React, {useEffect} from 'react';
import { NavigationContainer } from '@react-navigation/native';
import StackRoutes from './src/navigation/StackRoutes';
import { navigationRef } from './src/navigation/RootNavigation';
import BlockManager from './src/services/blockManager';

export default function App() {
  useEffect(() => {
    // checar bloqueio no start
    BlockManager.init();
  }, []);

  return (
    <NavigationContainer ref={navigationRef}>
      <StackRoutes />
    </NavigationContainer>
  );
}
