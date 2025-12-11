/**
 * blockEngine.js
 * Motor JS: persistência de regras, checagens e trigger de bloqueio
 *
 * Uso: importar e chamar as funções a partir do app React Native.
 */

import AsyncStorage from '@react-native-async-storage/async-storage';

const PREFS_KEY = 'meuapp_prefs_v1';

// helpers de armazenamento
async function readPrefs() {
  const raw = await AsyncStorage.getItem(PREFS_KEY);
  return raw ? JSON.parse(raw) : {};
}
async function writePrefs(obj) {
  await AsyncStorage.setItem(PREFS_KEY, JSON.stringify(obj || {}));
}

// regras: { deposit: number, stopWin: number, stopLoss: number, dailySeconds: number }
export async function saveRules(rules = {}) {
  const prefs = await readPrefs();
  prefs.rules = {
    deposit: rules.deposit ?? prefs.rules?.deposit ?? null,
    stopWin: rules.stopWin ?? prefs.rules?.stopWin ?? null,
    stopLoss: rules.stopLoss ?? prefs.rules?.stopLoss ?? null,
    dailySeconds: rules.dailySeconds ?? prefs.rules?.dailySeconds ?? null,
  };
  await writePrefs(prefs);
}

export async function getRules() {
  const prefs = await readPrefs();
  return prefs.rules || { deposit: null, stopWin: null, stopLoss: null, dailySeconds: null };
}

// blocked hosts list
export async function setBlockedHosts(hosts = []) {
  const prefs = await readPrefs();
  prefs.blocked_hosts = Array.from(new Set(hosts.map(h => h.toLowerCase())));
  await writePrefs(prefs);
}

export async function getBlockedHosts() {
  const prefs = await readPrefs();
  return prefs.blocked_hosts || [];
}

// guarda estado de bloqueio: block_until timestamp ms e reason
export async function triggerBlock(reason) {
  const prefs = await readPrefs();
  const end = Date.now() + 12 * 60 * 60 * 1000; // 12h
  prefs.block_until = end;
  prefs.block_reason = reason;
  await writePrefs(prefs);
  return end;
}

export async function isBlocked() {
  const prefs = await readPrefs();
  return !!(prefs.block_until && prefs.block_until > Date.now());
}

export async function getBlockInfo() {
  const prefs = await readPrefs();
  return { until: prefs.block_until || null, reason: prefs.block_reason || null };
}

/**
 * checkIfShouldBlock(balance)
 * Lógica que decide, a partir de saldo atual e regras salvas, se deve bloquear:
 * - compara com deposit + stopWin/stopLoss
 * - retorna { shouldBlock: bool, reason: string|null }
 */
export async function checkIfShouldBlock(currentBalance) {
  const rules = await getRules();
  if (!rules) return { shouldBlock: false, reason: null };

  const deposit = Number(rules.deposit) || 0;
  const stopWin = Number(rules.stopWin) || null;
  const stopLoss = Number(rules.stopLoss) || null;
  const result = { shouldBlock: false, reason: null };

  // calcular ganho/perda relativo ao depósito se disponível
  if (deposit > 0 && currentBalance != null) {
    const delta = currentBalance - deposit;
    if (stopWin !== null && delta >= stopWin) {
      result.shouldBlock = true;
      result.reason = `stopWin alcanzado (dep:${deposit} bal:${currentBalance} ganho:${delta})`;
      return result;
    }
    if (stopLoss !== null && -delta >= stopLoss) {
      result.shouldBlock = true;
      result.reason = `stopLoss alcanzado (dep:${deposit} bal:${currentBalance} perda:${-delta})`;
      return result;
    }
  } else {
    // se sem depósito, interpreta stopWin/stopLoss diretamente como saldo
    if (stopWin !== null && currentBalance >= stopWin) {
      result.shouldBlock = true;
      result.reason = `stopWin por saldo (bal:${currentBalance})`;
      return result;
    }
    if (stopLoss !== null && currentBalance <= stopLoss) {
      result.shouldBlock = true;
      result.reason = `stopLoss por saldo (bal:${currentBalance})`;
      return result;
    }
  }
  return result;
}

/**
 * Filtro de "valor confiável" — tenta sanitizar string e extrair número.
 * Retorna null se achar que o valor é de anúncio/ganhador (deve ser ignorado).
 *
 * Regras aplicadas:
 *  - desaparece se texto contém palavras do tipo "ganhou", "winner", "jackpot", ...
 *  - rejeita valores com formatos estranhos (multiplicadores, "x", "%", "pts")
 */
const AD_KEYWORDS = [
  'ganhou','ganhador','ganhadores','winner','winners','jackpot','premio','prêmio',
  'últimos','ultimos','vencedor','vencedores','multiplicador','x','vezes','vez'
];

export function sanitizeAndFilterText(text) {
  if (!text || typeof text !== 'string') return null;
  const low = text.toLowerCase();

  // se contém palavra de anúncio -> ignorar
  for (const k of AD_KEYWORDS) {
    if (low.includes(k)) return null;
  }

  // extrair primeiro número plausível com decimal/sep
  const numMatch = low.replace(/[^\d.,]/g, ' ').match(/(\d+[.,]?\d{0,2})/);
  if (!numMatch) return null;
  let v = numMatch[1].replace(',', '.');
  const val = parseFloat(v);
  if (isNaN(val)) return null;

  // descarta se valor muito grande (provavel anúncio) - heurística (>= 1e6)
  if (val >= 1_000_000) return null;

  return val;
}

/**
 * frequency guard: mantém histórico curto de leituras para detectar "banners" / updates rápidos
 * Ex: se 4 leituras diferentes em < 2s -> provavelmente animação/lista -> ignora
 */
const history = [];
export function updateAndCheckFrequency(key, value) {
  const now = Date.now();
  history.push({ key, value, t: now });
  // manter 6 seg window
  while (history.length && (now - history[0].t) > 6000) history.shift();

  // contar leituras distintas para a mesma key
  const items = history.filter(h => h.key === key);
  const distinct = new Set(items.map(it => `${it.value}`)).size;
  // se mais de 3 valores distintos em 6s, considerar animação => ignora
  return distinct <= 3;
}

export default {
  saveRules, getRules,
  setBlockedHosts, getBlockedHosts,
  triggerBlock, isBlocked, getBlockInfo,
  checkIfShouldBlock,
  sanitizeAndFilterText, updateAndCheckFrequency
};
