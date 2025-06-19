package com.example.bablootooth;

import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {

    EditText emailEditText, passwordEditText;
    Button loginButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);

        // Initialize Firebase Auth
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(view -> {
            String inputEmail = emailEditText.getText().toString().trim();
            String inputPassword = passwordEditText.getText().toString().trim();

            if (inputEmail.isEmpty() || inputPassword.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter both fields", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.signInWithEmailAndPassword(inputEmail, inputPassword)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(MainActivity.this, "Authentication passed.", Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(MainActivity.this, BluetoothScreenActivity.class));
                            finish();
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                            //updateUI(null);
                        }
                    });
            }
        });
    }
}