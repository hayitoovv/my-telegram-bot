package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Objects;

public class Main {

    public static void main(String[] args) {
        String botToken    = getEnvOrArg(args, 0, "BOT_TOKEN");
        String botUsername = getEnvOrArg(args, 1, "BOT_USERNAME");
        Long admin= Long.valueOf(Objects.requireNonNull(getEnvOrArg(args, 2, "ADMIN_CHAT_ID")));


        if (botToken == null || botToken.isEmpty()) {
            System.err.println("❌ BOT_TOKEN topilmadi!");
            System.exit(1);
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MyBot(botToken, botUsername,admin));
            System.out.println("✅ Bot muvaffaqiyatli ishga tushdi: @" + botUsername);
        } catch (TelegramApiException e) {
            System.err.println("❌ Botni ishga tushirishda xato: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getEnvOrArg(String[] args, int index, String envKey) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isEmpty()) return envVal;
        if (args.length > index) return args[index];
        return null;
    }
}
