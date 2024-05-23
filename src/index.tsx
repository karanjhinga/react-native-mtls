import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-mtls' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const Mtls = NativeModules.Mtls
  ? NativeModules.Mtls
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function setup(privateKey: string, baseUrl: string): Promise<boolean> {
  return Mtls.setup(privateKey, baseUrl);
}

export interface MtlsResponse {
  kind: 'ok' | 'cannot-connect' | 'unknown';
  status?: number;
  body?: string;
}

export function makeRequest(
  path: string,
  method: string,
  headers: { [key: string]: any },
  params: { [key: string]: any },
  body: { [key: string]: any }
): MtlsResponse {
  return Mtls.makeRequest(path, method, headers, params, body);
}
