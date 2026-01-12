package me.heldyy.textmanager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public class TextManager {

    private static final Map<String, String> TEXTS = new LinkedHashMap<>();
    private static final Map<String, String> ORIGINAL_NAMES = new LinkedHashMap<>();

    public static boolean addText(String name, String description) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        name = sanitizeAmpersandEscapes(name);
        description = sanitizeAmpersandEscapes(description);

        String nameKey = name.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();
        if (nameKey.isEmpty()) {
            return false;
        }

        if (TEXTS.containsKey(nameKey)) {
            sendStatus("Текст с таким названием уже существует: " + stripColorCodes(name), Formatting.RED);
            return false;
        }

        TEXTS.put(nameKey, description != null ? description : "");
        ORIGINAL_NAMES.put(nameKey, name);
        TextManagerConfig.saveTexts();
        sendStatus("Добавлен текст: " + stripColorCodes(name), Formatting.GREEN);
        return true;
    }

    public static boolean removeText(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        String nameKey = name.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();
        if (nameKey.isEmpty()) {
            sendStatus("Неверное имя для удаления", Formatting.RED);
            return false;
        }

        boolean removed = TEXTS.remove(nameKey) != null;
        if (removed) {
            ORIGINAL_NAMES.remove(nameKey);
            TextManagerConfig.saveTexts();
            sendStatus("Удален текст: " + stripColorCodes(name), Formatting.GREEN);
        } else {
            sendStatus("Текст не найден: " + stripColorCodes(name), Formatting.RED);
        }
        return removed;
    }

    public static boolean renameText(String oldName, String newName) {
        if (oldName == null || oldName.isEmpty() || newName == null || newName.isEmpty()) {
            return false;
        }

        String oldKey = oldName.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();
        String newKey = newName.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();

        if (oldKey.isEmpty() || newKey.isEmpty()) {
            return false;
        }

        if (!TEXTS.containsKey(oldKey)) {
            sendStatus("Старое название не найдено: " + stripColorCodes(oldName), Formatting.RED);
            return false;
        }

        if (!oldKey.equals(newKey) && TEXTS.containsKey(newKey)) {
            sendStatus("Новое название уже используется: " + stripColorCodes(newName), Formatting.RED);
            return false;
        }

        if (oldKey.equals(newKey)) {
            ORIGINAL_NAMES.put(oldKey, newName);
            TextManagerConfig.saveTexts();
            sendStatus("Переименован: " + stripColorCodes(oldName) + " -> " + stripColorCodes(newName), Formatting.GREEN);
            return true;
        }

        Map<String, String> newTexts = new LinkedHashMap<>();
        Map<String, String> newOriginals = new LinkedHashMap<>();

        for (Map.Entry<String, String> e : TEXTS.entrySet()) {
            String k = e.getKey();
            if (k.equals(oldKey)) {
                newTexts.put(newKey, e.getValue());
                newOriginals.put(newKey, newName);
            } else {
                newTexts.put(k, e.getValue());
                newOriginals.put(k, ORIGINAL_NAMES.getOrDefault(k, k));
            }
        }

        TEXTS.clear();
        TEXTS.putAll(newTexts);
        ORIGINAL_NAMES.clear();
        ORIGINAL_NAMES.putAll(newOriginals);

        TextManagerConfig.saveTexts();
        sendStatus("Переименован: " + stripColorCodes(oldName) + " -> " + stripColorCodes(newName), Formatting.GREEN);
        return true;
    }

    public static boolean updateTextDescription(String name, String newDescription) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        String nameKey = name.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();
        if (nameKey.isEmpty() || !TEXTS.containsKey(nameKey)) {
            return false;
        }

        TEXTS.put(nameKey, newDescription != null ? newDescription : "");
        TextManagerConfig.saveTexts();
        sendStatus("Обновлено описание для: " + stripColorCodes(name), Formatting.GREEN);
        return true;
    }

    public static String getTextDescription(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        String nameKey = name.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();
        return TEXTS.get(nameKey);
    }

    public static String getOriginalName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        String nameKey = name.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();
        return ORIGINAL_NAMES.get(nameKey);
    }

    public static Map<String, String> getAllTexts() {
        return new LinkedHashMap<>(TEXTS);
    }

    public static Map<String, String> getAllOriginalNames() {
        return new LinkedHashMap<>(ORIGINAL_NAMES);
    }

    public static String getTextNameByIndex(int index) {
        if (index < 1 || index > TEXTS.size()) {
            return null;
        }

        int currentIndex = 1;
        for (String nameKey : TEXTS.keySet()) {
            if (currentIndex == index) {
                return nameKey;
            }
            currentIndex++;
        }
        return null;
    }

    public static String getOriginalTextNameByIndex(int index) {
        String nameKey = getTextNameByIndex(index);
        if (nameKey == null) {
            return null;
        }
        return ORIGINAL_NAMES.getOrDefault(nameKey, nameKey);
    }

    public static void loadTexts(Map<String, String> texts, Map<String, String> originalNames) {
        TEXTS.clear();
        ORIGINAL_NAMES.clear();

        Map<String, String> loadedTexts = new LinkedHashMap<>();
        Map<String, String> loadedOriginals = new LinkedHashMap<>();

        if (texts != null) {
            for (Map.Entry<String, String> e : texts.entrySet()) {
                String k = normalizeKey(e.getKey());
                if (k.isEmpty()) continue;
                String v = sanitizeAmpersandEscapes(e.getValue());
                loadedTexts.put(k, v != null ? v : "");
            }
        }

        if (originalNames != null) {
            for (Map.Entry<String, String> e : originalNames.entrySet()) {
                String k = normalizeKey(e.getKey());
                if (k.isEmpty()) continue;
                String v = sanitizeAmpersandEscapes(e.getValue());
                loadedOriginals.put(k, v != null ? v : k);
            }
        }

        for (Map.Entry<String, String> e : loadedTexts.entrySet()) {
            String k = e.getKey();
            TEXTS.put(k, e.getValue());
            ORIGINAL_NAMES.put(k, loadedOriginals.getOrDefault(k, k));
        }
    }

    public static boolean moveText(String name, int position) {
        if (name == null || name.isEmpty()) return false;
        String key = normalizeKey(name);
        if (!TEXTS.containsKey(key)) return false;

        if (position < 1) position = 1;
        int size = TEXTS.size();
        if (position > size) position = size;

        Map<String, String> newTexts = new LinkedHashMap<>();
        Map<String, String> newOriginals = new LinkedHashMap<>();

        String desc = TEXTS.get(key);
        String original = ORIGINAL_NAMES.getOrDefault(key, key);

        int idx = 1;
        boolean inserted = false;
        for (Map.Entry<String, String> e : TEXTS.entrySet()) {
            String k = e.getKey();
            if (k.equals(key)) continue;

            if (idx == position) {
                newTexts.put(key, desc);
                newOriginals.put(key, original);
                inserted = true;
            }

            newTexts.put(k, e.getValue());
            newOriginals.put(k, ORIGINAL_NAMES.getOrDefault(k, k));
            idx++;
        }

        if (!inserted) {
            newTexts.put(key, desc);
            newOriginals.put(key, original);
        }

        TEXTS.clear();
        TEXTS.putAll(newTexts);
        ORIGINAL_NAMES.clear();
        ORIGINAL_NAMES.putAll(newOriginals);
        TextManagerConfig.saveTexts();
        sendStatus("Перемещен текст: " + stripColorCodes(name) + " -> позиция " + position, Formatting.GREEN);
        return true;
    }

    public static boolean moveTextByIndex(int fromIndex, int toIndex) {
        if (fromIndex < 1 || toIndex < 1) return false;
        int size = TEXTS.size();
        if (fromIndex > size) return false;
        if (toIndex > size) toIndex = size;

        String key = null;
        int idx = 1;
        for (String k : TEXTS.keySet()) {
            if (idx == fromIndex) {
                key = k;
                break;
            }
            idx++;
        }
        if (key == null) {
            sendStatus("Неверный индекс: " + fromIndex, Formatting.RED);
            return false;
        }
        boolean res = moveText(key, toIndex);
        if (!res) sendStatus("Не удалось переместить текст", Formatting.RED);
        return res;
    }

    private static String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase().replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();
    }

    private static String sanitizeAmpersandEscapes(String s) {
        if (s == null) return null;
        s = s.replace("\\u0026", "&");
        s = s.replace("\\u0026c", "&c");
        s = s.replace("\u0026", "&");
        return s;
    }

    private static void sendStatus(String message, Formatting color) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                mc.player.sendMessage(Text.literal("[TextManager] ").formatted(Formatting.GOLD)
                        .append(Text.literal(message).formatted(color)), false);
            }
        } catch (Exception ignored) {}
    }

    private static String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "").trim();
    }

    public static void sendText(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        String description = getTextDescription(name);
        if (description == null) {
            sendStatus("Текст не найден: " + name, Formatting.RED);
            return;
        }

        String trimmed = description.trim();
        boolean isCommand = trimmed.startsWith("/");

        if (isCommand) {
            if (mc.keyboard == null) return;

            mc.setScreen(new ChatScreen(trimmed));
            scheduleKeyPress(mc);

            if (TextManagerConfig.isCloseChatAfterSend()) {
                scheduleEscPress(mc, 90);
            }
            return;
        }

        if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendChatMessage(description);
        }

        if (TextManagerConfig.isCloseChatAfterSend()) {
            scheduleEscPress(mc, 20);
        }
    }

    private static void closeChatIfOpen(MinecraftClient mc) {
        try {
            Runnable close = () -> {
                if (mc.currentScreen == null) return;

                boolean isVanillaChat = mc.currentScreen instanceof ChatScreen;
                String cls = mc.currentScreen.getClass().getName().toLowerCase();
                boolean looksLikeChat = cls.contains("chat");

                if (isVanillaChat || looksLikeChat) {
                    mc.setScreen(null);
                }
            };

            try {
                mc.execute(close);
            } catch (Throwable ignored) {
                close.run();
            }
        } catch (Exception ignored) {}
    }

    private static void scheduleKeyPress(MinecraftClient mc) {
        new Thread(() -> {
            try {
                Thread.sleep(50);
                if (mc.keyboard != null) {
                    long window = mc.getWindow().getHandle();
                    mc.keyboard.onKey(window, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_PRESS, 0);
                    mc.keyboard.onKey(window, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_RELEASE, 0);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private static void scheduleEscPress(MinecraftClient mc, int delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(Math.max(0, delayMs));
                if (mc == null || mc.keyboard == null) return;

                Runnable press = () -> {
                    if (mc.currentScreen == null) return;

                    boolean isVanillaChat = mc.currentScreen instanceof ChatScreen;
                    String cls = mc.currentScreen.getClass().getName().toLowerCase();
                    boolean looksLikeChat = cls.contains("chat");

                    if (!isVanillaChat && !looksLikeChat) return;

                    long window = mc.getWindow().getHandle();
                    mc.keyboard.onKey(window, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_PRESS, 0);
                    mc.keyboard.onKey(window, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_RELEASE, 0);
                };

                try {
                    mc.execute(press);
                } catch (Throwable ignored) {
                    press.run();
                }
            } catch (Exception ignored) {}
        }).start();
    }

    public static void showTextsList() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (TEXTS.isEmpty()) {
            mc.player.sendMessage(Text.literal("У вас нет сохраненных текстов").formatted(Formatting.GRAY), false);
            return;
        }

        MutableText fullMessage = Text.literal("[TextManager] ").formatted(Formatting.GOLD)
                .append(Text.literal("Вот все ваши текста:").formatted(Formatting.YELLOW));

        int index = 1;
        for (Map.Entry<String, String> entry : TEXTS.entrySet()) {
            String nameKey = entry.getKey();
            String originalName = ORIGINAL_NAMES.getOrDefault(nameKey, nameKey);
            String displayName = originalName.replace("&", "§");

            fullMessage.append(Text.literal("\n"));

            MutableText textLine = Text.literal(index + ". " + displayName)
                    .styled(style -> style.withClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/textsend " + nameKey)
                    ));

            fullMessage.append(textLine);
            index++;
        }

        mc.player.sendMessage(fullMessage, false);
    }

    public static void showHelp() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        MutableText help = Text.literal("");
        help.append(Text.literal("\n═══════════════════════════════").formatted(Formatting.GOLD));
        help.append(Text.literal("\nTextManager - Справка").formatted(Formatting.YELLOW));
        help.append(Text.literal("\n═══════════════════════════════").formatted(Formatting.GOLD));

        help.append(Text.literal("\n\n[ПРОСМОТР И ОТПРАВКА]").formatted(Formatting.LIGHT_PURPLE));
        help.append(Text.literal("\n/textlist").formatted(Formatting.AQUA));
        help.append(Text.literal(" или ").formatted(Formatting.GRAY));
        help.append(Text.literal("/texts").formatted(Formatting.AQUA));
        help.append(Text.literal(" - показать все шаблоны").formatted(Formatting.GRAY));
        help.append(Text.literal("\n  Клик на название = отправить текст").formatted(Formatting.DARK_GRAY));

        help.append(Text.literal("\n\n[ДОБАВЛЕНИЕ ШАБЛОНА]").formatted(Formatting.LIGHT_PURPLE));
        help.append(Text.literal("\n/textadd <название>/<описание>").formatted(Formatting.AQUA));
        help.append(Text.literal("\n  /textadd Проверка/&c&lЭто проверка на читы!").formatted(Formatting.DARK_GRAY));
        help.append(Text.literal("\n  /textadd &d&lНеадекват / /hm sban 30d Неадекват").formatted(Formatting.DARK_GRAY));

        help.append(Text.literal("\n\n[РЕДАКТИРОВАНИЕ]").formatted(Formatting.LIGHT_PURPLE));
        help.append(Text.literal("\n/textedit <название|№>/<описание>").formatted(Formatting.AQUA));
        help.append(Text.literal("\n/textrename <название|№>/<новое имя>").formatted(Formatting.AQUA));

        help.append(Text.literal("\n\n[УДАЛЕНИЕ]").formatted(Formatting.LIGHT_PURPLE));
        help.append(Text.literal("\n/textremove").formatted(Formatting.AQUA));
        help.append(Text.literal(" или ").formatted(Formatting.GRAY));
        help.append(Text.literal("/textdelete <название|№>").formatted(Formatting.AQUA));

        help.append(Text.literal("\n\n[ПЕРЕМЕЩЕНИЕ]").formatted(Formatting.LIGHT_PURPLE));
        help.append(Text.literal("\n/textmove <название> <позиция>").formatted(Formatting.AQUA));
        help.append(Text.literal("\n/textmoveindex <из> <в>").formatted(Formatting.AQUA));

        help.append(Text.literal("\n\n[ОПЦИИ]").formatted(Formatting.LIGHT_PURPLE));
        help.append(Text.literal("\n/autoclosechat on|off").formatted(Formatting.AQUA));
        help.append(Text.literal(" - закрывать чат после отправки текста (команды закрываются всегда)").formatted(Formatting.GRAY));
        help.append(Text.literal("\n/textsconfig").formatted(Formatting.AQUA));
        help.append(Text.literal(" - перезагрузить конфиг из файла").formatted(Formatting.GRAY));

        help.append(Text.literal("\n\n[ЦВЕТОВЫЕ КОДЫ]").formatted(Formatting.LIGHT_PURPLE));
        help.append(Text.literal("\n&c - красный, &a - зелёный, &e - жёлтый, &6 - золото").formatted(Formatting.DARK_GRAY));
        help.append(Text.literal("\n&d - фиолетовый, &f - белый, &0 - чёрный, &7 - серый").formatted(Formatting.DARK_GRAY));
        help.append(Text.literal("\n&b - голубой, &3 - тёмный голубой, &9 - синий, &1 - тёмно-синий").formatted(Formatting.DARK_GRAY));
        help.append(Text.literal("\n&2 - тёмно-зелёный, &4 - тёмно-красный, &5 - тёмно-фиолетовый, &8 - тёмно-серый").formatted(Formatting.DARK_GRAY));
        help.append(Text.literal("\n&l - жирный, &o - курсив, &n - подчёркивание, &m - зачёркивание, &k - магия").formatted(Formatting.DARK_GRAY));

        help.append(Text.literal("\n\n═══════════════════════════════").formatted(Formatting.GOLD));

        mc.player.sendMessage(help, false);
    }

    public static void toggleCloseChatAfterSend() {
        boolean newValue = !TextManagerConfig.isCloseChatAfterSend();
        TextManagerConfig.setCloseChatAfterSend(newValue);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.sendMessage(Text.literal("[TextManager] ").formatted(Formatting.GOLD)
                    .append(Text.literal(newValue ? "Чат будет закрываться после отправки" : "Чат останется открытым после отправки")
                            .formatted(newValue ? Formatting.RED : Formatting.GREEN)), false);
        }
    }

    public static void setCloseChatAfterSend(boolean value) {
        TextManagerConfig.setCloseChatAfterSend(value);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            boolean now = TextManagerConfig.isCloseChatAfterSend();
            mc.player.sendMessage(Text.literal("[TextManager] ").formatted(Formatting.GOLD)
                    .append(Text.literal(now ? "Чат будет закрываться после отправки" : "Чат останется открытым после отправки")
                            .formatted(now ? Formatting.RED : Formatting.GREEN)), false);
        }
    }

    public static void reloadConfig() {
        TextManagerConfig.reloadConfig();
        sendStatus("Конфиг обновлен!", Formatting.GREEN);
    }
			}
