# Add project specific ProGuard rules here.

-keep class com.ren42377.bimalaunch.core.NativeBridge { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
