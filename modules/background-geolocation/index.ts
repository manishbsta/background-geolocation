import {
  Alert,
  EmitterSubscription,
  Linking,
  NativeEventEmitter,
  NativeModules,
  PermissionsAndroid,
} from 'react-native';
import {LatLang, Status} from './types';

const {BackgroundGeolocation: BackgroundGeolocationModule, ModuleWithEmitter} =
  NativeModules;
const eventEmitter = new NativeEventEmitter(ModuleWithEmitter);

const BackgroundGeolocation = {
  start: async (interval: number = 10) => {
    const results = await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
    ]);

    if (
      results['android.permission.ACCESS_FINE_LOCATION'] === 'granted' ||
      results['android.permission.POST_NOTIFICATIONS'] === 'granted'
    ) {
      await BackgroundGeolocationModule.start(interval);
    } else {
      Alert.alert(
        'Permissions Denied',
        'You must provide the following permissions:\n1.Notification\n2.Location',
        [
          {text: 'Cancel'},
          {text: 'Allow', onPress: () => Linking.openSettings},
        ],
      );
    }
  },
  stop: async () => {
    await BackgroundGeolocationModule.stop();
  },
  onLocation: (onLocation: (latLng: LatLang) => EmitterSubscription) => {
    const subscription = eventEmitter.addListener('onLocation', onLocation);
    return subscription;
  },
  isGpsEnabled: async (): Promise<Status> => {
    return BackgroundGeolocationModule.isGpsEnabled();
  },
  isRunning: async (): Promise<Status> => {
    return BackgroundGeolocationModule.isRunning();
  },
};

export default BackgroundGeolocation;
