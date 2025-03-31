package com.avemeo.accessibility_service;

import com.avemeo.accessibility_service.IAccessibilityCallback;

interface IAccessibilityService {
    void registerCallback(IAccessibilityCallback callback);
    void unregisterCallback(IAccessibilityCallback callback);
    void launchApp(String packageName);
    void setDebugLogging(boolean enabled);
}