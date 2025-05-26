package com.example.tomatosapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.tomatosapp.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EnvironmentActivity extends AppCompatActivity {

    private LineChart temperatureChart, humidityChart;
    private DatabaseReference databaseRef;
    private TextView lastUpdateText;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_environment);

        // Initialisation des vues
        temperatureChart = findViewById(R.id.temperatureChart);
        humidityChart = findViewById(R.id.humidityChart);
        lastUpdateText = findViewById(R.id.lastUpdateText);

        // Configuration des graphiques
        setupChart(temperatureChart, "Température (°C)", Color.RED);
        setupChart(humidityChart, "Humidité (%)", Color.BLUE);

        // FIXED: Configure Firebase for Europe West 1 region
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://tomateapp-b5904-default-rtdb.europe-west1.firebasedatabase.app/");
        databaseRef = database.getReference("capteurs");

        // Test de connexion Firebase
        testFirebaseConnection();

        // Charger les données
        loadSensorData();
    }

    private void testFirebaseConnection() {
        // FIXED: Use the same regional database instance for connection testing
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://tomateapp-b5904-default-rtdb.europe-west1.firebasedatabase.app/");
        DatabaseReference connectedRef = database.getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                Log.d("Firebase", "Connecté: " + connected);
                if (!connected) {
                    Log.e("Firebase", "Pas de connexion à la base de données");
                } else {
                    Log.d("Firebase", "Connexion réussie à la base européenne!");
                    // Test if we can read the capteurs node
                    testDataAccess();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Listener connection annulé", error.toException());
            }
        });
    }

    // ADDED: Test data access to verify database structure
    private void testDataAccess() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("Firebase", "Structure capteurs existe: " + snapshot.exists());
                Log.d("Firebase", "Enfants capteurs: " + snapshot.getChildrenCount());
                for (DataSnapshot child : snapshot.getChildren()) {
                    Log.d("Firebase", "Clé trouvée: " + child.getKey() + " avec " + child.getChildrenCount() + " éléments");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Erreur test accès données", error.toException());
            }
        });
    }

    private void loadSensorData() {
        Log.d("Firebase", "Début du chargement des données");

        // Réinitialisation des graphiques
        temperatureChart.clear();
        humidityChart.clear();

        // FIXED: Chargement température avec le bon chemin
        databaseRef.child("temperature").orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Entry> entries = new ArrayList<>();
                        long latestTimestamp = 0;

                        Log.d("Firebase", "Données température reçues: " + snapshot.getChildrenCount());

                        for (DataSnapshot data : snapshot.getChildren()) {
                            try {
                                // IMPROVED: Better null checking and type conversion
                                Object valueObj = data.child("value").getValue();
                                Object timestampObj = data.child("timestamp").getValue();

                                Float value = null;
                                Long timestamp = null;

                                // Handle different number types for value
                                if (valueObj instanceof Double) {
                                    value = ((Double) valueObj).floatValue();
                                } else if (valueObj instanceof Float) {
                                    value = (Float) valueObj;
                                } else if (valueObj instanceof Long) {
                                    value = ((Long) valueObj).floatValue();
                                } else if (valueObj instanceof Integer) {
                                    value = ((Integer) valueObj).floatValue();
                                }

                                // Handle timestamp conversion
                                if (timestampObj instanceof Long) {
                                    timestamp = (Long) timestampObj;
                                    // FIXED: Convert to milliseconds if timestamp is in seconds
                                    if (timestamp < 1000000000000L) { // If less than year 2001 in milliseconds
                                        timestamp = timestamp * 1000; // Convert from seconds to milliseconds
                                    }
                                } else if (timestampObj instanceof Integer) {
                                    timestamp = ((Integer) timestampObj).longValue();
                                    if (timestamp < 1000000000000L) {
                                        timestamp = timestamp * 1000;
                                    }
                                }

                                if (value != null && timestamp != null) {
                                    entries.add(new Entry(timestamp, value));
                                    if (timestamp > latestTimestamp) {
                                        latestTimestamp = timestamp;
                                    }
                                    Log.d("FirebaseData", "Temp: " + value + " à " + new Date(timestamp));
                                } else {
                                    Log.w("Firebase", "Données invalides - value: " + valueObj + ", timestamp: " + timestampObj);
                                }
                            } catch (Exception e) {
                                Log.e("Firebase", "Erreur parsing température", e);
                            }
                        }

                        // Tri par timestamp
                        Collections.sort(entries, Comparator.comparing(Entry::getX));

                        updateChart(temperatureChart, entries, "Température", Color.RED);
                        updateLastUpdateTime(latestTimestamp);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Erreur température", error.toException());
                    }
                });

        // FIXED: Chargement humidité avec le bon chemin
        databaseRef.child("humidite").orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Entry> entries = new ArrayList<>();
                        long latestTimestamp = 0;

                        Log.d("Firebase", "Données humidité reçues: " + snapshot.getChildrenCount());

                        for (DataSnapshot data : snapshot.getChildren()) {
                            try {
                                // IMPROVED: Better null checking and type conversion
                                Object valueObj = data.child("value").getValue();
                                Object timestampObj = data.child("timestamp").getValue();

                                Float value = null;
                                Long timestamp = null;

                                // Handle different number types for value
                                if (valueObj instanceof Double) {
                                    value = ((Double) valueObj).floatValue();
                                } else if (valueObj instanceof Float) {
                                    value = (Float) valueObj;
                                } else if (valueObj instanceof Long) {
                                    value = ((Long) valueObj).floatValue();
                                } else if (valueObj instanceof Integer) {
                                    value = ((Integer) valueObj).floatValue();
                                }

                                // Handle timestamp conversion
                                if (timestampObj instanceof Long) {
                                    timestamp = (Long) timestampObj;
                                    // FIXED: Convert to milliseconds if timestamp is in seconds
                                    if (timestamp < 1000000000000L) { // If less than year 2001 in milliseconds
                                        timestamp = timestamp * 1000; // Convert from seconds to milliseconds
                                    }
                                } else if (timestampObj instanceof Integer) {
                                    timestamp = ((Integer) timestampObj).longValue();
                                    if (timestamp < 1000000000000L) {
                                        timestamp = timestamp * 1000;
                                    }
                                }

                                if (value != null && timestamp != null) {
                                    entries.add(new Entry(timestamp, value));
                                    if (timestamp > latestTimestamp) {
                                        latestTimestamp = timestamp;
                                    }
                                    Log.d("FirebaseData", "Humid: " + value + " à " + new Date(timestamp));
                                } else {
                                    Log.w("Firebase", "Données invalides - value: " + valueObj + ", timestamp: " + timestampObj);
                                }
                            } catch (Exception e) {
                                Log.e("Firebase", "Erreur parsing humidité", e);
                            }
                        }

                        // Tri par timestamp
                        Collections.sort(entries, Comparator.comparing(Entry::getX));

                        updateChart(humidityChart, entries, "Humidité", Color.BLUE);
                        updateLastUpdateTime(latestTimestamp);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Erreur humidité", error.toException());
                    }
                });
    }

    private void setupChart(LineChart chart, String label, int color) {
        chart.getDescription().setText(label);
        chart.getDescription().setTextSize(12f);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setNoDataText("Chargement des données...");
        chart.setNoDataTextColor(Color.GRAY);

        // Configuration axe X
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(3600000f); // 1 heure en ms
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                return timeFormat.format(new Date((long) value));
            }
        });
        xAxis.setLabelCount(5, true);

        // Configuration axe Y
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(label.contains("Temp") ? 10f : 0f);
        leftAxis.setAxisMaximum(label.contains("Temp") ? 40f : 100f);
        leftAxis.setGranularity(5f);
        leftAxis.setDrawGridLines(true);

        chart.getAxisRight().setEnabled(false);
    }

    private void updateChart(LineChart chart, List<Entry> entries, String label, int color) {
        if (entries.isEmpty()) {
            Log.e("Chart", "Aucune donnée pour " + label);
            chart.setNoDataText("Aucune donnée disponible");
            chart.invalidate();
            return;
        }

        Log.d("Chart", "Mise à jour " + label + " avec " + entries.size() + " points");

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Animation
        chart.animateY(1000);
        chart.invalidate();
    }

    private void updateLastUpdateTime(long timestamp) {
        if (timestamp > 0) {
            String updateText = "Dernière mise à jour: " + dateFormat.format(new Date(timestamp));
            lastUpdateText.setText(updateText);
            Log.d("UpdateTime", updateText);
        }
    }
}