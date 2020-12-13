package com.example.googlelogin.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest
import com.example.googlelogin.R
import com.example.googlelogin.data.model.LoggedInUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import org.json.JSONObject

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private val RESULT_SIGN_IN: Int = 1
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener(this);
        findViewById<Button>(R.id.sign_out_button).setOnClickListener(this);

        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("SERVER_CLIENT_ID")
                .requestEmail()
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.sign_in_button -> signIn()
            R.id.sign_out_button -> signOut()
        }
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.getSignInIntent()
        startActivityForResult(signInIntent, RESULT_SIGN_IN)
    }

    private fun signOut() {
        mGoogleSignInClient.signOut();
        mGoogleSignInClient.revokeAccess();
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_SIGN_IN) {

            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)

            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account =
                completedTask.getResult(ApiException::class.java)

            Log.d("LoginActivity", account?.email + ", " + account?.displayName + ", ")

            if (account != null) {
                authenticateWithBackend(LoggedInUser(account.idToken!!))
            }

        } catch (e: ApiException) {

            Log.e("LoginActivity", e.message)
        }
    }

    private fun authenticateWithBackend(loggedInUser: LoggedInUser) {

        val cache = DiskBasedCache(cacheDir, 1024 * 1024)
        val network = BasicNetwork(HurlStack())
        val requestQueue = RequestQueue(cache, network).apply {
            start()
        }

        val url = "http://10.0.2.2:3000/tokeninfo"

        val params = HashMap<String, String>()
        params["googleIdToken"] = loggedInUser.googleIdToken

        val jsonRequest =
            JsonObjectRequest(
                Request.Method.POST,
                url,
                JSONObject(params as Map<*, *>),
                Response.Listener { response ->
                    Log.d("LoginActivity", "Response:$response")
                },
                Response.ErrorListener { error ->
                    Log.e("LoginActivity", "Error:$error")
                }
            )

        requestQueue.add(jsonRequest)
    }
}