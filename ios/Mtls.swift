@objc(Mtls)
class Mtls: NSObject {

  @objc func makeRequest(path:String,method:String,headers:NSDictionary,params:NSDictionary,body:NSDictionary,resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
      NSLog("make request called")
  }

  @objc func setup(privateKey:String,baseUrl:String,resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
      NSLog("setup called")
  }
}
