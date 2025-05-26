package com.example.tomatosapp.activities;

import android.os.Bundle;
import android.view.View;
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

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

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
        addBotMessage("🍅 Bonjour! Je suis votre assistant spécialisé dans la protection des tomates et leurs maladies. " +
                "Je peux vous aider avec les maladies, parasites, carences nutritionnelles et conseils de culture. " +
                "Posez-moi vos questions!");

        sendButton.setOnClickListener(v -> sendMessage());

        // Load previous chat history
        loadChatHistory();
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty()) {
            // Add user message
            addUserMessage(message);
            messageInput.setText("");

            // Process and generate bot response
            generateBotResponse(message);
        }
    }

    private void addUserMessage(String message) {
        chatMessages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

        // Save to Firestore
        saveMessageToFirestore(message, true);
    }

    private void addBotMessage(String message) {
        chatMessages.add(new ChatMessage(message, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

        // Save to Firestore
        if (!message.contains("Bonjour! Je suis votre assistant")) { // Don't save welcome message repeatedly
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

        // Utiliser l'email comme ID du document utilisateur
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

        String userEmail = auth.getCurrentUser().getEmail();

        db.collection("utilisateurs").document(userEmail)
                .collection("chats")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(50) // Limiter aux 50 derniers messages
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

    private void generateBotResponse(String userMessage) {
        String query = userMessage.toLowerCase().trim();
        String response = "";

        // Maladies fongiques
        if (containsAny(query, "mildiou", "blight", "phytophthora")) {
            response = "🍄 **MILDIOU (Phytophthora infestans)**\n\n" +
                    "**Symptômes:** Taches brunes sur feuilles avec duvet blanc, brunissement des tiges\n\n" +
                    "**Traitement:**\n" +
                    "• Bouillie bordelaise (cuivre) en prévention\n" +
                    "• Éliminer parties infectées\n" +
                    "• Améliorer aération entre plants\n" +
                    "• Arroser au pied, éviter les feuilles\n" +
                    "• Paillis pour éviter éclaboussures\n\n" +
                    "**Prévention:** Rotation des cultures, variétés résistantes";
        }
        else if (containsAny(query, "alternariose", "alternaria", "tache concentrique")) {
            response = "🔄 **ALTERNARIOSE (Alternaria solani)**\n\n" +
                    "**Symptômes:** Taches brunes concentriques sur feuilles âgées\n\n" +
                    "**Traitement:**\n" +
                    "• Fongicides à base de cuivre\n" +
                    "• Suppression feuilles infectées\n" +
                    "• Renforcement plante (potassium)\n" +
                    "• Éviter stress hydrique";
        }
        else if (containsAny(query, "septoriose", "septoria", "tache grise")) {
            response = "⚪ **SEPTORIOSE (Septoria lycopersici)**\n\n" +
                    "**Symptômes:** Petites taches grises avec centre clair\n\n" +
                    "**Traitement:**\n" +
                    "• Fongicides préventifs\n" +
                    "• Élimination feuilles touchées\n" +
                    "• Espacement plants pour aération";
        }

        // Parasites
        else if (containsAny(query, "puceron", "aphid", "insecte vert")) {
            response = "🐛 **PUCERONS**\n\n" +
                    "**Identification:** Petits insects verts/noirs sous feuilles\n\n" +
                    "**Traitement:**\n" +
                    "• Savon noir dilué (pulvérisation)\n" +
                    "• Huile de neem\n" +
                    "• Coccinelles (auxiliaires)\n" +
                    "• Jet d'eau pour déloger\n\n" +
                    "**Prévention:** Plantes compagnes (basilic, œillets d'Inde)";
        }
        else if (containsAny(query, "araignée", "spider", "tétranyque", "acarien")) {
            response = "🕷️ **ARAIGNÉES ROUGES (Tétranyques)**\n\n" +
                    "**Symptômes:** Feuilles jaunies, toiles fines, points jaunes\n\n" +
                    "**Traitement:**\n" +
                    "• Augmenter humidité ambiante\n" +
                    "• Douches fréquentes sous feuilles\n" +
                    "• Acaricides naturels (huile blanche)\n" +
                    "• Prédateurs naturels (phytoséiules)";
        }
        else if (containsAny(query, "aleurode", "mouche blanche", "whitefly")) {
            response = "🦟 **ALEURODES (Mouches blanches)**\n\n" +
                    "**Symptômes:** Petites mouches blanches, feuilles collantes\n\n" +
                    "**Traitement:**\n" +
                    "• Pièges jaunes englués\n" +
                    "• Savon insecticide\n" +
                    "• Huile de neem\n" +
                    "• Encarsia formosa (auxiliaire)";
        }

        // Carences nutritionnelles
        else if (containsAny(query, "jaun", "yellow", "chlorose", "carence")) {
            response = "💛 **JAUNISSEMENT DES FEUILLES**\n\n" +
                    "**Causes possibles:**\n" +
                    "• Carence azote: Jaunissement général\n" +
                    "• Carence magnésium: Jaunissement entre nervures\n" +
                    "• Carence fer: Jeunes feuilles jaunes\n" +
                    "• Excès d'eau: Jaunissement + flétrissement\n\n" +
                    "**Solutions:**\n" +
                    "• Engrais équilibré NPK\n" +
                    "• Sulfate de magnésium (sel d'Epsom)\n" +
                    "• Chélate de fer si sol calcaire\n" +
                    "• Drainage si excès d'eau";
        }

        // Problèmes de fructification
        else if (containsAny(query, "fleur", "flower", "fruit", "pollinisation", "nouaison")) {
            response = "🌸 **PROBLÈMES DE FRUCTIFICATION**\n\n" +
                    "**Chute des fleurs:**\n" +
                    "• Températures extrêmes (>32°C ou <15°C)\n" +
                    "• Stress hydrique\n" +
                    "• Excès d'azote\n\n" +
                    "**Solutions:**\n" +
                    "• Ombrage si trop chaud\n" +
                    "• Arrosage régulier\n" +
                    "• Réduire engrais azoté\n" +
                    "• Pollinisation manuelle (pinceau)\n" +
                    "• Secouer plants le matin";
        }
        else if (containsAny(query, "pourri", "rot", "moisissure", "botrytis")) {
            response = "🦠 **POURRITURE DES FRUITS**\n\n" +
                    "**Types courants:**\n" +
                    "• Pourriture grise (Botrytis): Duvet gris\n" +
                    "• Pourriture apicale: Tache noire au bout\n\n" +
                    "**Traitement:**\n" +
                    "• Éliminer fruits atteints\n" +
                    "• Réduire humidité\n" +
                    "• Calcium si pourriture apicale\n" +
                    "• Fongicides préventifs\n" +
                    "• Éviter blessures aux fruits";
        }

        // Conseils généraux de culture
        else if (containsAny(query, "culture", "plantation", "conseil", "comment", "quand")) {
            response = "🌱 **CONSEILS DE CULTURE**\n\n" +
                    "**Plantation:**\n" +
                    "• Distance: 50-60cm entre plants\n" +
                    "• Sol: Riche, bien drainé, pH 6-7\n" +
                    "• Exposition: Soleil, à l'abri du vent\n\n" +
                    "**Entretien:**\n" +
                    "• Arrosage: Régulier au pied\n" +
                    "• Paillage: Conserver humidité\n" +
                    "• Tuteurage: Indispensable\n" +
                    "• Taille: Gourmands et feuilles basses\n\n" +
                    "**Fertilisation:** NPK équilibré + compost";
        }

        // Recherche par symptômes
        else if (containsAny(query, "tache", "spot", "marque")) {
            if (containsAny(query, "brun", "brown", "noir", "black")) {
                response = "🔍 **TACHES BRUNES/NOIRES**\n\n" +
                        "**Localisations possibles:**\n" +
                        "• Feuilles: Mildiou, Alternariose\n" +
                        "• Fruits: Anthracnose, coup de soleil\n" +
                        "• Tiges: Chancre, mildiou\n\n" +
                        "Pouvez-vous préciser où se trouvent les taches et leur aspect ?";
            } else {
                response = "🔍 **DIAGNOSTIC DES TACHES**\n\n" +
                        "Pour un diagnostic précis, décrivez-moi:\n" +
                        "• Couleur: brune, jaune, noire ?\n" +
                        "• Localisation: feuilles, fruits, tiges ?\n" +
                        "• Forme: ronde, irrégulière ?\n" +
                        "• Présence de duvet ou moisissure ?\n\n" +
                        "Ces détails m'aideront à identifier le problème!";
            }
        }

        // Réponse par défaut
        else {
            response = "🍅 **ASSISTANT TOMATES**\n\n" +
                    "Je suis spécialisé dans la protection des tomates. Je peux vous aider avec:\n\n" +
                    "🦠 **Maladies:** Mildiou, alternariose, septoriose\n" +
                    "🐛 **Parasites:** Pucerons, araignées rouges, aleurodes\n" +
                    "💛 **Carences:** Azote, potassium, magnésium\n" +
                    "🌸 **Fructification:** Pollinisation, chute des fleurs\n" +
                    "🌱 **Culture:** Plantation, entretien, fertilisation\n\n" +
                    "Décrivez-moi votre problème en détail pour un diagnostic précis!";
        }

        addBotMessage(response);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}