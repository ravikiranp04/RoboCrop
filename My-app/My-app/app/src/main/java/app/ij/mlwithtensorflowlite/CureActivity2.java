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

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import app.ij.mlwithtensorflowlite.ml.ModelUnquant ;

public class CureActivity2 extends AppCompatActivity {
    int predictStatus=0;
    String predicted;
    TextView result1,resultcure;
    ImageView imageView1;
    Button picture1;
    private SearchView searchView1;
    private Button searchButton1;

    private SimpleCursorAdapter suggestionsAdapter;
    int imageSize = 224;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cure2);

        result1 = findViewById(R.id.result1);

        imageView1 = findViewById(R.id.imageView1);
        picture1 = findViewById(R.id.button1);
        resultcure=findViewById(R.id.resultcure);
        searchView1 = findViewById(R.id.searchView1);
        searchButton1 = findViewById(R.id.searchButton1);
        picture1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {

                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
        // Initialize suggestions adapter
        String[] from = new String[] { "disease_name" };
        int[] to = new int[] { android.R.id.text1 };
        suggestionsAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null,
                from,
                to,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        searchView1.setSuggestionsAdapter(suggestionsAdapter);
        searchView1.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
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
                    searchView1.setQuery(query, true);
                }
                return true;
            }
        });
        // query text changes
        searchView1.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Handle query submission
                displayDiseaseInfo(query,predicted);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Handle query text change
                filterAndDisplaySuggestions(newText);
                return false;
            }
        });
        // listener for the search button
        searchButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String query = searchView1.getQuery().toString();
                displayDiseaseInfo(query,predicted);
            }
        });
    }
    private void displayDiseaseInfo(String diseaseName, String predictedPlantName) {
        // Load JSON data
        String json = loadJSONFromAsset();


        if (json != null) {
            try {
                // Parse JSON data
                JSONObject jsonObject = new JSONObject(json);


                JSONObject diseaseObject = jsonObject.optJSONObject(diseaseName);

                resultcure.setText("");


                if(predictStatus==0){
                    resultcure.setText("Upload a Picture");
                }
                else if (diseaseObject != null && predictStatus==1) {

                    if (diseaseObject.has(predicted)) {
                        resultcure.append("Cure Found" + "\n\n");
                        JSONObject plantObject = diseaseObject.getJSONObject(predicted);
                        JSONArray methods = plantObject.getJSONArray("Method");

                        resultcure.append("Plant : "+predictedPlantName + "\n\n");
                        // Iterate
                        for (int i = 0; i < methods.length(); i++) {
                            String method = methods.getString(i);
                            resultcure.append(method + "\n\n");
                        }
                    }
                    else {

                        resultcure.setText(getString(R.string.cure_not_available));
                    }
                }
                else {

                    resultcure.setText(getString(R.string.cure_not_available));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            Log.e("displayDiseaseInfo", "JSON data is null");
            System.out.println("Json data is null");
        }
    }



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
        // Load JSON data
        String json = loadJSONFromAsset();


        if (json != null) {
            try {
                // Parse JSON data
                JSONObject jsonObject = new JSONObject(json);


                MatrixCursor cursor = new MatrixCursor(new String[] { "_id", "disease_name" });
                // Get all disease names
                Iterator<String> keys = jsonObject.keys();
                int id = 0;
                while (keys.hasNext()) {
                    String key = keys.next();
                    // Add the disease name to suggestions if it starts with the input text
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
    public void classifyImage(Bitmap image){
        try {
            ModelUnquant model = ModelUnquant.newInstance(getApplicationContext());


            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());


            int [] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());


            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);


            ModelUnquant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for(int i = 0; i < confidences.length; i++){
                if(confidences[i] > maxConfidence){
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"Aloevera", "amla fruit", "Amruthaballi", "ashwagandha roots","Bamboo","Betel","Bringaraja","castor root","Doddpathre","Drumstick","Ganike","hibiscus flower","lemon","mango flower","Mint","neem flower","papaya seeds","pepper seeds","sapota seeds","Tulsi"};
            result1.setText("");

            String s = "";
           /* for(int i = 0; i < classes.length; i++){
                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
            }*/
            s+=String.format("%s \n", classes[maxPos]);
           result1.append(s);
           predicted=classes[maxPos];
           predictStatus=1;
            System.out.println(predicted);

            model.close();
        } catch (IOException e) {
            result1.setText(("Could Not Predict... Please Try Again"));
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            imageView1.setImageBitmap(image);

            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
