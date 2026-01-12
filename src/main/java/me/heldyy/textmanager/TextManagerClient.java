package me.heldyy.textmanager;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TextManagerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TextManagerConfig.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("autoclosechat")
                    .then(literal("on").executes(ctx -> {
                        TextManager.setCloseChatAfterSend(true);
                        return 1;
                    }))
                    .then(literal("off").executes(ctx -> {
                        TextManager.setCloseChatAfterSend(false);
                        return 1;
                    }))
            );

            dispatcher.register(literal("textsend")
                    .then(argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        TextManager.sendText(name);
                        return 1;
                    }))
            );

            dispatcher.register(literal("hactextsend")
                    .then(argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        TextManager.sendText(name);
                        return 1;
                    }))
            );

            dispatcher.register(literal("textadd")
                    .then(argument("data", StringArgumentType.greedyString()).executes(ctx -> {
                        String data = StringArgumentType.getString(ctx, "data");
                        MinecraftClient mc = MinecraftClient.getInstance();
                        ClientPlayerEntity p = mc != null ? mc.player : null;

                        String name = "";
                        String description = "";
                        int slashIndex = data.indexOf('/');
                        if (slashIndex >= 0) {
                            name = data.substring(0, slashIndex).trim();
                            description = data.substring(slashIndex + 1).trim();
                        } else {
                            name = data.trim();
                            description = "";
                        }

                        if (name.isEmpty()) {
                            if (p != null) p.sendMessage(Text.literal("Неверный формат. Используйте /textadd Название/Описание").formatted(Formatting.RED), false);
                            return 0;
                        }

                        boolean success = TextManager.addText(name, description);
                        return success ? 1 : 0;
                    }))
            );

            dispatcher.register(literal("texts").executes(ctx -> {
                TextManager.showTextsList();
                return 1;
            }));

            dispatcher.register(literal("textlist").executes(ctx -> {
                TextManager.showTextsList();
                return 1;
            }));

            dispatcher.register(literal("textshelp").executes(ctx -> {
                TextManager.showHelp();
                return 1;
            }));

            dispatcher.register(literal("textrename")
                    .then(argument("args", StringArgumentType.greedyString()).executes(ctx -> {
                        String args = StringArgumentType.getString(ctx, "args");
                        String[] parts = args.split("/", 2);
                        if (parts.length < 2) {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            if (mc != null && mc.player != null) {
                                mc.player.sendMessage(Text.literal("Используйте: /textrename <старое имя>/<новое имя>").formatted(Formatting.RED), false);
                            }
                            return 0;
                        }
                        String oldNameOrIndex = parts[0].trim();
                        String newName = parts[1].trim();
                        MinecraftClient mc = MinecraftClient.getInstance();
                        ClientPlayerEntity p = mc != null ? mc.player : null;

                        String actualOldName = oldNameOrIndex;
                        try {
                            int idx = Integer.parseInt(oldNameOrIndex.trim());
                            String k = TextManager.getTextNameByIndex(idx);
                            if (k != null) actualOldName = k;
                        } catch (NumberFormatException ignored) {}

                        if (actualOldName == null || actualOldName.isEmpty()) {
                            if (p != null) p.sendMessage(Text.literal("Исходное имя не найдено").formatted(Formatting.RED), false);
                            return 0;
                        }

                        boolean success = TextManager.renameText(actualOldName, newName);
                        return success ? 1 : 0;
                    }))
            );

            dispatcher.register(literal("textedit")
                    .then(argument("args", StringArgumentType.greedyString()).executes(ctx -> {
                        String args = StringArgumentType.getString(ctx, "args");
                        String[] parts = args.split("/", 2);
                        if (parts.length < 2) {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            if (mc != null && mc.player != null) {
                                mc.player.sendMessage(Text.literal("Используйте: /textedit <имя или №>/<новое описание>").formatted(Formatting.RED), false);
                            }
                            return 0;
                        }
                        String nameOrIndex = parts[0].trim();
                        String description = parts[1].trim();
                        MinecraftClient mc = MinecraftClient.getInstance();
                        ClientPlayerEntity p = mc != null ? mc.player : null;

                        String actualName = nameOrIndex;
                        try {
                            int idx = Integer.parseInt(nameOrIndex.trim());
                            String k = TextManager.getTextNameByIndex(idx);
                            if (k != null) actualName = k;
                        } catch (NumberFormatException ignored) {}

                        if (actualName == null || actualName.isEmpty()) {
                            if (p != null) p.sendMessage(Text.literal("Текст не найден").formatted(Formatting.RED), false);
                            return 0;
                        }

                        boolean success = TextManager.updateTextDescription(actualName, description);
                        return success ? 1 : 0;
                    }))
            );

            dispatcher.register(literal("textremove")
                    .then(argument("nameOrIndex", StringArgumentType.greedyString()).executes(ctx -> {
                        String nameOrIndex = StringArgumentType.getString(ctx, "nameOrIndex");
                        MinecraftClient mc = MinecraftClient.getInstance();
                        ClientPlayerEntity p = mc != null ? mc.player : null;

                        String actual = nameOrIndex;
                        try {
                            int idx = Integer.parseInt(nameOrIndex.trim());
                            String k = TextManager.getTextNameByIndex(idx);
                            if (k != null) actual = k;
                        } catch (NumberFormatException ignored) {}

                        boolean success = TextManager.removeText(actual);
                        return success ? 1 : 0;
                    }))
            );

            dispatcher.register(literal("textdelete")
                    .then(argument("nameOrIndex", StringArgumentType.greedyString()).executes(ctx -> {
                        String nameOrIndex = StringArgumentType.getString(ctx, "nameOrIndex");
                        String actual = nameOrIndex;
                        try {
                            int idx = Integer.parseInt(nameOrIndex.trim());
                            String k = TextManager.getTextNameByIndex(idx);
                            if (k != null) actual = k;
                        } catch (NumberFormatException ignored) {}

                        boolean success = TextManager.removeText(actual);
                        return success ? 1 : 0;
                    }))
            );

            dispatcher.register(literal("textmove")
                    .then(argument("name", StringArgumentType.greedyString())
                            .then(argument("pos", IntegerArgumentType.integer(1)).executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                int pos = IntegerArgumentType.getInteger(ctx, "pos");
                                boolean ok = TextManager.moveText(name, pos);
                                return ok ? 1 : 0;
                            })))
            );

            dispatcher.register(literal("textmoveindex")
                    .then(argument("from", IntegerArgumentType.integer(1))
                            .then(argument("to", IntegerArgumentType.integer(1)).executes(ctx -> {
                                int from = IntegerArgumentType.getInteger(ctx, "from");
                                int to = IntegerArgumentType.getInteger(ctx, "to");
                                boolean ok = TextManager.moveTextByIndex(from, to);
                                return ok ? 1 : 0;
                            })))
            );

            dispatcher.register(literal("textsconfig").executes(ctx -> {
                TextManager.reloadConfig();
                return 1;
            }));
        });
    }
                                                                                          }
