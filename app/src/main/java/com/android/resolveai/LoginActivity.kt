package com.android.resolveai

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.android.resolveai.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInButton: SignInButton
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_login)

        signInButton = binding.signInButton
        // Configures the GoogleSignIn to request the user email
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener {
            val firebaseUser = auth.currentUser
            // If firebaseUser is not null, user is logged in, so there's no need to stay in the LoginActivity
            if (firebaseUser != null) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                if(Build.VERSION.SDK_INT > 21) {
                    window.exitTransition = null
                }
                overridePendingTransition(0, 0);
                finish()
            }
        }
        database = FirebaseDatabase.getInstance().reference
        signInButton.setOnClickListener {
            signIn()
        }

    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        auth.removeAuthStateListener(authStateListener)
        super.onStop()
    }
    private fun signIn() {
        val signInIntent: Intent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                val toast = Toast.makeText(applicationContext, "Ocorreu um problema na autenticação.", Toast.LENGTH_LONG)
                toast.show()
            }
        }
    }

    // Authenticate the user, based on the chosen google account
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result!!.additionalUserInfo!!.isNewUser
                    if (isNewUser) {
                        writeNewUser() //Write the new user data in Firebase Database
                    }
                    val intent = Intent(baseContext, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        window.exitTransition = null
                    }
                    overridePendingTransition(0, 0);
                    finish()
                } else {
                    val toast = Toast.makeText(applicationContext, "Não foi possível fazer login :(.", Toast.LENGTH_LONG)
                    toast.show()
                }
            }
    }

    //Write the new user data in Firebase Database. User, Post and Comment are data classes
    private fun writeNewUser() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val comment = Comment()
        val post = Post(
        )

        val user = User(firebaseUser!!.uid, firebaseUser.displayName, firebaseUser.email, "", "", listOf(post), listOf(post), listOf(comment))
        database.child("users").child(firebaseUser.uid).setValue(user)
    }

}