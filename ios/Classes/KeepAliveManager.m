//
//  KeepAliveManager.m
//  bluetooth_helper
//
//  Created by 陈柏伶 on 2020/6/13.
//

#import "KeepAliveManager.h"
#import <CoreBluetooth/CoreBluetooth.h>

@interface KeepAliveManager() <CBCentralManagerDelegate>

@property (nonatomic, strong) CBCentralManager *centralManager;
@property (nonatomic, strong) NSTimer *timer;

@end

@implementation KeepAliveManager

+ (instancetype)shared {
    static KeepAliveManager *manager;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        manager = [[self alloc] init];
    });
    return manager;
}

- (instancetype)init {
    if (self = [super init]) {
        [self centralManager];
    }
    return self;
}

- (CBCentralManager *)centralManager {
    if (_centralManager == nil) {
        _centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
    }
    return _centralManager;
}


- (void)centralManagerDidUpdateState:(nonnull CBCentralManager *)central {
    
}

- (void)keepAliveWithOpen:(BOOL)isOpen {
    if (isOpen) {
        [self.timer invalidate];
        self.timer = [NSTimer scheduledTimerWithTimeInterval:10 target:self selector:@selector(beginScan) userInfo:nil repeats:true];
        [self.timer fire];
    } else {
        if (self.timer != nil) {
            [self.timer invalidate];
            [self.centralManager stopScan];
        }
        self.timer = nil;
    }
}

- (void)beginScan {
    [self.centralManager stopScan];
    [self.centralManager scanForPeripheralsWithServices:nil options:nil];
}

@end
