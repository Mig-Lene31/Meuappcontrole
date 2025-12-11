import React from 'react';
import { View, Text, Linking, TouchableOpacity } from 'react-native';

export default function IndicarInvestimentos() {
  return (
    <View style={{ flex:1, padding:24 }}>
      <Text style={{ fontSize:26, fontWeight:'bold' }}>Indicações</Text>

      <Text style={{ marginTop:20 }}>
        Aqui estarão sugestões de conteúdos seguros.
      </Text>

      <TouchableOpacity
        onPress={() => Linking.openURL("https://www.tesourodireto.com.br")}
        style={{ marginTop:20 }}
      >
        <Text style={{ color:'blue' }}>Tesouro Direto</Text>
      </TouchableOpacity>
    </View>
  );
}
