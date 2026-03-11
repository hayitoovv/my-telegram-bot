package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Dastur kirish nuqtasi
 * Ishga tushirish uchun:
 *   java -jar telegram-bot.jar <BOT_TOKEN> <BOT_USERNAME>
 * yoki
 *   environment variable orqali: BOT_TOKEN, BOT_USERNAME
 */
public class Main {

    public static void main(String[] args) {

        // 1. Token va username olish (environment variable yoki argument)
        String botToken    = "8244475879:AAFwsIyRJzYhztoBvIUMGoiEgBjB_JP5Zus";
        String botUsername ="Zarmed_shartnoma_bot";

        if (botToken == null || botToken.isEmpty()) {
            System.err.println("❌ BOT_TOKEN topilmadi!");
            System.err.println("Ishlatish: java -jar telegram-bot.jar <TOKEN> <USERNAME>");
            System.err.println("yoki: export BOT_TOKEN=... && export BOT_USERNAME=...");
            System.exit(1);
        }

        // 2. Botni ro'yxatdan o'tkazish va ishga tushirish
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MyBot(botToken, botUsername));
            System.out.println("✅ Bot muvaffaqiyatli ishga tushdi: @" + botUsername);
        } catch (TelegramApiException e) {
            System.err.println("❌ Botni ishga tushirishda xato: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Avval environment variable-dan, keyin argument-dan o'qiydi
     */
    private static String getEnvOrArg(String[] args, int index, String envKey) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isEmpty()) return envVal;
        if (args.length > index) return args[index];
        return null;
    }
}
