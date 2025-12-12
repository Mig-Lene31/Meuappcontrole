import React, {useState} from 'react';
import { View, Text, TextInput, Button, StyleSheet, Alert } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

const PREFS = 'meuapp_prefs_v1';

export default function AuthScreen({ navigation }) {
  const [login, setLogin] = useState('');
  const [pass, setPass] = useState('');

  async function signup() {
    if (!login || !pass) return Alert.alert('Preencha login e senha');
    const store = { user: { login, pass } };
    await AsyncStorage.mergeItem(PREFS, JSON.stringify(store));
    Alert.alert('Conta criada');
    navigation.replace('Home');
  }

  async function signin() {
    const raw = await AsyncStorage.getItem(PREFS);
    const prefs = raw ? JSON.parse(raw) : {};
    const u = prefs.user;
    if (u && u.login === login && u.pass === pass) {
      navigation.replace('Home');
    } else {
      Alert.alert('Login inválido');
    }
  }

  return (
    <View style={s.container}>
      <Text style={s.title}>MeuAppControle</Text>
      <TextInput placeholder="Login" value={login} onChangeText={setLogin} style={s.input}/>
      <TextInput placeholder="Senha" value={pass} onChangeText={setPass} secureTextEntry style={s.input}/>
      <View style={{height:10}}/>
      <Button title="Entrar" onPress={signin}/>
      <View style={{height:6}}/>
      <Button title="Cadastrar" onPress={signup}/>
    </View>
  );
}

const s = StyleSheet.create({
  container:{flex:1,justifyContent:'center',padding:20},
  title:{fontSize:22, textAlign:'center', marginBottom:20},
  input:{borderWidth:1,borderColor:'#ccc',padding:10,borderRadius:6,marginBottom:8}
});
