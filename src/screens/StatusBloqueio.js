import React, { useEffect, useState } from 'react';
import { View, Text } from 'react-native';
import BlockManager from '../services/blockManager';

export default function StatusBloqueio() {
  const [message, setMessage] = useState('Verificando...');

  useEffect(() => {
    let timer = null;
    (async () => {
      const until = await BlockManager.getBlockedUntil();
      if (!until) {
        setMessage('Nenhum bloqueio ativo.');
        return;
      }
      const update = () => {
        const diff = until.getTime() - Date.now();
        if (diff <= 0) {
          setMessage('Nenhum bloqueio ativo.');
          if (timer) clearInterval(timer);
          return;
        }
        const hrs = Math.floor(diff / (3600*1000));
        const mins = Math.floor((diff % (3600*1000)) / (60*1000));
        setMessage('Bloqueado — restante: ' + hrs + 'h ' + mins + 'm');
      };
      update();
      timer = setInterval(update, 60*1000);
    })();
    return () => timer && clearInterval(timer);
  }, []);

  return (
    <View style={{ flex:1, padding:24 }}>
      <Text style={{ fontSize:26, fontWeight:'bold' }}>Status do Bloqueio</Text>
      <Text style={{ marginTop:20 }}>{message}</Text>
    </View>
  );
}
