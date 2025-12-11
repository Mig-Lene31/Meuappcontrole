import AsyncStorage from '@react-native-async-storage/async-storage';
import { NativeModules, Alert } from 'react-native';
import detectionService from './detectionService';

const { DetectionModule } = NativeModules;
const BLOCK_KEY = 'blocked_until';
const LAST_DETECTED = 'last_detected_domain';

async function nowMs(){ return Date.now(); }

const BlockManager = {
  async init() {
    // start listening native detections
    detectionService.start();
    // checar se já está bloqueado e, se sim, garantir comportamento
    const until = await AsyncStorage.getItem(BLOCK_KEY);
    if (until) {
      const ts = Number(until);
      if (Date.now() < ts) {
        // já bloqueado — garantir chamadas nativas se necessário
        // (pode reativar VPN se o app reiniciou)
        if (DetectionModule && DetectionModule.startBlocking) {
          // calcula horas restantes arredondadas pra cima
          const hrs = Math.ceil((ts - Date.now()) / (3600*1000));
          try { DetectionModule.startBlocking(hrs); } catch(e){}
        }
      } else {
        await AsyncStorage.removeItem(BLOCK_KEY);
      }
    }
  },

  async requestConfirmAndBlock(domain, navigation) {
    // mostra confirmação nativa (ou use navigation para modal)
    return new Promise(async (resolve) => {
      Alert.alert(
        'Confirmação de bloqueio',
        'Detectei o domínio: ' + domain + '\\nDeseja confirmar bloqueio por 12 horas?',
        [
          { text: 'Cancelar', style: 'cancel', onPress: () => resolve(false) },
          { text: 'Confirmar', onPress: async () => {
              await BlockManager.activateBlock(12);
              resolve(true);
          }}
        ],
        { cancelable: false }
      );
    });
  },

  async activateBlock(hours = 12) {
    const until = Date.now() + hours * 3600 * 1000;
    await AsyncStorage.setItem(BLOCK_KEY, String(until));
    // chama módulo nativo para iniciar VPN/block
    try {
      if (DetectionModule && DetectionModule.startBlocking) {
        DetectionModule.startBlocking(hours);
      }
    } catch (e) {
      console.warn('startBlocking native error', e);
    }
  },

  async isBlocked() {
    const v = await AsyncStorage.getItem(BLOCK_KEY);
    if (!v) return false;
    return Date.now() < Number(v);
  },

  async getBlockedUntil() {
    const v = await AsyncStorage.getItem(BLOCK_KEY);
    if (!v) return null;
    return new Date(Number(v));
  },

  async getLastDetected() {
    return await AsyncStorage.getItem(LAST_DETECTED);
  }
};

export default BlockManager;
