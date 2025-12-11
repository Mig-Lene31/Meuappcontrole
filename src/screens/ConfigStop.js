import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity } from 'react-native';

export default function ConfigStop() {
  const [valor, setValor] = useState('');

  return (
    <View style={{ flex:1, padding:24 }}>
      <Text style={{ fontSize:26, fontWeight:'bold' }}>Configurar Stop</Text>

      <Text style={{ marginTop:20 }}>
        Esta função será usada em versões futuras. Por enquanto, apenas informe um valor.
      </Text>

      <TextInput
        placeholder="Ex: 100"
        value={valor}
        onChangeText={setValor}
        style={{
          borderWidth:1, borderColor:'#ccc',
          padding:10, marginTop:20, borderRadius:8
        }}
      />

      <TouchableOpacity
        onPress={() => {}}
        style={{
          marginTop:20, backgroundColor:'black',
          padding:12, borderRadius:8
        }}>
        <Text style={{ color:'white', textAlign:'center' }}>
          Salvar (não funcional ainda)
        </Text>
      </TouchableOpacity>
    </View>
  );
}
