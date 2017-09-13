#import "VuforiaPlugin.h"
#import "ViewController.h"

@interface VuforiaPlugin()

@property CDVInvokedUrlCommand *command;
@property ViewController *imageRecViewController;
@property BOOL startedVuforia;
@property BOOL autostopOnImageFound; // TODO: remove
@property NSDictionary* previousMarkers;

@end

@implementation VuforiaPlugin

- (void) cordovaStartVuforia:(CDVInvokedUrlCommand *)command {

    NSLog(@"Vuforia Plugin :: Start plugin");

    NSLog(@"Arguments: %@", command.arguments);
    NSLog(@"KEY: %@", [command.arguments objectAtIndex:3]);

    NSString *overlayText = ([command.arguments objectAtIndex:2] == (id)[NSNull null]) ? @"" : [command.arguments objectAtIndex:2]; // TODO: remove

    NSDictionary *overlayOptions =  [[NSDictionary alloc] initWithObjectsAndKeys: overlayText, @"overlayText", [NSNumber numberWithBool:[[command.arguments objectAtIndex:5] integerValue]], @"showDevicesIcon", nil]; // TODO: remove

    self.autostopOnImageFound = [[command.arguments objectAtIndex:6] integerValue]; // TODO: remove

    self.previousMarkers = [[NSDictionary alloc] init]; // used to check if recognized marker set updated between frames

    [self startVuforiaWithImageTargetFile:[command.arguments objectAtIndex:0] imageTargetNames: [command.arguments objectAtIndex:1] overlayOptions: overlayOptions vuforiaLicenseKey: [command.arguments objectAtIndex:3]];
    self.command = command;

    self.startedVuforia = true;
}

- (void) cordovaStopVuforia:(CDVInvokedUrlCommand *)command {
    self.command = command;

    NSDictionary *jsonObj = [NSDictionary alloc];

    if(self.startedVuforia == true){
        NSLog(@"Vuforia Plugin :: Stopping plugin");

        jsonObj = [ [NSDictionary alloc] initWithObjectsAndKeys :
                   @"true", @"success",
                   nil
                   ];
    }else{
        NSLog(@"Vuforia Plugin :: Cannot stop the plugin because it wasn't started");

        jsonObj = [ [NSDictionary alloc] initWithObjectsAndKeys :
                   @"false", @"success",
                   @"No Vuforia session running", @"message",
                   nil
                   ];
    }

    CDVPluginResult *pluginResult = [ CDVPluginResult
                                     resultWithStatus    : CDVCommandStatus_OK
                                     messageAsDictionary : jsonObj
                                     ];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];

    [self VP_closeView];
}

- (void) cordovaStopTrackers:(CDVInvokedUrlCommand *)command{
    bool result = [self.imageRecViewController stopTrackers];

    [self handleResultMessage:result command:command];
}

- (void) cordovaStartTrackers:(CDVInvokedUrlCommand *)command{
    bool result = [self.imageRecViewController startTrackers];

    [self handleResultMessage:result command:command];
}

- (void) cordovaUpdateTargets:(CDVInvokedUrlCommand *)command{
    NSArray *targets = [command.arguments objectAtIndex:0];

    // We need to ensure our targets are flatened, if we pass an array of items it'll crash if we dont
    NSMutableArray *flattenedTargets = [[NSMutableArray alloc] init];
    for (int i = 0; i < targets.count ; i++)
    {
        if([[targets objectAtIndex:i] respondsToSelector:@selector(count)]) {
            [flattenedTargets addObjectsFromArray:[targets objectAtIndex:i]];
        } else {
            [flattenedTargets addObject:[targets objectAtIndex:i]];
        }
    }

    targets = [flattenedTargets copy];

    NSLog(@"Updating targets: %@", targets);

    bool result = [self.imageRecViewController updateTargets:targets];

    [self handleResultMessage:result command:command];
}

#pragma mark - Util_Methods
- (void) startVuforiaWithImageTargetFile:(NSString *)imageTargetfile imageTargetNames:(NSArray *)imageTargetNames overlayOptions:(NSDictionary *)overlayOptions vuforiaLicenseKey:(NSString *)vuforiaLicenseKey {

    [[NSNotificationCenter defaultCenter] removeObserver:self name:@"ImageMatched" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(imageMatched:) name:@"ImageMatched" object:nil]; // TODO: remove
    [[NSNotificationCenter defaultCenter] removeObserver:self name:@"MarkerUpdate" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(markerUpdate:) name:@"MarkerUpdate" object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:@"CloseRequest" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(closeRequest:) name:@"CloseRequest" object:nil];

    self.imageRecViewController = [[ViewController alloc] initWithFileName:imageTargetfile targetNames:imageTargetNames overlayOptions:overlayOptions vuforiaLicenseKey:vuforiaLicenseKey];

    // make webview transparent and display the camera below the webview

    [self.viewController addChildViewController:self.imageRecViewController];
    
    self.webView.opaque = NO;
    self.webView.backgroundColor = [UIColor clearColor];
    [self.webView.scrollView setScrollEnabled:NO];
    [self.webView.scrollView setBounces:NO];
    
    [self.webView.superview addSubview:self.imageRecViewController.view];
    [self.webView.superview bringSubviewToFront:self.webView];
    
    // most reliable hack to force redraw

    [self.webView setFrame:CGRectMake(self.webView.frame.origin.x, self.webView.frame.origin.y, self.webView.frame.size.width-1, self.webView.frame.size.height-1)];
    [self.webView setFrame:CGRectMake(self.webView.frame.origin.x, self.webView.frame.origin.y, self.webView.frame.size.width+1, self.webView.frame.size.height+1)];
}

