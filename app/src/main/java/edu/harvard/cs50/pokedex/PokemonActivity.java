package edu.harvard.cs50.pokedex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaSync;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class PokemonActivity extends AppCompatActivity {
    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private String url;
    private RequestQueue requestQueue;
    private ImageView imageView;
    private TextView description;
    Button catchButton;

    public boolean caught = false;
    public static final String PREFS_NAME = "PREFS_NAME";
    public static final String BUTTON_STATUS = "status";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = getIntent().getStringExtra("url");
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);
        catchButton = findViewById(R.id.pokemon_catch);
        imageView = findViewById(R.id.pokemon_view);
        description = findViewById(R.id.pokemon_description);

        load();

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        caught = sharedPreferences.getBoolean(BUTTON_STATUS,false);
        if (caught) {
            catchButton.setText("Release");
        } else {
            catchButton.setText("Catch");
        }


    }

    public void load() {
        type1TextView.setText("");
        type2TextView.setText("");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    nameTextView.setText(response.getString("name"));
                    numberTextView.setText(String.format("#%03d", response.getInt("id")));

                    JSONArray typeEntries = response.getJSONArray("types");
                    for (int i = 0; i < typeEntries.length(); i++) {
                        JSONObject typeEntry = typeEntries.getJSONObject(i);
                        int slot = typeEntry.getInt("slot");
                        String type = typeEntry.getJSONObject("type").getString("name");

                        if (slot == 1) {
                            type1TextView.setText(type);
                        }
                        else if (slot == 2) {
                            type2TextView.setText(type);
                        }
                    }
                    loadImage(response);
                    loadText(response);
                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });

        requestQueue.add(request);
    }


    public void toggleCatch(View view) {
        catchButton = findViewById(R.id.pokemon_catch);
        String name = nameTextView.getText().toString();
        if (caught) {
            caught = false;
            catchButton.setText("Catch");
        } else {
            caught = true;
            catchButton.setText("Release");
        }

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(name,name);
        editor.putBoolean(BUTTON_STATUS, caught);
        editor.apply();

    }

    public void loadText(JSONObject response) {
        String speciesUrl = "";
        try {
            speciesUrl = response.getJSONObject("species").getString("url");
        } catch (JSONException e) {
            Log.e("Pokedex", "Pokemon description error");
        }
        Log.d("Pokedex", "Pokemon species error url: " + speciesUrl);
        if (!speciesUrl.equals("")) {
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, speciesUrl, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    JSONArray flavEntries = null;
                    try {
                        flavEntries = response.getJSONArray("flavor_text_entries");
                    } catch (JSONException e) {
                        Log.e("Pokedex","Pokemon array flavor error");
                    }
                    for (int i = 0; i < flavEntries.length(); i++) {
                        try {
                            if (flavEntries.getJSONObject(i).getJSONObject("language").getString("name").equals("en")) {
                                String flaEntry = flavEntries.getJSONObject(i).getString("flavor_text");
                                description.setText(flaEntry);
                                break;
                            }
                        } catch (JSONException e) {
                            Log.e("Pokedex", "Pokemon flavor json error");
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Pokedex", "Pokemon description request error");
                }
            });
            requestQueue.add(request);

        }
    }

    public void loadImage(JSONObject response) {
        try {
            String imageUrl = response.getJSONObject("sprites").getString("front_default");
            new DownloadSpriteTask().execute(imageUrl);
        } catch (JSONException e) {
            Log.e("Pokedex","Pokemon json image error");
        }
    }

    private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                return BitmapFactory.decodeStream(url.openStream());
            }
            catch (IOException e) {
                Log.e("cs50", "Download sprite error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // load the bitmap into the ImageView!
            imageView.setImageBitmap(bitmap);
        }
    }


}
