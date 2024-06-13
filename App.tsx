import React, {useEffect, useState} from 'react';
import {Button, FlatList, StyleSheet, Text, View} from 'react-native';

import BackgroundGeolocation from './modules/background-geolocation';
import {LatLang} from './modules/background-geolocation/types';

const App = () => {
  const [coords, setCoords] = useState<Array<LatLang>>([]);

  useEffect(() => {
    const subscription = BackgroundGeolocation.onLocation((latlng: LatLang) => {
      setCoords(cd => [...cd, latlng]);
    });

    return () => {
      subscription.remove();
    };
  }, []);

  const startTrackingLocationBg = async () => {
    await BackgroundGeolocation.start(10); //seconds
  };

  const stopTracking = async () => {
    await BackgroundGeolocation.stop();
  };

  return (
    <View style={styles.container}>
      <Button title="Start Tracking" onPress={startTrackingLocationBg} />
      <Button title="End Tracking" onPress={stopTracking} />
      <FlatList
        data={coords}
        renderItem={({item, index}) => {
          return <Text key={index}>{JSON.stringify(item)}</Text>;
        }}
      />
    </View>
  );
};

export default App;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    rowGap: 20,
    padding: 16,
  },
});
