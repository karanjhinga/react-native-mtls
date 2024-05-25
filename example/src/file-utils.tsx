import RNFetchBlob from 'react-native-blob-util';
import { Asset } from 'react-native-image-picker';
const fs = RNFetchBlob.fs;
const IMAGES_DIR = '/img/compression_cache';
const UPLOADS_DIR = '/img/pending_uploads';

export interface CompressedImage {
  uri: string;
  path: string;
  fileName: string;
  fileSize: number;
}

const resizeImage = async (image: Asset): Promise<CompressedImage> => {
  const finalDir = fs.dirs.CacheDir + UPLOADS_DIR;
  const cacheDir = fs.dirs.CacheDir + IMAGES_DIR;

  if (await fs.exists(cacheDir)) {
    fs.unlink(cacheDir);
  }

  if (!(await fs.exists(finalDir))) {
    fs.mkdir(finalDir);
  }

  await fs.mkdir(cacheDir);

  const path = finalDir + '/' + image.fileName;

  await fs.cp(image.uri!!, path);

  return {
    uri: 'file://' + path,
    fileSize: image.fileSize,
    fileName: image.fileName,
    path,
  };
};

export const ext = (url: string) => {
  return (url = url.substr(1 + url.lastIndexOf('/')).split('?')[0])
    .split('#')[0]
    .substr(url.lastIndexOf('.'));
};

export { resizeImage };
