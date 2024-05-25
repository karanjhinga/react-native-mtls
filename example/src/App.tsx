import * as React from 'react';

import { StyleSheet, View, Button } from 'react-native';
import { makeRequest, multipart, setup } from 'react-native-mtls';
import { launchCamera } from 'react-native-image-picker';
import { CompressedImage, resizeImage } from './file-utils';

const pk =
  'MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCpbY7IuWAGOAeqyxfAlGqczpIww5qExB2zmJkbxyl3Zy8KGHHyuF+G3RXqb33BSPgj6FEkNHXFEkZ0Ri7QM5AjXAw8etnw4ljsJB/jmu1hN2zvxkNT0oKXvPDboXz8IQLOGOWiaFylr0xMWV4vLnWhrdSqC7W1PoF0cCmYewaprZORrwbkmPrVb+vr/XfhIIms5KGx38SKiJdaRqzqSSNcQyEBPjxPOuuIBBYB2bEPZGr/MxK1ct1Z8XpWKs0UYnvdzv9pSIj7/35JbIOyVLzN5cjbg5aG9s7NQbbEDunZkm9Wl/bLmVPBZTzmxZsX7q8qhi2MppZYxMkMpaJlVplpAgMBAAECggEAFLHE8lPBLLVIi3YfzjYaoRFStinYLG4LbjG+BtBwak0UR+yFM4dNZLrJp99LGhIce/9O9OgC+AmjySU+58+ahBUkyfjb+LIe+CXv7rQwWTLkx5M9C0afzKKnfT4n9uBd2DheiRQr99FV9Y/nYwYu2EsUGhqQzOTn+UXPcNhbD5uo9GNdjoIpQyVt/bCDyvSOMHtFqtGYuKC2rEJeV6m6nphZ9ilI6SSEbUN+aCZXdsEqesbfd6Kclk+jcrC/p7bLO55fgZNOpsCmjYTdqmaFIegrr+V+SgKV32LLMlD0d2lAQdjx+A1FsquJuLwIx896mmPtGk2HmMptiVun3jxaIQKBgQDTNtKmW16JkJyXddXmCKmxcT7anLg1Fv9TXWpdfIcgUIFCSHuyu/+EvYQnspBYu6W9WxGV/C0jjKU+1hUaUrHg9NtufDeFJKIrZ5PoWvpiXY6INWSmf6QzTVULeY9uFe3x8iq4F/862kVYFuPf6mORA48Y83cxt5iCrjTvQ6vriQKBgQDNWnyhE6Yi29nxBrz/zK0gf/F0bRScUf6l/zOrdrGyYXZGNdLlqlMDToB4F9ksepCll4TrNk4+1+VnHX+gbMqOFKEh4iObwTlwmkjJ3UX9Fw200u+OeddGOoR4T4CIpQjDaRTYPHFduQVozo/38Bas7exIg/IiNTKtL2MCQ0Rm4QKBgCiDnJZyckja516aKMRuJva1bUQLyb3NQn6gLZXvHBBTwOeQ9cFMFHBG1gGzW8LZg6o1vMLTU2k9QjkyYWviLuKitTCVPSUZ6M2Ambt5MAwMPBnefAb/9uQsUkLYN237YOAG/rC/UaLdWW3TPjSmRiD72MgFc3ii6esvNVIr3d9ZAoGAb/Wcq2l+E9VPvrRQyRo7bdOzvilWql+d+bzo29wLx9iRVngz8plpRw0+x7sg4bo1MMMqad8iy+qDnTOdCMcnrE0dCM81YM75VSYY904XwYQXKfYzO7e8IgL3hGy8BEQOQhQBlF9n45RjmZ+9KI2Sn1fckjC2C+Z7KacDURGBsqECgYAdq25upuMoGV1M3kUw0bc6do53t5j5p4DXcJ1AFxlYE+XEiD64mpbQjVBsX6eJ2KKFnpejbfvF03wllT9jKdnfs++son0QQgunJ+Lkysjna+QCZCqWJ5iKS4WSWSCBRM5VgxzVn7Y/1bKBo4XHLh2xei1Xv5HNSf10O/5wTckZ9A==';

export default function App() {
  const [init, setInit] = React.useState(false);

  const invokeApi = React.useCallback(async (image: CompressedImage) => {
    const headers = {
      'app-version': '1.2405.5',
      'Authorization':
        'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI4MDU5Nzg1MDczIiwiaWQiOjQsImF1dGhUeXBlIjoibW9iaWxlIiwiZXhwIjoxNzE5MjI0MDM3LCJpYXQiOjE3MTY2MzIwMzcsImVudGl0eSI6InVzZXIifQ.RG9R1EBYQNLd9GQ7CylhtFv5fKjVplEuSlD2iuXB_4w',
    };

    const data = new FormData();
    console.log('imageUri =', image.uri);
    data.append('image', {
      uri: image.uri,
      type: 'image/*',
      name: 'img.png',
      fileName: 'img.png',
    });

    multipart(
      'v1/user/profile-picture',
      headers,
      { a: 'b' },
      {},
      'image',
      image.uri,
      {
        type: 'image/*',
        name: `hello.png`,
      }
    )
      .then((res) => {
        console.log('response=>', res);
      })
      .catch((e) => console.log('rejected', e));
  }, []);

  const pickImageFrom = React.useCallback(async () => {
    if (!init) {
      console.log('return ');
    }
    launchCamera({ mediaType: 'photo' }).then(async (val) => {
      console.log(val.assets[0]);
      invokeApi(await resizeImage(val.assets[0]));
    });
  }, [init, invokeApi]);

  const setupApi = React.useCallback(async () => {
    const headers = new Map();
    headers.set('app-version', '1.2405.5');
    setup(pk, 'https:/stage.traveleva.in').then(() => {
      setInit(true);

      makeRequest('v1/home/2', 'GET', headers, {}, {})
        .then((res) => {
          console.log('response=>', res);
        })
        .catch((e) => console.log('rejected', e));
    });
  }, []);

  React.useEffect(() => {
    setupApi();
  }, [setupApi]);

  return (
    <View style={styles.container}>
      <Button title="pick" onPress={pickImageFrom} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
