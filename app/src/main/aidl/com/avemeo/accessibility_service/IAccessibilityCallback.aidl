package com.avemeo.accessibility_service;

interface IAccessibilityCallback {
    void onKeyEventReceived(int keyCode);
    void onAccessibilityEventTypeReceived(int eventType);
    void onAccessibilityEventPackageNameReceived(String eventType);
} 