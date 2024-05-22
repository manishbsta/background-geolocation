import {NativeEventEmitter, NativeModules} from 'react-native';
const {ModuleWithEmitter} = NativeModules;

const eventEmitter = new NativeEventEmitter(ModuleWithEmitter);

const onSessionConnect = (event: any) => {
  console.log(event);
};

export const subscription = eventEmitter.addListener(
  'onLocation',
  onSessionConnect,
);
