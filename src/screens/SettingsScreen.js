import React, {useState, useEffect} from 'react';
import { View, Text, TextInput, Button, StyleSheet, Switch, Alert } from 'react-native';
import { getRules, saveRules, getBlockedHosts, setBlockedHosts } from '../services/blockEngine';

export default function SettingsScreen(){
  const [deposit, setDeposit] = useState('');
  const [stopWin, setStopWin] = useState('');
  const [stopLoss, setStopLoss] = useState('');
  const [dailySeconds, setDailySeconds] = useState('');
  const [hosts, setHosts] = useState('');
  const [ignoreAds, setIgnoreAds] = useState(true);

  useEffect(async ()=>{
    const r = await getRules();
    setDeposit(r.deposit ? String(r.deposit) : '');
    setStopWin(r.stopWin ? String(r.stopWin) : '');
    setStopLoss(r.stopLoss ? String(r.stopLoss) : '');
    setDailySeconds(r.dailySeconds ? String(r.dailySeconds) : '');
    const h = await getBlockedHosts(); setHosts(h.join(','));
  },[]);

  async function save(){
    await saveRules({
      deposit: deposit ? Number(deposit) : null,
      stopWin: stopWin ? Number(stopWin) : null,
      stopLoss: stopLoss ? Number(stopLoss) : null,
      dailySeconds: dailySeconds ? Number(dailySeconds) : null
    });
    await setBlockedHosts(hosts.split(',').map(s=>s.trim()).filter(Boolean));
    Alert.alert('Configurado');
  }

  return (
    <View style={s.container}>
      <Text style={s.title}>Configurações</Text>
      <Text>Depósito inicial (R$)</Text>
      <TextInput keyboardType="numeric" style={s.input} value={deposit} onChangeText={setDeposit}/>
      <Text>Stop Win (R$)</Text>
      <TextInput keyboardType="numeric" style={s.input} value={stopWin} onChangeText={setStopWin}/>
      <Text>Stop Loss (R$)</Text>
      <TextInput keyboardType="numeric" style={s.input} value={stopLoss} onChangeText={setStopLoss}/>
      <Text>Tempo diário permitido (segundos)</Text>
      <TextInput keyboardType="numeric" style={s.input} value={dailySeconds} onChangeText={setDailySeconds}/>
      <Text>Sites bloqueados (vírgula separado)</Text>
      <TextInput style={s.input} value={hosts} onChangeText={setHosts}/>
      <View style={{flexDirection:'row',alignItems:'center',marginVertical:8}}>
        <Switch value={ignoreAds} onValueChange={setIgnoreAds}/>
        <Text style={{marginLeft:8}}>Ignorar banners/ganhadores</Text>
      </View>
      <Button title="Salvar" onPress={save}/>
    </View>
  );
}

const s = StyleSheet.create({
  container:{flex:1,padding:20},
  title:{fontSize:18,textAlign:'center',marginBottom:12},
  input:{borderWidth:1,borderColor:'#ccc',padding:8,borderRadius:6,marginBottom:8}
});
