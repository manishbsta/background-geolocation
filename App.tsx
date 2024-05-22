import React, {useEffect, useState} from 'react';
import {
  Button,
  FlatList,
  NativeEventEmitter,
  NativeModules,
  PermissionsAndroid,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import {LatLang} from './modules/BackgroundGeolocation/types';

const {BackgroundGeolocation, ModuleWithEmitter} = NativeModules;
const eventEmitter = new NativeEventEmitter(ModuleWithEmitter);

const App = () => {
  const [coords, setCoords] = useState<Array<LatLang>>([]);

  useEffect(() => {
    const onLocation = (latlng: LatLang) => {
      setCoords(cd => [...cd, latlng]);
    };

    const subscription = eventEmitter.addListener('onLocation', onLocation);

    return () => {
      subscription.remove();
    };
  }, []);

  const startTrackingLocationBg = async () => {
    await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
    ]);

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
