package com.easygautam.openid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState


class MainActivity : AppCompatActivity() {

    private val openIdAuth = OpenIdAuthentication(this, lifecycleScope)

    private val btnAuth by lazy { findViewById<Button>(R.id.btnAuth) }
    private val btnClearAuth by lazy { findViewById<Button>(R.id.btnClearAuth) }
    private val btnRefreshToken by lazy { findViewById<Button>(R.id.btnRefreshToken) }
    private val resultText by lazy { findViewById<TextView>(R.id.resultText) }
    private val progress by lazy { findViewById<ProgressBar>(R.id.progress) }

    private var authState: AuthState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        openIdAuth.attachActivity(this)

        // Initial view state
        hideProgress()
        nonAuthUI()

        btnAuth.setOnClickListener {
            showProgress()
            lifecycleScope.launch { doAuth() }
        }
        btnClearAuth.setOnClickListener {
            lifecycleScope.launch { clearAuth() }
        }
        btnRefreshToken.setOnClickListener {
            lifecycleScope.launch { refreshTokenAuth() }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        openIdAuth.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun doAuth() {
        openIdAuth.doAuthorization()
            .let {
                hideProgress()
                when (it) {
                    is AuthResult.Success -> {
                        // Detail of authentication
                        authenticatedUi(it.authState)
                    }
                    is AuthResult.Cancel -> {
                        // Authentication canceled
                        Toast.makeText(this, "Authentication canceled", Toast.LENGTH_SHORT).show()
                    }
                    is AuthResult.Failed -> {
                        // Authentication failed
                        Toast.makeText(
                            this,
                            it.message ?: "Authentication failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            }
    }

    private suspend fun clearAuth() {
        authState?.let { currentAuthState ->
            openIdAuth.doClearAuthorization(currentAuthState)
                .let {
                    hideProgress()
                    when (it) {
                        is AuthResult.Success -> {
                            nonAuthUI()
                        }
                        is AuthResult.Failed -> {
                            // Authentication failed
                            Toast.makeText(
                                this,
                                it.message ?: "Clear authentication failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
        } ?: Toast.makeText(
            this,
            "No auth state found",
            Toast.LENGTH_SHORT
        ).show()
    }

    private suspend fun refreshTokenAuth() {
        authState?.let { currentAuthState ->
            openIdAuth.doRefreshToken(currentAuthState)
                .let {
                    hideProgress()
                    when (it) {
                        is AuthResult.Success -> {
                            authenticatedUi(it.authState)
                        }
                        is AuthResult.Failed -> {
                            // Authentication failed
                            Toast.makeText(
                                this,
                                it.message ?: "Refresh token failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
        } ?: Toast.makeText(
            this,
            "No auth state found",
            Toast.LENGTH_SHORT
        ).show()

    }

    private fun authenticatedUi(authState: AuthState) {
        this.authState = authState
        btnRefreshToken.isEnabled = true
        btnClearAuth.isEnabled = true
        btnAuth.isEnabled = false
        resultText.visibility = View.VISIBLE
        resultText.text =
            "Refresh Token = ${authState.refreshToken}\n\nAccess Token = ${authState.accessToken}"
    }

    private fun nonAuthUI() {
        btnRefreshToken.isEnabled = false
        btnClearAuth.isEnabled = false
        btnAuth.isEnabled = true
        resultText.visibility = View.GONE
    }

    private fun showProgress() {
        progress.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progress.visibility = View.GONE
    }

}