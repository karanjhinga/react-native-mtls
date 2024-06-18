#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(Mtls, NSObject)

RCT_EXPORT_METHOD(makeRequest:(NSString *)path
                  method:(NSString *)method
                  headers:(NSDictionary *)headers
                  params:(NSDictionary *)params
                  body:(NSDictionary *)body
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
