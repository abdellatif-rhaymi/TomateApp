package com.example.tomatosapp.activities;

import android.os.Bundle;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tomatosapp.R;
import com.example.tomatosapp.adapters.ChatAdapter;
import com.example.tomatosapp.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import okhttp3.OkHttpClient;


public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize HTTP client
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // Initialize views
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        // Setup RecyclerView
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Add welcome message
        addBotMessage("🍅 Bonjour! Je suis votre assistant IA gratuit spécialisé dans la culture des tomates. " +
                "Je peux vous aider avec les maladies, parasites, techniques de culture, et conseils pratiques. " +
                "Posez-moi vos questions!");

        sendButton.setOnClickListener(v -> sendMessage());
        loadChatHistory();
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty()) {
            addUserMessage(message);
            messageInput.setText("");

            // Utiliser l'API gratuite choisie
            generateBotResponseWithFreeAPI(message);
        }
    }

    // OPTION 3: API locale avec base de connaissances (TOTALEMENT GRATUITE)
    private void generateBotResponseWithLocalKnowledge(String userMessage) {
        final String query = userMessage.toLowerCase().trim();
        String response = "";

        // Base de connaissances étendue et améliorée
        if (containsAny(query, "mildiou", "blight", "phytophthora", "taches brunes feuilles")) {
            response = "🍄 **MILDIOU - Phytophthora infestans**\n\n" +
                    "**Symptômes:**\n" +
                    "• Taches brunes irrégulières sur feuilles\n" +
                    "• Duvet blanc sous les feuilles par temps humide\n" +
                    "• Brunissement et dessèchement rapide\n" +
                    "• Taches noires sur tiges et fruits\n\n" +
                    "**Traitement naturel:**\n" +
                    "• Bouillie bordelaise (cuivre) en prévention\n" +
                    "• Décoction de prêle (renforce les tissus)\n" +
                    "• Bicarbonate de soude (1g/L d'eau)\n" +
                    "• Éliminer immédiatement les parties touchées\n\n" +
                    "**Prévention:**\n" +
                    "• Espacer les plants (60cm minimum)\n" +
                    "• Arroser au pied uniquement\n" +
                    "• Pailler le sol pour éviter les éclaboussures\n" +
                    "• Planter des variétés résistantes\n" +
                    "• Éviter l'arrosage en soirée";
        }
        else if (containsAny(query, "puceron", "pucerons", "insectes verts", "petits insectes")) {
            response = "🐛 **PUCERONS**\n\n" +
                    "**Identification:**\n" +
                    "• Petits insectes verts, noirs ou blancs\n" +
                    "• Se regroupent sous les feuilles\n" +
                    "• Feuilles qui se recroquevillent\n" +
                    "• Présence de miellat (substance collante)\n\n" +
                    "**Traitements naturels:**\n" +
                    "• Savon noir: 2 cuillères à soupe/L d'eau\n" +
                    "• Huile de neem: efficace et bio\n" +
                    "• Jet d'eau puissant le matin\n" +
                    "• Coccinelles (prédateurs naturels)\n\n" +
                    "**Répulsifs naturels:**\n" +
                    "• Planter basilic et œillets d'Inde à proximité\n" +
                    "• Pulvérisation d'ail macéré\n" +
                    "• Purins d'ortie dilués";
        }
        else if (containsAny(query, "jaunissement", "feuilles jaunes", "chlorose", "carence")) {
            response = "💛 **JAUNISSEMENT DES FEUILLES**\n\n" +
                    "**Diagnostic selon la localisation:**\n\n" +
                    "**Feuilles du bas qui jaunissent:**\n" +
                    "• Normal en fin de saison\n" +
                    "• Manque d'azote si généralisé\n" +
                    "• Solution: engrais azoté modéré\n\n" +
                    "**Jaunissement entre les nervures:**\n" +
                    "• Carence en magnésium\n" +
                    "• Solution: sel d'Epsom (sulfate de magnésium)\n" +
                    "• Dosage: 1 cuillère à café/L d'eau\n\n" +
                    "**Jeunes feuilles jaunes:**\n" +
                    "• Carence en fer (chlorose ferrique)\n" +
                    "• Solution: chélate de fer\n" +
                    "• Améliorer le drainage si sol trop humide\n\n" +
                    "**Jaunissement avec flétrissement:**\n" +
                    "• Excès d'eau ou problème racinaire\n" +
                    "• Réduire l'arrosage et vérifier le drainage";
        }
        else if (containsAny(query, "tomates qui ne poussent pas", "croissance lente", "petites tomates")) {
            response = "🌱 **PROBLÈMES DE CROISSANCE**\n\n" +
                    "**Causes possibles:**\n\n" +
                    "**Sol pauvre:**\n" +
                    "• Apporter compost bien décomposé\n" +
                    "• Engrais organique NPK équilibré\n" +
                    "• Paillis nutritif (compost, fumier)\n\n" +
                    "**Manque de lumière:**\n" +
                    "• Minimum 6h de soleil direct\n" +
                    "• Tailler les branches qui font de l'ombre\n" +
                    "• Éviter les emplacements trop ombragés\n\n" +
                    "**Stress hydrique:**\n" +
                    "• Arrosage régulier et profond\n" +
                    "• Pailler pour conserver l'humidité\n" +
                    "• Éviter les arrosages superficiels\n\n" +
                    "**Températures inadéquates:**\n" +
                    "• Optimum: 20-25°C le jour, 15-18°C la nuit\n" +
                    "• Protection contre le froid/canicule";
        }
        else if (containsAny(query, "pollinisation", "fleurs tombent", "pas de fruits", "nouaison")) {
            response = "🌸 **PROBLÈMES DE POLLINISATION**\n\n" +
                    "**Causes de chute des fleurs:**\n\n" +
                    "**Températures extrêmes:**\n" +
                    "• Trop chaud (>32°C): ombrage à midi\n" +
                    "• Trop froid (<15°C): protection nocturne\n" +
                    "• Optimum: 20-25°C\n\n" +
                    "**Stress hydrique:**\n" +
                    "• Arrosage irrégulier = chute des fleurs\n" +
                    "• Maintenir humidité constante du sol\n\n" +
                    "**Excès d'azote:**\n" +
                    "• Trop de feuillage, peu de fleurs\n" +
                    "• Réduire les engrais azotés\n" +
                    "• Privilégier phosphore et potassium\n\n" +
                    "**Solutions pour améliorer la pollinisation:**\n" +
                    "• Secouer délicatement les plants le matin\n" +
                    "• Pollinisation manuelle avec pinceau\n" +
                    "• Attirer les pollinisateurs (fleurs mellifères à proximité)";
        }
        else if (containsAny(query, "culture", "plantation", "quand planter", "comment cultiver")) {
            response = "🌱 **GUIDE DE CULTURE DES TOMATES**\n\n" +
                    "**Plantation:**\n" +
                    "• Période: après les dernières gelées (mi-mai)\n" +
                    "• Distance: 60-80cm entre plants\n" +
                    "• Profondeur: enterrer 2/3 de la tige\n" +
                    "• Exposition: plein soleil, à l'abri du vent\n\n" +
                    "**Préparation du sol:**\n" +
                    "• Sol riche, bien drainé, pH 6-7\n" +
                    "• Apport de compost ou fumier décomposé\n" +
                    "• Bêchage profond (20-30cm)\n\n" +
                    "**Entretien régulier:**\n" +
                    "• Arrosage: 2-3 fois/semaine au pied\n" +
                    "• Paillage: paille, tontes, compost\n" +
                    "• Tuteurage: indispensable dès plantation\n" +
                    "• Taille des gourmands: hebdomadaire\n" +
                    "• Suppression feuilles basses touchant le sol\n\n" +
                    "**Fertilisation:**\n" +
                    "• Engrais riche en potassium pour la fructification\n" +
                    "• Apports réguliers mais modérés";
        }
        else {
            response = "🍅 **ASSISTANT TOMATES - IA GRATUITE**\n\n" +
                    "Je suis votre expert gratuit en culture de tomates! 🤖\n\n" +
                    "**Mes spécialités:**\n" +
                    "🦠 **Maladies:** Mildiou, alternariose, fusariose\n" +
                    "🐛 **Parasites:** Pucerons, araignées rouges, aleurodes\n" +
                    "💛 **Carences:** Azote, potassium, magnésium, fer\n" +
                    "🌸 **Fructification:** Pollinisation, nouaison\n" +
                    "🌱 **Culture:** Plantation, entretien, taille\n" +
                    "🍅 **Variétés:** Conseils selon votre région\n\n" +
                    "**Exemples de questions:**\n" +
                    "• 'Mes feuilles ont des taches brunes'\n" +
                    "• 'Les fleurs tombent sans faire de fruits'\n" +
                    "• 'Mes tomates jaunissent'\n" +
                    "• 'Comment traiter les pucerons naturellement?'\n\n" +
                    "Décrivez-moi votre problème en détail! 🔍";
        }

        addBotMessage(response);
    }

    private void generateBotResponseWithFreeAPI(String userMessage) {
        generateBotResponseWithLocalKnowledge(userMessage);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void addUserMessage(String message) {
        chatMessages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        saveMessageToFirestore(message, true);
    }

    private void addBotMessage(String message) {
        chatMessages.add(new ChatMessage(message, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

        if (!message.contains("Bonjour! Je suis votre assistant")) {
            saveMessageToFirestore(message, false);
        }
    }

    private void saveMessageToFirestore(String message, boolean isUser) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = auth.getCurrentUser().getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Email utilisateur introuvable", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("message", message);
        chatData.put("isUser", isUser);
        chatData.put("timestamp", System.currentTimeMillis());
        chatData.put("date", new java.util.Date());

        db.collection("utilisateurs").document(userEmail)
                .collection("chats").add(chatData)
                .addOnSuccessListener(documentReference -> {
                    // Message sauvegardé avec succès
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de sauvegarde: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadChatHistory() {
        if (auth.getCurrentUser() == null || auth.getCurrentUser().getEmail() == null) {
            return;
        }

        final String userEmail = auth.getCurrentUser().getEmail();

        db.collection("utilisateurs").document(userEmail)
                .collection("chats")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chatMessages.clear();
                    queryDocumentSnapshots.forEach(document -> {
                        String message = document.getString("message");
                        Boolean isUser = document.getBoolean("isUser");
                        if (message != null && isUser != null) {
                            chatMessages.add(new ChatMessage(message, isUser));
                        }
                    });
                    chatAdapter.notifyDataSetChanged();
                    if (!chatMessages.isEmpty()) {
                        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}