package com.easygautam.openid

import android.net.Uri


object AuthConfiguration {

    const val clientId = "your-client-id"
    const val scopes = "openid email profile {all scopes}"

    val authRedirectUri: Uri = Uri.parse("com.easygautam.openid://redirectUrl")
    val endSessionRedirectUri: Uri = Uri.parse("com.easygautam.openid://redirectUrl")
    val authEndpointUri: Uri = Uri.parse("auth-end-point-url")
    val tokenEndpointUri: Uri = Uri.parse("token-end-point-url")
    val registrationEndpointUri: Uri = Uri.parse("registration-end-point-url")
    val endSessionEndpointUri: Uri = Uri.parse("end-session-end-point-url")


}