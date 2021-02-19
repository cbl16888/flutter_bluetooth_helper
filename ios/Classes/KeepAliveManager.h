//
//  KeepAliveManager.h
//  bluetooth_helper
//
//  Created by 陈柏伶 on 2020/6/13.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface KeepAliveManager : NSObject

+ (instancetype)shared;
- (void)keepAliveWithOpen:(BOOL)isOpen;

@end

NS_ASSUME_NONNULL_END
