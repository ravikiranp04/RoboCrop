package app.ij.mlwithtensorflowlite;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.CursorAdapter;
import android.util.Log;
import android.widget.TextView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class CureActivity extends AppCompatActivity {

    private TextView diseaseInfoTextView;
    private SearchView searchView;
    private Button searchButton;
    private TextView description;
    private SimpleCursorAdapter suggestionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cure);

        // Initialize views
        diseaseInfoTextView = findViewById(R.id.diseaseInfoTextView);
        searchView = findViewById(R.id.searchView);
        searchButton = findViewById(R.id.searchButton);
        description=findViewById(R.id.description);
        description.setText("Search For Your Illness");
        // Initialize suggestions adapter
        String[] from = new String[] { "disease_name" };
        int[] to = new int[] { android.R.id.text1 };
        suggestionsAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null,
                from,
                to,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        searchView.setSuggestionsAdapter(suggestionsAdapter);
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                // Get the selected suggestion and set it as the query
                Cursor cursor = suggestionsAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    String query = cursor.getString(cursor.getColumnIndex("disease_name"));
                    searchView.setQuery(query, true);
                }
                return true;
            }
        });
        // listener for the query text changes
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Handle query submission
                displayDiseaseInfo(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                filterAndDisplaySuggestions(newText);
                return false;
            }
        });


        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the query from the search view and trigger search
                String query = searchView.getQuery().toString();
                displayDiseaseInfo(query);
            }
        });
    }

    private void displayDiseaseInfo(String diseaseName) {
        // Load JSON data
        String json = loadJSONFromAsset();


        if (json != null) {
            try {
                // Parse JSON data
                JSONObject jsonObject = new JSONObject(json);


                JSONObject diseaseObject = jsonObject.optJSONObject(diseaseName);


                diseaseInfoTextView.setText("");

                // Check if disease information exists
                if (diseaseObject != null) {
                    // Iterate over each treatment method for the disease
                    Iterator<String> plantKeys = diseaseObject.keys();
                    while (plantKeys.hasNext()) {
                        String plant = plantKeys.next();
                        System.out.println(plant);
                        JSONObject plantObject = diseaseObject.getJSONObject(plant);
                        JSONArray methods = plantObject.getJSONArray("Method");
                        SpannableString spannableString = new SpannableString(plant);
                        int backgroundColor = ContextCompat.getColor(this, android.R.color.holo_blue_light); // 'this' is your activity context
                        BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(backgroundColor);
                        spannableString.setSpan(backgroundColorSpan, 0, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        diseaseInfoTextView.append("Plant : "+spannableString + "\n\n");
                        // Iterate over each method for the plant
                        for (int i = 0; i < methods.length(); i++) {
                            String method = methods.getString(i);
                            diseaseInfoTextView.append(method + "\n\n");
                        }
                    }
                } else {

                    diseaseInfoTextView.setText(getString(R.string.info_not_available));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            Log.e("displayDiseaseInfo", "JSON data is null");
            System.out.println("Json data is null");
        }
    }

    // Method to load JSON data
    private String loadJSONFromAsset() {
        String json;
        try {
            InputStream is = getAssets().open("disease.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            int bytesRead = is.read(buffer);
            is.close();
            if (bytesRead == -1) {

                json = "";
                Log.e("loadJSONFromAsset", "No data was read from the InputStream");
                System.out.println("Fail");
            } else {
                json = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                Log.e("loadJSONFromAsset", "Success");
                System.out.println("Success");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Error");
            return null;
        }
        return json;
    }


    private void filterAndDisplaySuggestions(String newText) {
        // Load JSON data from assets folder
        String json = loadJSONFromAsset();


        if (json != null) {
            try {
                // Parse JSON data
                JSONObject jsonObject = new JSONObject(json);


                MatrixCursor cursor = new MatrixCursor(new String[] { "_id", "disease_name" });

                Iterator<String> keys = jsonObject.keys();
                int id = 0;
                while (keys.hasNext()) {
                    String key = keys.next();

                    if (key.toLowerCase().replace(" ","").contains(newText.toLowerCase())) {
                        cursor.addRow(new Object[] { id++, key });
                    }
                }

                suggestionsAdapter.swapCursor(cursor);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
