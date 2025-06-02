package com.example.tomatosapp.activities;

import android.os.Bundle;
import android.util.Log;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";

    // Google Gemini API Configuration
    private static final String GEMINI_API_KEY = "";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY;

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private OkHttpClient httpClient;

    // Enhanced conversation management
    private List<Map<String, String>> conversationHistory;
    private FlexibleKnowledgeBase knowledgeBase;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize enhanced components
        conversationHistory = new ArrayList<>();
        knowledgeBase = new FlexibleKnowledgeBase();
        random = new Random();

        // Initialize HTTP client with enhanced configuration
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
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

        // Add enhanced welcome message
        addBotMessage(knowledgeBase.getWelcomeMessage());

        sendButton.setOnClickListener(v -> sendMessage());
        loadChatHistory();
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty()) {
            addUserMessage(message);
            messageInput.setText("");
            sendButton.setEnabled(false);

            // Enhanced response generation with context awareness
            generateEnhancedBotResponse(message);
        }
    }

    private void generateEnhancedBotResponse(String userMessage) {
        // First, try to get a contextual response from local knowledge
        String contextualResponse = knowledgeBase.getContextualResponse(userMessage, conversationHistory);

        if (!contextualResponse.isEmpty()) {
            // If we have a good local response, use it but still try to enhance with Gemini
            generateBotResponseWithGemini(userMessage, contextualResponse);
        } else {
            // No local context, rely on Gemini
            generateBotResponseWithGemini(userMessage, null);
        }
    }

    private void generateBotResponseWithGemini(String userMessage, String fallbackResponse) {
        try {
            JSONObject requestBody = createEnhancedGeminiRequestBody(userMessage);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Gemini API call failed", e);
                    runOnUiThread(() -> {
                        sendButton.setEnabled(true);
                        if (fallbackResponse != null) {
                            addBotMessage("🌱 " + fallbackResponse);
                        } else {
                            fallbackToEnhancedLocalKnowledge(userMessage);
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> sendButton.setEnabled(true));

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Gemini API error: " + response.code());
                        runOnUiThread(() -> {
                            if (fallbackResponse != null) {
                                addBotMessage("🌱 " + fallbackResponse);
                            } else {
                                fallbackToEnhancedLocalKnowledge(userMessage);
                            }
                        });
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        String botResponse = parseGeminiResponse(responseBody);

                        if (!botResponse.isEmpty()) {
                            addToConversationHistory(userMessage, botResponse);
                            runOnUiThread(() -> addBotMessage("🤖 " + botResponse));
                        } else {
                            runOnUiThread(() -> {
                                if (fallbackResponse != null) {
                                    addBotMessage("🌱 " + fallbackResponse);
                                } else {
                                    fallbackToEnhancedLocalKnowledge(userMessage);
                                }
                            });
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing Gemini response", e);
                        runOnUiThread(() -> {
                            if (fallbackResponse != null) {
                                addBotMessage("🌱 " + fallbackResponse);
                            } else {
                                fallbackToEnhancedLocalKnowledge(userMessage);
                            }
                        });
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error creating Gemini request", e);
            sendButton.setEnabled(true);
            if (fallbackResponse != null) {
                addBotMessage("🌱 " + fallbackResponse);
            } else {
                fallbackToEnhancedLocalKnowledge(userMessage);
            }
        }
    }

    private JSONObject createEnhancedGeminiRequestBody(String userMessage) throws JSONException {
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();

        // Enhanced system prompt with more flexibility
        String systemPrompt = "Tu es un expert en jardinage passionné, spécialisé dans la culture des tomates. " +
                "Tu adoptes un ton bienveillant et encourageant. Tu personnalises tes réponses selon le niveau " +
                "d'expérience apparent de l'utilisateur. Tu donnes des conseils pratiques, adaptés aux conditions " +
                "locales quand c'est possible. Tu peux faire des liens avec d'autres aspects du jardinage. " +
                "Si la question sort du jardinage, tu la redirige avec tact vers ton domaine tout en restant utile. " +
                "Structure tes réponses de manière claire avec des sections distinctes quand c'est approprié.";

        // Add conversation context with better weighting
        int historyStart = Math.max(0, conversationHistory.size() - 4);
        for (int i = historyStart; i < conversationHistory.size(); i++) {
            Map<String, String> exchange = conversationHistory.get(i);

            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            JSONArray userParts = new JSONArray();
            JSONObject userText = new JSONObject();
            userText.put("text", exchange.get("user"));
            userParts.put(userText);
            userContent.put("parts", userParts);
            contents.put(userContent);

            JSONObject assistantContent = new JSONObject();
            assistantContent.put("role", "model");
            JSONArray assistantParts = new JSONArray();
            JSONObject assistantText = new JSONObject();
            assistantText.put("text", exchange.get("assistant"));
            assistantParts.put(assistantText);
            assistantContent.put("parts", assistantParts);
            contents.put(assistantContent);
        }

        // Current message with enhanced context
        JSONObject currentUserContent = new JSONObject();
        currentUserContent.put("role", "user");
        JSONArray currentUserParts = new JSONArray();
        JSONObject currentUserText = new JSONObject();

        String fullPrompt = conversationHistory.isEmpty() ?
                systemPrompt + "\n\nQuestion: " + userMessage : userMessage;

        currentUserText.put("text", fullPrompt);
        currentUserParts.put(currentUserText);
        currentUserContent.put("parts", currentUserParts);
        contents.put(currentUserContent);

        requestBody.put("contents", contents);

        // Enhanced generation configuration for more creative responses
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.8); // Increased for more creativity
        generationConfig.put("topK", 50); // Increased for more variety
        generationConfig.put("topP", 0.9);
        generationConfig.put("maxOutputTokens", 1500); // Increased for more detailed responses
        requestBody.put("generationConfig", generationConfig);

        // Safety settings
        JSONArray safetySettings = new JSONArray();
        String[] categories = {
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT"
        };

        for (String category : categories) {
            JSONObject safety = new JSONObject();
            safety.put("category", category);
            safety.put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            safetySettings.put(safety);
        }

        requestBody.put("safetySettings", safetySettings);
        return requestBody;
    }

    private String parseGeminiResponse(String responseBody) throws JSONException {
        JSONObject jsonResponse = new JSONObject(responseBody);

        if (jsonResponse.has("candidates") &&
                jsonResponse.getJSONArray("candidates").length() > 0) {

            JSONObject candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);

            if (candidate.has("finishReason") &&
                    candidate.getString("finishReason").equals("SAFETY")) {
                return knowledgeBase.getSafetyResponse();
            }

            if (candidate.has("content") &&
                    candidate.getJSONObject("content").has("parts") &&
                    candidate.getJSONObject("content").getJSONArray("parts").length() > 0) {

                JSONObject part = candidate.getJSONObject("content")
                        .getJSONArray("parts").getJSONObject(0);

                if (part.has("text")) {
                    String text = part.getString("text").trim();
                    return enhanceResponse(text);
                }
            }
        }

        return "";
    }

    private String enhanceResponse(String response) {
        // Clean markdown but preserve structure
        response = response.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        response = response.replaceAll("\\*(.*?)\\*", "$1");
        response = response.replaceAll("```[\\s\\S]*?```", "");
        response = response.replaceAll("`(.*?)`", "$1");
        response = response.replaceAll("\\n{3,}", "\n\n");
        response = response.trim();

        // Smart truncation that preserves meaning
        if (response.length() > 1200) {
            int lastSentence = response.lastIndexOf('.', 1200);
            if (lastSentence > 800) {
                response = response.substring(0, lastSentence + 1);
            } else {
                response = response.substring(0, 1200) + "...";
            }
        }

        return response;
    }

    private void fallbackToEnhancedLocalKnowledge(String userMessage) {
        String response = knowledgeBase.getFlexibleResponse(userMessage, conversationHistory);
        addBotMessage(response);
    }

    // Inner class for enhanced knowledge management
    private class FlexibleKnowledgeBase {
        private Map<String, List<String>> responses;
        private Map<String, String[]> keywords;
        private List<String> welcomeMessages;
        private List<String> encouragements;

        public FlexibleKnowledgeBase() {
            initializeResponses();
            initializeKeywords();
            initializeVariations();
        }

        private void initializeResponses() {
            responses = new HashMap<>();

            // Disease responses with variations
            responses.put("mildiou", Arrays.asList(
                    "🍄 **MILDIOU DÉTECTÉ**\n\n**Diagnostic rapide:**\nLe mildiou est reconnaissable par ses taches brunes irrégulières sur les feuilles, souvent accompagnées d'un duvet blanchâtre en dessous. Cette maladie fongique adore l'humidité!\n\n**Action immédiate:**\n• Supprimez toutes les parties atteintes et brûlez-les\n• Pulvérisez de la bouillie bordelaise le soir\n• Améliorez l'aération entre vos plants\n\n**Prévention future:**\nPensez au paillage pour éviter les éclaboussures de terre, et arrosez uniquement au pied de vos tomates. Un espacement de 60cm minimum entre plants aide beaucoup!",

                    "🌿 **LUTTE CONTRE LE MILDIOU**\n\n**Situation critique mais gérable!**\nLe mildiou progresse vite, mais avec les bons gestes, vous pouvez sauver vos tomates.\n\n**Solutions naturelles efficaces:**\n• Décoction de prêle (excellent préventif)\n• Bicarbonate de soude: 5g par litre d'eau\n• Lait écrémé dilué à 10% (surprenant mais efficace!)\n\n**Conseil d'expert:**\nTraitez en prévention dès que l'humidité s'installe. Un climat sec pendant 3-4 jours peut stopper la progression naturellement.",

                    "⚡ **ALERTE MILDIOU - GUIDE COMPLET**\n\n**Phase 1 - Diagnostic:**\nTaches brunes + duvet blanc + propagation rapide = mildiou confirmé\n\n**Phase 2 - Traitement d'urgence:**\n• Élimination chirurgicale des parties atteintes\n• Traitement fongicide naturel immédiat\n• Modification des conditions environnementales\n\n**Phase 3 - Surveillance:**\nInspection quotidienne pendant 2 semaines\n\n**Astuce pro:** Plantez du basilic entre vos tomates, il repousse naturellement certains champignons!"
            ));

            responses.put("puceron", Arrays.asList(
                    "🐛 **INVASION DE PUCERONS**\n\n**Identification confirmée:**\nCes petits vampires verts, noirs ou blancs adorent les jeunes pousses tendres. Ils affaiblissent vos plants en suçant leur sève.\n\n**Riposte naturelle:**\n• Jet d'eau froide puissant (matin de préférence)\n• Savon noir: 2 cuillères à soupe par litre\n• Huile de neem en pulvérisation nocturne\n\n**Alliés précieux:**\nLes coccinelles sont vos meilleures amies! Une coccinelle mange 150 pucerons par jour. Attirez-les avec des capucines et des fenouils.",

                    "🌱 **GUERRE BIOLOGIQUE CONTRE LES PUCERONS**\n\n**Stratégie écologique:**\nPas de panique! Ces parasites ont des ennemis naturels très efficaces.\n\n**Arsenal vert:**\n• Purin d'ortie dilué (1:10) - répulsif puissant\n• Infusion d'ail (3 gousses/litre, infuser 24h)\n• Plantation de basilic et menthe à proximité\n\n**Technique de grand-mère validée:**\nDéposez de la cendre de bois au pied des plants. Elle perturbe les pucerons et nourrit la terre en potasse!",

                    "🔄 **CYCLE DE TRAITEMENT ANTI-PUCERONS**\n\n**Jour 1-3:** Nettoyage mécanique (jet d'eau)\n**Jour 4-7:** Traitement savon noir le soir\n**Jour 8-14:** Surveillance et traitement localisé si besoin\n\n**Prévention intelligente:**\nLes pucerons détestent les odeurs fortes. Plantez thym, lavande et romarin autour de votre potager. Bonus: vous aurez des herbes aromatiques fraîches!"
            ));

            responses.put("arrosage", Arrays.asList(
                    "💧 **MAÎTRISE DE L'ARROSAGE**\n\n**Règle d'or:** Mieux vaut arroser moins souvent mais en profondeur!\n\n**Timing parfait:**\n• Matin (6h-9h): idéal, les plants ont toute la journée pour sécher\n• Soir (après 18h): acceptable si nécessaire\n• JAMAIS en plein soleil!\n\n**Technique professionnelle:**\nCreusez une cuvette autour de chaque pied, arrosez lentement jusqu'à ce que l'eau ne s'infiltre plus. Comptez 10-15L par plant adulte, 2-3 fois par semaine.",

                    "🌊 **SCIENCE DE L'HYDRATATION**\n\n**Test de terrain:**\nEnfoncez votre doigt à 5cm de profondeur près du pied. Si c'est sec, arrosez. Si c'est humide, attendez!\n\n**Paillage magique:**\nÉtalez 5-10cm de paille, tontes de gazon ou feuilles mortes. Vous diviserez vos besoins en eau par 2 et éliminerez 80% du désherbage!\n\n**Astuce récup':**\nRécupérez l'eau de cuisson des légumes (refroidie). Elle est riche en minéraux que vos tomates adorent!",

                    "⚖️ **ÉQUILIBRE HYDRIQUE PARFAIT**\n\n**Signes de soif:**\n• Feuilles qui pendent à 10h du matin\n• Croissance ralentie\n• Fruits qui se fendent (stress hydrique)\n\n**Système goutte-à-goutte fait maison:**\nPercez des bouteilles plastique, enterrez-les à côté des plants. Remplissage 1-2 fois par semaine selon la météo.\n\n**Conseil météo:**\nAugmentez de 50% par temps chaud et venteux, réduisez par temps couvert et humide."
            ));
        }

        private void initializeKeywords() {
            keywords = new HashMap<>();
            keywords.put("mildiou", new String[]{"mildiou", "blight", "phytophthora", "taches brunes", "duvet blanc", "maladie fongique", "champignon"});
            keywords.put("puceron", new String[]{"puceron", "pucerons", "insectes verts", "parasites", "feuilles enroulées", "miellat", "colonies"});
            keywords.put("arrosage", new String[]{"arrosage", "eau", "irrigation", "sécheresse", "humidité", "paillage", "goutte"});
            keywords.put("culture", new String[]{"plantation", "culture", "croissance", "entretien", "sol", "terreau", "semis"});
            keywords.put("recolte", new String[]{"récolte", "maturité", "rouge", "mûr", "cueillette", "conservation"});
        }

        private void initializeVariations() {
            welcomeMessages = Arrays.asList(
                    "🍅 Salut jardinier! Je suis votre expert tomates, propulsé par l'IA mais nourri de passion pour le jardinage! Que cultivons-nous aujourd'hui?",
                    "🌱 Bonjour! Votre conseiller tomates personnel est là! Prêt à transformer votre jardin en paradis de la tomate rouge?",
                    "🌿 Hey! Expert tomates à votre service! Des questions sur vos plants? Des défis au potager? Je suis là pour vous accompagner!"
            );

            encouragements = Arrays.asList(
                    "Excellent choix de sujet! 🌟",
                    "Parfait, parlons jardinage! 🌱",
                    "Bonne question, j'adore! 🍅",
                    "C'est parti pour du jardinage efficace! 💪"
            );
        }

        public String getWelcomeMessage() {
            return welcomeMessages.get(random.nextInt(welcomeMessages.size()));
        }

        public String getSafetyResponse() {
            return "🌿 Restons dans le jardinage! Je suis là pour vous aider avec vos tomates et votre potager. Quelle est votre préoccupation jardinière du moment?";
        }

        public String getContextualResponse(String userMessage, List<Map<String, String>> history) {
            String query = userMessage.toLowerCase().trim();

            // Check for follow-up questions
            if (history.size() > 0) {
                Map<String, String> lastExchange = history.get(history.size() - 1);
                String lastResponse = lastExchange.get("assistant").toLowerCase();

                if (containsAny(query, "merci", "ok", "d'accord") &&
                        containsAny(lastResponse, "mildiou", "puceron", "arrosage")) {
                    return "Parfait! N'hésitez pas si vous avez d'autres questions. Le jardinage, c'est un apprentissage constant! 🌱";
                }

                if (containsAny(query, "comment", "pourquoi", "quand") &&
                        containsAny(lastResponse, "solution", "traitement")) {
                    return getDetailedFollowUp(lastResponse, query);
                }
            }

            // Return empty if no contextual response found
            return "";
        }

        public String getFlexibleResponse(String userMessage, List<Map<String, String>> history) {
            String query = userMessage.toLowerCase().trim();

            // Try to match with knowledge base
            for (Map.Entry<String, String[]> entry : keywords.entrySet()) {
                if (containsAny(query, entry.getValue())) {
                    List<String> possibleResponses = responses.get(entry.getKey());
                    if (possibleResponses != null && !possibleResponses.isEmpty()) {
                        // Select response based on conversation history to avoid repetition
                        return selectVariedResponse(possibleResponses, history);
                    }
                }
            }

            // Fallback for unrecognized queries
            return getSmartFallback(query);
        }

        private String selectVariedResponse(List<String> possibleResponses, List<Map<String, String>> history) {
            // If no history, return first response
            if (history.isEmpty()) {
                return possibleResponses.get(0);
            }

            // Try to find a response not recently used
            for (String response : possibleResponses) {
                boolean recentlyUsed = false;
                for (int i = Math.max(0, history.size() - 3); i < history.size(); i++) {
                    if (history.get(i).get("assistant").contains(response.substring(0, Math.min(50, response.length())))) {
                        recentlyUsed = true;
                        break;
                    }
                }
                if (!recentlyUsed) {
                    return response;
                }
            }

            // If all recently used, return random
            return possibleResponses.get(random.nextInt(possibleResponses.size()));
        }

        private String getDetailedFollowUp(String lastResponse, String query) {
            if (containsAny(lastResponse, "mildiou") && containsAny(query, "quand", "comment")) {
                return "🕐 **TIMING MILDIOU:**\nTraitez dès les premiers signes (taches jaunes). En prévention: chaque semaine par temps humide. Température idéale du mildiou: 15-25°C + humidité > 80%.";
            }
            if (containsAny(lastResponse, "arrosage") && containsAny(query, "combien", "quantité")) {
                return "📏 **DOSAGE PRÉCIS:**\n• Jeunes plants: 2-3L tous les 2 jours\n• Plants adultes: 10-15L, 2 fois/semaine\n• Par forte chaleur: +50%\n• Test: l'eau doit pénétrer à 30cm de profondeur";
            }
            return "";
        }

        private String getSmartFallback(String query) {
            String[] greetings = {"bonjour", "salut", "hello", "bonsoir"};
            String[] thanks = {"merci", "thanks", "génial", "parfait"};

            if (containsAny(query, greetings)) {
                return encouragements.get(random.nextInt(encouragements.size())) +
                        " Comment vont vos tomates aujourd'hui?";
            }

            if (containsAny(query, thanks)) {
                return "Avec plaisir! 😊 Le jardinage, c'est encore mieux quand on partage les connaissances. D'autres questions?";
            }

            // Enhanced fallback with suggestions
            return "🍅 **MODE EXPERT ACTIVÉ**\n\n" +
                    "Je ne saisis pas exactement votre question, mais je peux vous aider avec:\n\n" +
                    "🦠 **Problèmes courants:** mildiou, pucerons, maladies\n" +
                    "💧 **Techniques d'arrosage:** fréquence, quantité, méthodes\n" +
                    "🌱 **Culture:** plantation, croissance, entretien\n" +
                    "🍅 **Récolte:** timing, conservation, astuces\n\n" +
                    "**Reformulez votre question** ou décrivez précisément votre situation. " +
                    "Plus vous donnez de détails, plus je peux vous aider efficacement! 🎯";
        }

        private boolean containsAny(String text, String... keywords) {
            for (String keyword : keywords) {
                if (text.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
    }

    private void addToConversationHistory(String userMessage, String botResponse) {
        Map<String, String> exchange = new HashMap<>();
        exchange.put("user", userMessage);
        exchange.put("assistant", botResponse);
        conversationHistory.add(exchange);

        // Keep conversation history manageable
        if (conversationHistory.size() > 15) {
            conversationHistory.remove(0);
        }
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

        if (!message.contains("Salut jardinier") && !message.contains("Bonjour") && !message.contains("Hey")) {
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
                    // Message saved successfully
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
        // Clear conversation history to free memory
        if (conversationHistory != null) {
            conversationHistory.clear();
        }
    }
}