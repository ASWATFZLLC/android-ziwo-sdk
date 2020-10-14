package com.ziwo.ziwosdk


open class ZiwoException(message: String) : Exception(message) {}

class DomainNotSetException : ZiwoException(
    "You need to set the domain before you can do this operation"
) {}

class SessionIdNotSetException(message: String? = null) : ZiwoException(
    message ?: "You need to provide a session id before you can do this operation"
) {}

class SessionIdNotValidException : ZiwoException(
    "You need to provide a valid session id string in valid uuid format"
) {}