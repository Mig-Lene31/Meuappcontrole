import { NativeModules, Platform } from 'react-native';
const { /* se tiver NativeModule nativo */ } = NativeModules;

const PREFS_KEY = 'meuapp_prefs';

// funções de persistência simples (usando AsyncStorage)
import AsyncStorage from '@react-native-async-storage/async-storage';

export async function setBlockedHosts(hosts = []) {
  const prefs = await AsyncStorage.getItem(PREFS_KEY) || '{}';
  const obj = JSON.parse(prefs || '{}');
  obj.blocked_hosts = hosts;
  await AsyncStorage.setItem(PREFS_KEY, JSON.stringify(obj));
}

export async function getBlockedHosts() {
  const prefs = await AsyncStorage.getItem(PREFS_KEY) || '{}';
  const obj = JSON.parse(prefs || '{}');
  return obj.blocked_hosts || [];
}

// regras: stopWin, stopLoss, dailyTime (em segundos)
export async function saveRules(rules) {
  const prefs = await AsyncStorage.getItem(PREFS_KEY) || '{}';
  const obj = JSON.parse(prefs || '{}');
  obj.rules = rules;
  await AsyncStorage.setItem(PREFS_KEY, JSON.stringify(obj));
}

export async function getRules() {
  const prefs = await AsyncStorage.getItem(PREFS_KEY) || '{}';
  const obj = JSON.parse(prefs || '{}');
  return obj.rules || { stopWin: null, stopLoss: null, dailySeconds: null };
}

// chamada quando regras atingidas: grava estado de bloqueio (timestamp fim)
export async function triggerBlock(reason) {
  const prefs = await AsyncStorage.getItem(PREFS_KEY) || '{}';
  const obj = JSON.parse(prefs || '{}');
  const end = Date.now() + (12 * 60 * 60 * 1000); // 12h
  obj.block_until = end;
  obj.block_reason = reason;
  await AsyncStorage.setItem(PREFS_KEY, JSON.stringify(obj));
  // também atualiza blocked_hosts se necessário
}

// helper para checar se está bloqueado
export async function isBlocked() {
  const prefs = await AsyncStorage.getItem(PREFS_KEY) || '{}';
  const obj = JSON.parse(prefs || '{}');
  return (obj.block_until && obj.block_until > Date.now()) || false;
}
