# Ziwo SDK

Our team built a SDK that provides an easy way to integrate Ziwo calls to your android app. Written in **Kotlin**, joining **WebSockets**, **[Verto protocol](https://evoluxbr.github.io/verto-docs/)** and **[GoogleRTC](https://webrtc.org/)**.

## Requirements
- Android SDK >= 26

## Installation

check for the latest version in from [github releases](https://github.com/ASWATFZLLC/android-ziwo-sdk/releases)

### Gradle
```
implementation 'com.ziwo.ziwosdk:ziwosdk:0.0.04'
```

### Maven
```
<dependency>
	<groupId>com.ziwo.ziwosdk</groupId>
	<artifactId>ziwosdk</artifactId>
	<version>0.0.04</version>
	<type>pom</type>
</dependency>
```

## Initialization

In order to setup the Ziwo SDK and the Verto protocol, follow the steps below.

1. **Get an instance of ZiwoSDK**.

```kotlin
ziwoSdk = Ziwo(applicationContext, debugBool)
```

2.A **Get an access token** (only needed if you dont have a valid token)

```kotlin
val response = ziwoSdk.ziwoApiClient.login(callCenter, userName, userPassword)

// save the response
response.content.access_token
response.content.ccLogin
response.content.ccPassword
```

2.B If you already have an access token **set the call center and access token** for the modules you need to use

```kotlin
// http api client
ziwoSdk.ziwoApiClient.setCredentials(callCenter, accessToken)

// verto websocket login
ziwoSdk.vertoWs.login(
    callCenter,
    "agent-${ccLogin}",
    stringToMd5("${ccLogin}${ccPassword}"),
    sessionId,
)

// websocket api login
ziwoSdk.ziwoApiWs.login(
    callCenter,
    access_token
)
```

3. **Implement Listeners** if youre using the websockets module
```
ziwoSdk.vertoWs.vertoHandler = myVertoListinerObject
ziwoSdk.ziwoApiWs.socketHandler = myApiSocketListinerObject
```


## Example

- **To make a call**
```
ziwoSdk.vertoWs.webSocketCommandsSender.startCall( phoneNumber )
```

- **To receive a call**

``` kotlin
myVertoListinerObject = object: VertoHandlerInterface{
    ...
    override fun callStarted(call: Call) {
        // your logic
    }
    ...
}

ziwoSdk.vertoWs.vertoHandler = myVertoListinerObject
```

## Getting Help

- **Have a bug to report?** [Open a GitHub issue](https://github.com/ASWATFZLLC/android-ziwo-sdk/issues). If possible, include the version of ZiwoSDK, a full log, and a project that shows the issue.
- **Have a feature request?** [Open a GitHub issue](https://github.com/ASWATFZLLC/android-ziwo-sdk/issues). Tell us what the feature should do and why you want the feature.


## Contributing
Pull requests are welcome. For major changes, [please open an issue](https://github.com/ASWATFZLLC/android-ziwo-sdk/issues) first to discuss what you would like to change.

