package com.example.garbagescaner.maps;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.garbagescaner.R; 

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private WebView mapWebView;
    private Spinner materialSpinner;
    private static final String TAG = "MapActivity";
    private static final String API_KEY = "b1b2edbc-f67c-4ef5-886b-ad011dbebae6"; // Замените на ваш ключ 2ГИС
    private static final String BASE_URL = "https://catalog.api.2gis.com/3.0/items";

    // Центр Бишкека для начального отображения карты
    private static final double BISHKEK_LAT = 42.8746;
    private static final double BISHKEK_LON = 74.5698;

    // Строки поиска для разных материалов
    private static final String PLASTIC_QUERY = "пункт приема пластика";
    private static final String GLASS_QUERY = "пункт приема стекла";
    private static final String PAPER_QUERY = "пункт приема макулатуры";
    private static final String METAL_QUERY = "пункт приема металла";
    private static final String ELECTRONICS_QUERY = "пункт приема электроники";

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Инициализация WebView
        mapWebView = findViewById(R.id.mapWebView);
        mapWebView.getSettings().setJavaScriptEnabled(true);
        mapWebView.setWebViewClient(new WebViewClient());

        // Инициализация Spinner
        materialSpinner = findViewById(R.id.materialSpinner);
        setupSpinner();

        // Инициализация Volley для HTTP запросов
        requestQueue = Volley.newRequestQueue(this);

        // Загрузка начальной карты Бишкека
        loadInitialMap();
    }

    private void setupSpinner() {
        List<String> materials = new ArrayList<>();
        materials.add("Выберите материал");
        materials.add("Пластик");
        materials.add("Стекло");
        materials.add("Бумага");
        materials.add("Металл");
        materials.add("Электроника");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, materials);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        materialSpinner.setAdapter(adapter);

        materialSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Игнорируем первый элемент "Выберите материал"
                    String selectedMaterial = (String) parent.getItemAtPosition(position);
                    searchRecyclingPoints(selectedMaterial);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });
    }

    private void loadInitialMap() {
        // Загружаем карту 2ГИС через HTML
        String mapHtml = generateMapHtml(BISHKEK_LAT, BISHKEK_LON, 12, new ArrayList<>());
        mapWebView.loadDataWithBaseURL("https://maps.2gis.com/", mapHtml, "text/html", "UTF-8", null);
    }

    private void searchRecyclingPoints(String material) {
        String query;

        // Определяем строку поиска на основе выбранного материала
        switch (material) {
            case "Пластик":
                query = PLASTIC_QUERY;
                break;
            case "Стекло":
                query = GLASS_QUERY;
                break;
            case "Бумага":
                query = PAPER_QUERY;
                break;
            case "Металл":
                query = METAL_QUERY;
                break;
            case "Электроника":
                query = ELECTRONICS_QUERY;
                break;
            default:
                query = "пункт приема";
                break;
        }

        // Выполняем запрос к API 2ГИС
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = BASE_URL + "?q=" + encodedQuery +
                    "&location=" + BISHKEK_LON + "," + BISHKEK_LAT +
                    "&radius=15000&fields=items.point&key=" + API_KEY;

            Log.d(TAG, "API URL: " + url);

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                List<RecyclingPoint> points = parseResponse(response);
                                updateMap(points);
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                Toast.makeText(MapActivity.this,
                                        "Ошибка обработки данных", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "API Error: " + error.toString());
                            Toast.makeText(MapActivity.this,
                                    "Ошибка получения данных", Toast.LENGTH_SHORT).show();
                        }
                    });

            requestQueue.add(stringRequest);

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "URL encoding error: " + e.getMessage());
        }
    }

    private List<RecyclingPoint> parseResponse(String jsonResponse) throws JSONException {
        List<RecyclingPoint> points = new ArrayList<>();

        JSONObject response = new JSONObject(jsonResponse);
        JSONObject result = response.getJSONObject("result");

        if (!result.has("items")) {
            return points;
        }

        JSONArray items = result.getJSONArray("items");

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);

            String id = item.getString("id");
            String name = item.getString("name");
            String address = item.optString("address_name", "");

            double lat = 0;
            double lon = 0;

            if (item.has("point")) {
                JSONObject point = item.getJSONObject("point");
                lon = point.getDouble("lon");
                lat = point.getDouble("lat");
            }

            RecyclingPoint recyclingPoint = new RecyclingPoint(id, name, address, lat, lon);
            points.add(recyclingPoint);
        }

        return points;
    }

    private void updateMap(List<RecyclingPoint> points) {
        String mapHtml = generateMapHtml(BISHKEK_LAT, BISHKEK_LON, 12, points);
        mapWebView.loadDataWithBaseURL("https://maps.2gis.com/", mapHtml, "text/html", "UTF-8", null);
    }

    private String generateMapHtml(double centerLat, double centerLon, int zoom, List<RecyclingPoint> points) {
        StringBuilder markersJs = new StringBuilder();

        for (RecyclingPoint point : points) {
            markersJs.append("DG.marker([")
                    .append(point.getLat()).append(", ")
                    .append(point.getLon())
                    .append("])")
                    .append(".addTo(map)")
                    .append(".bindPopup('")
                    .append("<strong>").append(escapeJsString(point.getName())).append("</strong><br>")
                    .append(escapeJsString(point.getAddress()))
                    .append("');\n");
        }

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <script src=\"https://maps.api.2gis.ru/2.0/loader.js?pkg=full\"></script>\n" +
                "    <style>\n" +
                "        html, body, #map {\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "            padding: 0;\n" +
                "            margin: 0;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "    <script>\n" +
                "        DG.then(function() {\n" +
                "            map = DG.map('map', {\n" +
                "                center: [" + centerLat + ", " + centerLon + "],\n" +
                "                zoom: " + zoom + "\n" +
                "            });\n" +
                markersJs.toString() +
                "        });\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private String escapeJsString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("'", "\\'").replace("\n", "\\n");
    }

    // Класс для хранения данных о пунктах утилизации
    private static class RecyclingPoint {
        private final String id;
        private final String name;
        private final String address;
        private final double lat;
        private final double lon;

        public RecyclingPoint(String id, String name, String address, double lat, double lon) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lon = lon;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }
    }
}