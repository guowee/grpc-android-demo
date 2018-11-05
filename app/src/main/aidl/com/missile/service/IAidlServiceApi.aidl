// IAidlServiceApi.aidl
package com.missile.service;

// Declare any non-default types here with import statements

interface IAidlServiceApi {
       void reboot(boolean confirm, String reason, boolean wait);
       String getVersion();
}
