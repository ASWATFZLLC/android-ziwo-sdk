# Keep data classes and all of their members from being obfuscated
-keepclasseswithmembers class com.ziwo.ziwosdk.** {
    public ** component1();
    <fields>;
}
## Socket.io
#-keep class io.socket.** { *; }
#-dontwarn io.socket.**

# WebRTC
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**


## Kotlin Standard Library
#-keep class kotlin.** { *; }
#-dontwarn kotlin.**
#
## Kotlin Reflect
#-keep class kotlin.reflect.** { *; }
#-dontwarn kotlin.reflect.**
#
## Kotlin @Parcelize
#-keep @kotlinx.parcelize.Parcelize class * {
#    <fields>;
#    <init>(...);
#}
#-keep class * implements android.os.Parcelable {
#    public static final ** CREATOR;
#}
#
## Retrofit and OkHttp
#-keepattributes Signature
#-keepattributes Exceptions
#-keepattributes *Annotation*
#-keep class retrofit2.** { *; }
#-dontwarn retrofit2.**
#-keep class com.squareup.okhttp3.** { *; }
#-dontwarn com.squareup.okhttp3.**
#-keepclasseswithmembers class * {
#    @retrofit2.http.* <methods>;
#}
#
## Platform calls Class.forName on types which do not exist on Android to determine platform.
#-dontnote retrofit2.Platform
## Retain generic type information for use by reflection by converters and adapters.
#
#
# GSON
#-keep class com.google.gson.** { *; }
#-dontwarn com.google.gson.**
#-keep class com.yourcompany.sdk.model.** { *; }
#-keep class * implements java.io.Serializable {
#    private static final long serialVersionUID;
#    private static final java.io.ObjectStreamField[] serialPersistentFields;
#    private void writeObject(java.io.ObjectOutputStream);
#    private void readObject(java.io.ObjectInputStream);
#    java.lang.Object writeReplace();
#    java.lang.Object readResolve();
#}
#
## Chucker
#-keep class com.github.chuckerteam.chucker.** { *; }
#-dontwarn com.github.chuckerteam.chucker.**
#
# Prevent obfuscation of Parcelable classes and members
#-keep class * implements android.os.Parcelable {
#  public static final android.os.Parcelable$Creator *;
#}
#
## Preserve all native method names and the classes that use them
#-keepclasseswithmembernames class * {
#    native <methods>;
#}
#
# Preserve all classes that implement Serializable interface
#-keepclassmembers class * implements java.io.Serializable {
#    static final long serialVersionUID;
#    private static final java.io.ObjectStreamField[] serialPersistentFields;
#    private void writeObject(java.io.ObjectOutputStream);
#    private void readObject(java.io.ObjectInputStream);
#    java.lang.Object writeReplace();
#    java.lang.Object readResolve();
#}
## Keep Parcelable Creator and class names
#-keepclassmembers class * implements android.os.Parcelable {
#    public static final ** CREATOR;
#}
#
#
## ... add similar rules for all your other data classes
#
## If you're using Kotlin, you may need to keep the names of the properties' backing fields
#-keepclassmembers class * {
#    *** Companion;
#    public static ** Companion;
#}
#
# If using reflection, keep names of methods and fields that could be accessed via reflection
#-keepclassmembers class * {
#    @com.google.gson.annotations.SerializedName <fields>;
#    @com.google.gson.annotations.Expose <fields>;
#}
