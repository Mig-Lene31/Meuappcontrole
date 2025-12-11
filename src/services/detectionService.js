import { NativeEventEmitter, NativeModules } from 'react-native';
import { navigate } from '../navigation/RootNavigation';
import AsyncStorage from '@react-native-async-storage/async-storage';

const { DetectionModule } = NativeModules;
const emitter = new NativeEventEmitter(DetectionModule);

let subscription = null;

export default {
  start() {
    if (!subscription) {
      subscription = emitter.addListener('OnDetected', async (domain) => {
        try {
          // grava último detectado para referência
          await AsyncStorage.setItem('last_detected_domain', domain);
          // navega para Home e passa o param para confirmação
          navigate('Home', { detected_domain: domain });
        } catch (e) {
          console.warn('detection onDetected error', e);
        }
      });
    }
  },
  stop() {
    if (subscription) {
      subscription.remove();
      subscription = null;
    }
  }
}
