package com.easygautam.openid

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.*
import kotlin.coroutines.resume

private const val RC_AUTH = 101
private const val END_SESSION_REQUEST_CODE = 102
private const val TAG = "OpenIdAuthentication"

/**
 * Authenticate user using openid auth.
 *
 * @property context application context is required
 * @property scope coroutine scope required synchronize the authentication response
 */
class OpenIdAuthentication(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private lateinit var activity: Activity
    private var doAuthContinuation: CancellableContinuation<AuthResult>? = null
    private var doClearAuthContinuation: CancellableContinuation<AuthResult>? = null

    /**
     * Configure the auth service from [AuthConfiguration]
     */
    private val serviceConfig by lazy {
        AuthorizationServiceConfiguration(
            AuthConfiguration.authEndpointUri,
            AuthConfiguration.tokenEndpointUri,
            AuthConfiguration.registrationEndpointUri,
            AuthConfiguration.endSessionEndpointUri
        )
    }

    /**
     * Create the auth request
     */
    private val authRequestBuilder by lazy {
        AuthorizationRequest.Builder(
            serviceConfig,
            AuthConfiguration.clientId,
            ResponseTypeValues.CODE,
            AuthConfiguration.authRedirectUri
        )
    }

    /**
     * Build the auth request
     */
    private val authRequest by lazy {
        authRequestBuilder
            .setScope(AuthConfiguration.scopes)
            .build()
    }

    /**
     * Auth service to perform authentication related task
     */
    private val authService by lazy { AuthorizationService(context) }

    /**
     * Attach activity to start authentication and receive result in the activity
     *
     * @param activity
     */
    fun attachActivity(activity: Activity) {
        this.activity = activity
    }

    /**
     * Handle the authentication result
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_AUTH) {
            LogUtils.debug(TAG, "Authentication result received")
            if (resultCode == Activity.RESULT_CANCELED) {
                // Handle auth cancel
                doAuthContinuation?.resume(AuthResult.Cancel)
            } else {
                extractAuth(data)
            }
        } else if (requestCode == END_SESSION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let { intent ->
                val resp = AuthorizationResponse.fromIntent(intent)
                val ex: AuthorizationException? = AuthorizationException.fromIntent(intent)
                doAuthContinuation?.resume(AuthResult.Success(AuthState(resp, ex)))
            } ?: doAuthContinuation?.resume(AuthResult.Failed())
        }
    }

    private fun extractAuth(data: Intent?) = scope.launch {
        doAuthContinuation?.resume(data?.let { intent ->
            val resp = AuthorizationResponse.fromIntent(intent)
            val ex: AuthorizationException? = AuthorizationException.fromIntent(intent)
            resp?.let { authRes ->
                authRes.fetchAccessToken(ex).let {
                    if (it.isAuthorized) {
                        AuthResult.Success(it)
                    } else {
                        AuthResult.Failed()
                    }
                }
            } ?: AuthResult.Failed()
        } ?: AuthResult.Failed())
    }

    private suspend fun AuthorizationResponse.fetchAccessToken(ex: AuthorizationException?) =
        suspendCancellableCoroutine<AuthState> { cancelableCoroutine ->
            val authState = AuthState(this, ex)
            authService.performTokenRequest(createTokenExchangeRequest()) { tokenResponse: TokenResponse?, authEx: AuthorizationException? ->
                authState.update(tokenResponse, authEx)
                cancelableCoroutine.resume(authState)
            }
        }

    private suspend fun AuthState.refreshToken() =
        suspendCancellableCoroutine<AuthState> { cancelableCoroutine ->
            authService.performTokenRequest(createTokenRefreshRequest()) { tokenResponse: TokenResponse?, authEx: AuthorizationException? ->
                update(tokenResponse, authEx)
                cancelableCoroutine.resume(this)
            }
        }

    suspend fun doAuthorization() = suspendCancellableCoroutine<AuthResult> { cancelableCoroutine ->
        doAuthContinuation = cancelableCoroutine
        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        activity.startActivityForResult(authIntent, RC_AUTH)
    }

    suspend fun doClearAuthorization(currentState: AuthState) =
        suspendCancellableCoroutine<AuthResult> { cancelableCoroutine ->
            doClearAuthContinuation = cancelableCoroutine
            val config = currentState.authorizationServiceConfiguration
            if (config?.endSessionEndpoint != null) {
                val endSessionIntent: Intent = authService.getEndSessionRequestIntent(
                    EndSessionRequest.Builder(
                        config,
                        currentState.idToken!!,
                        AuthConfiguration.endSessionRedirectUri
                    ).build()
                )
                activity.startActivityForResult(
                    endSessionIntent,
                    END_SESSION_REQUEST_CODE
                )
            } else {
                doClearAuthContinuation?.resume(AuthResult.Failed("End session endpoint not configured"))
            }
        }

    suspend fun doRefreshToken(authState: AuthState): AuthResult =
        if (authState.refreshToken.isNullOrEmpty()) {
            AuthResult.Failed("No refresh token found")
        } else {
            authState.refreshToken().let {
                if (it.isAuthorized) {
                    AuthResult.Success(it)
                } else {
                    AuthResult.Failed()
                }
            }
        }
}

open class AuthResult {
    class Success(val authState: AuthState) : AuthResult()
    object Cancel : AuthResult()
    class Failed(val message: String? = null) : AuthResult()
}
