package de.blau.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Taken from https://www.bignerdranch.com/blog/splash-screens-the-right-way/
 *
 */
public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, Main.class);
        startActivity(intent);
        finish();
    }
}
