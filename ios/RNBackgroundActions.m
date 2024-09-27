@import UIKit;
#import "RNBackgroundActions.h"

@implementation RNBackgroundActions {
    UIBackgroundTaskIdentifier bgTask;
}

RCT_EXPORT_MODULE()
- (NSArray<NSString *> *)supportedEvents
{
    return @[@"expiration"];
}

- (void)requestNotificationPermissions {
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert | UNAuthorizationOptionSound | UNAuthorizationOptionBadge)
                          completionHandler:^(BOOL granted, NSError * _Nullable error) {
        if (!granted) {
            NSLog(@"Notification permissions not granted");
        }
    }];
}

// 백그라운드 작업 시작
- (void) _start:(NSDictionary *)options
{
    [self _stop]; // 기존 백그라운드 작업이 있으면 종료
    bgTask = [[UIApplication sharedApplication] beginBackgroundTaskWithName:@"RNBackgroundActions" expirationHandler:^{
        [self onExpiration]; // 백그라운드 작업이 만료되면 실행
        [[UIApplication sharedApplication] endBackgroundTask: self->bgTask];
        self->bgTask = UIBackgroundTaskInvalid;
    }];

    NSString *notificationTitle = options[@"taskTitle"] ?: @"Fleetune Driver App";
    NSString *notificationBody = options[@"taskDesc"] ?: @"App is running in background";
    
    [self showNotification:notificationTitle body:notificationBody];
}

- (void) _stop
{
    if (bgTask != UIBackgroundTaskInvalid) {
        [[UIApplication sharedApplication] endBackgroundTask:bgTask];
        bgTask = UIBackgroundTaskInvalid;
    }
}

- (void)onExpiration
{
    [self sendEventWithName:@"expiration"
                       body:@{}];
}

- (void)showNotification:(NSString *)title body:(NSString *)body {
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    
    // 알림 내용 설정
    UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
    content.title = [NSString localizedUserNotificationStringForKey:title arguments:nil];
    content.body = [NSString localizedUserNotificationStringForKey:body arguments:nil];
    content.sound = [UNNotificationSound defaultSound];\
    
    // 1초 뒤에 알림을 보내도록 설정
    UNTimeIntervalNotificationTrigger *trigger = [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:1 repeats:NO];
    
    // 알림 요청 설정
    UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:@"RNBackgroundActionsNotification"
                                                                          content:content
                                                                          trigger:trigger];
    
    // 알림을 추가
    [center addNotificationRequest:request withCompletionHandler:^(NSError * _Nullable error) {
        if (error != nil) {
            NSLog(@"Error adding notification: %@", error.localizedDescription);
        }
    }];
}

RCT_EXPORT_METHOD(start:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [self _start:options];
    resolve(nil);
}

RCT_EXPORT_METHOD(stop:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [self _stop];
    resolve(nil);
}

@end
