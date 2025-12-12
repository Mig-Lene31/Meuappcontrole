import React, {useEffect, useState} from 'react';
import { View, Text, Button, StyleSheet, Alert } from 'react-native';
import { getBlockInfo, isBlocked } from '../services/blockEngine';

export default function HomeScreen({ navigation }) {
  const [blocked, setBlocked] = useState(false);
  const [info, setInfo] = useState({});

  useEffect(()=>{ refresh(); }, []);

  async function refresh(){
    setBlocked(await isBlocked());
    setInfo(await getBlockInfo());
  }

  return (
    <View style={s.container}>
      <Text style={s.title}>MeuAppControle — Painel</Text>

      <Text>Bloqueado agora: {blocked ? 'SIM' : 'NÃO'}</Text>
      {blocked && <Text>Motivo: {info.reason}</Text>}
      <View style={{height:12}}/>
      <Button title="Configurações" onPress={()=>navigation.navigate('Settings')} />
      <View style={{height:6}}/>
      <Button title="Atualizar" onPress={refresh} />
      <View style={{height:12}}/>
      <Text style={{fontSize:12,color:'#666'}}>Para o bloqueio automático funcionar ative o serviço de Acessibilidade nas Configurações do Android.</Text>
    </View>
  );
}

const s = StyleSheet.create({
  container:{flex:1,justifyContent:'center',padding:20},
  title:{fontSize:20, textAlign:'center', marginBottom:20},
});
