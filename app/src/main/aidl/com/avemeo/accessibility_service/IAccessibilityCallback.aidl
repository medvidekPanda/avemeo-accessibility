package com.avemeo.accessibility_service;

interface IAccessibilityCallback {
    void onKeyEventReceived(int keyCode);
    void onLogDebugReceived(String message, String tag);
    void onLogErrorReceived(String message, String tag);
    void onAccessibilityEventTypeReceived(int eventType);
    void onAccessibilityEventPackageNameReceived(String packageName);
} 