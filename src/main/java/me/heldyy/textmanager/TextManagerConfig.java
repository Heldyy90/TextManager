package me.heldyy.textmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Конфиг TextManager:
 * - хранит тексты и оригинальные названия
 * - хранит порядок (чтобы перемещения сохранялись между перезапусками)
 */
public class TextManagerConfig {

    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("textmanager_config.json");

    private static final Map<String, String> TEXTS = new LinkedHashMap<>();
    private static final Map<String, String> ORIGINAL_NAMES = new LinkedHashMap<>();
    private static boolean CLOSE_CHAT_AFTER_SEND = true;

    static {
        load();
    }

    public static Map<String, String> getTexts() {
        return new LinkedHashMap<>(TEXTS);
    }

    public static Map<String, String> getOriginalNames() {
        return new LinkedHashMap<>(ORIGINAL_NAMES);
    }

    public static boolean isCloseChatAfterSend() {
        return CLOSE_CHAT_AFTER_SEND;
    }

    public static void setCloseChatAfterSend(boolean value) {
        CLOSE_CHAT_AFTER_SEND = value;
        saveTexts();
    }

    /**
     * Сохранить конфиг (включая порядок ключей).
     */
    public static void saveTexts() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                Map<String, Object> root = new LinkedHashMap<>();

                // Порядок (важно для перемещений)
                List<String> order = new ArrayList<>(TextManager.getAllTexts().keySet());
                root.put("order", order);

                // Данные
                root.put("textOriginalNames", TextManager.getAllOriginalNames());
                root.put("texts", TextManager.getAllTexts());

                // Опции
                root.put("closeChatAfterSend", CLOSE_CHAT_AFTER_SEND);

                G.toJson(root, w);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Загрузить конфиг (с сохранением порядка).
     * Поддерживает старый формат без поля "order".
     */
    public static void load() {
        try {
            if (!Files.exists(FILE)) return;

            try (Reader r = Files.newBufferedReader(FILE)) {
                JsonElement parsed = JsonParser.parseReader(r);
                if (parsed == null || !parsed.isJsonObject()) return;

                JsonObject root = parsed.getAsJsonObject();

                JsonObject textsObj = root.has("texts") && root.get("texts").isJsonObject()
                        ? root.getAsJsonObject("texts")
                        : new JsonObject();

                JsonObject originalsObj = root.has("textOriginalNames") && root.get("textOriginalNames").isJsonObject()
                        ? root.getAsJsonObject("textOriginalNames")
                        : new JsonObject();

                if (root.has("closeChatAfterSend") && root.get("closeChatAfterSend").isJsonPrimitive()) {
                    try {
                        CLOSE_CHAT_AFTER_SEND = root.get("closeChatAfterSend").getAsBoolean();
                    } catch (Exception ignored) {}
                }

                // Собираем карты в корректном порядке
                LinkedHashMap<String, String> loadedTexts = new LinkedHashMap<>();
                LinkedHashMap<String, String> loadedOriginals = new LinkedHashMap<>();

                // 1) Если есть order — идем по нему
                if (root.has("order") && root.get("order").isJsonArray()) {
                    JsonArray orderArr = root.getAsJsonArray("order");
                    for (JsonElement e : orderArr) {
                        if (e == null || !e.isJsonPrimitive()) continue;
                        String key = e.getAsString();
                        if (key == null) continue;

                        if (textsObj.has(key)) {
                            loadedTexts.put(key, textsObj.get(key).isJsonNull() ? "" : textsObj.get(key).getAsString());
                            if (originalsObj.has(key)) {
                                loadedOriginals.put(key, originalsObj.get(key).isJsonNull() ? key : originalsObj.get(key).getAsString());
                            }
                        }
                    }
                }

                // 2) Добавляем все, что есть в texts, но еще не было добавлено (на случай если order отсутствует или устарел)
                for (Map.Entry<String, JsonElement> entry : textsObj.entrySet()) {
                    String key = entry.getKey();
                    if (loadedTexts.containsKey(key)) continue;

                    JsonElement v = entry.getValue();
                    loadedTexts.put(key, (v == null || v.isJsonNull()) ? "" : v.getAsString());

                    if (originalsObj.has(key)) {
                        JsonElement ov = originalsObj.get(key);
                        loadedOriginals.put(key, (ov == null || ov.isJsonNull()) ? key : ov.getAsString());
                    }
                }

                TEXTS.clear();
                TEXTS.putAll(loadedTexts);
                ORIGINAL_NAMES.clear();
                ORIGINAL_NAMES.putAll(loadedOriginals);

                // Передать TextManager загруженные данные
                try {
                    TextManager.loadTexts(TEXTS, ORIGINAL_NAMES);
                } catch (Throwable ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /**
     * Перезагрузить конфиг из файла (для синхронизации при изменении файла внешними инструментами)
     */
    public static void reloadConfig() {
        load();
    }
}