// send model view and projection matrices for the recognized markers
// TODO: cleanup and document
- (void)markerUpdate:(NSNotification *)notification {
    
    NSDictionary* userInfo = notification.userInfo;
    
    // get a stringified projectionMatrix
    NSData* projectionMatrix = [self.imageRecViewController getProjectionMatrix];
    float* projectionMatrixData = (float*) [projectionMatrix bytes];
    NSString* projectionMatrixString = [self stringFromMatrix:projectionMatrixData]; // TODO: add a separate plugin function to query this on demand, rather than sending every time
    
    // for each marker detected, add its stringified name and modelViewMatrix to an array
    NSMutableArray* markersFound = userInfo[@"markersFound"];
    NSMutableArray* markersJson = [NSMutableArray arrayWithCapacity:markersFound.count];
    
    for (NSDictionary* marker in markersFound) {
        
        NSString* markerNameString = marker[@"name"];
        NSData* modelViewMatrix = marker[@"modelViewMatrix"];
        float* modelViewMatrixData = (float*) [modelViewMatrix bytes];
        NSString* modelViewMatrixString = [self stringFromMatrix:modelViewMatrixData];
        
        NSDictionary* markerJson = @{@"name": markerNameString, @"modelViewMatrix": modelViewMatrixString};
        [markersJson addObject:markerJson];
    }
    
    // contains the name and modelView of each marker, and the projection matrix
    NSDictionary* jsonObj = @{@"markersFound": markersJson, @"projectionMatrix": projectionMatrixString};
    
    // only send the markers to the webview if any of them have changed
    if (![jsonObj isEqualToDictionary:self.previousMarkers]) {
        self.previousMarkers = jsonObj;
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_OK messageAsDictionary: jsonObj];
        [pluginResult setKeepCallbackAsBool:TRUE];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
    }
}

- (NSString *)stringFromMatrix:(float*)mat
{
    return [NSString stringWithFormat:@"[%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f]",
            mat[0], mat[1], mat[2], mat[3],
            mat[4], mat[5], mat[6], mat[7],
            mat[8], mat[9], mat[10],mat[11],
            mat[12],mat[13],mat[14],mat[15]];
}

- (void)imageMatched:(NSNotification *)notification { // TODO: remove

    NSDictionary* userInfo = notification.userInfo;

    NSLog(@"Vuforia Plugin :: image matched");
    NSDictionary* jsonObj = @{@"status": @{@"imageFound": @true, @"message": @"Image Found."}, @"result": @{@"imageName": userInfo[@"result"][@"imageName"]}};

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_OK messageAsDictionary: jsonObj];

    if(!self.autostopOnImageFound){
        [pluginResult setKeepCallbackAsBool:TRUE];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];

    if(self.autostopOnImageFound){
        [self VP_closeView];
    }
}

- (void)closeRequest:(NSNotification *)notification {

    NSDictionary* jsonObj = @{@"status": @{@"manuallyClosed": @true, @"message": @"User manually closed the plugin."}};

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_OK messageAsDictionary: jsonObj];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
    [self VP_closeView];
}

- (void) VP_closeView {
    if(self.startedVuforia == true){
        [self.imageRecViewController close];
        self.imageRecViewController = nil;
        self.startedVuforia = false;
    }
}

-(void) handleResultMessage:(bool)result command:(CDVInvokedUrlCommand *)command {
    if(result){
        [self sendSuccessMessage:command];
    } else {
        [self sendErrorMessage:command];
    }
}

-(void) sendSuccessMessage:(CDVInvokedUrlCommand *)command {
    NSDictionary *jsonObj = [ [NSDictionary alloc] initWithObjectsAndKeys :
                             @"true", @"success",
                             nil
                             ];

    CDVPluginResult *pluginResult = [ CDVPluginResult
                                     resultWithStatus    : CDVCommandStatus_OK
                                     messageAsDictionary : jsonObj
                                     ];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

-(void) sendErrorMessage:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult = [ CDVPluginResult
                                     resultWithStatus    : CDVCommandStatus_ERROR
                                     messageAsString: @"Did not successfully complete"
                                     ];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
