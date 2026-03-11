package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Asosiy Telegram Bot klassi
 * /start bosilganda ketma-ket: pasport seriya → JSHSHR → rasm so'raydi
 */
public class MyBot extends TelegramLongPollingBot {
    private static final long ADMIN_CHAT_ID = 333682433;
    private static final Logger logger = Logger.getLogger(MyBot.class.getName());

    // Bot sozlamalari — .env yoki config fayldan ham o'qisa bo'ladi
    private final String BOT_TOKEN;
    private final String BOT_USERNAME;

    // Har bir foydalanuvchi uchun holat xotirasi (chatId → UserData)
    private final Map<Long, UserData> userDataMap = new HashMap<>();

    public MyBot(String botToken, String botUsername) {
        this.BOT_TOKEN = botToken;
        this.BOT_USERNAME = botUsername;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    // =====================================================================
    //  Barcha xabarlarni qabul qilish
    // =====================================================================
    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            String firstName = message.getFrom().getFirstName();

            // Foydalanuvchi ma'lumotlarini olish yoki yangi yaratish
            UserData userData = userDataMap.getOrDefault(chatId, new UserData());

            // /start buyrug'i
            if (message.hasText() && message.getText().equals("/start")) {
                handleStart(chatId, firstName, userData);
                userDataMap.put(chatId, userData);
                return;
            }

            // Holat bo'yicha qayta ishlash
            switch (userData.getState()) {

                case WAITING_PASSPORT:
                    handlePassport(chatId, message, userData);
                    break;

                case WAITING_JSHSHR:
                    handleJshshr(chatId, message, userData);
                    break;

                case WAITING_PHOTO_FRONT:
                    handlePhotoFront(chatId, message, userData);
                    break;

                case WAITING_PHOTO_BACK:
                    handlePhotoBack(chatId, message, userData);
                    break;

                case COMPLETED:
                    sendText(chatId, "✅ Ma'lumotlaringiz allaqachon qabul qilingan.\n"
                            + "Qaytadan boshlash uchun /start bosing.");
                    break;

                default:
                    sendText(chatId, "Boshlash uchun /start bosing.");
                    break;
            }

            userDataMap.put(chatId, userData);
        }
    }

    // =====================================================================
    //  /start — salomlashish va pasport seriyani so'rash
    // =====================================================================
    private void handleStart(long chatId, String firstName, UserData userData) {
        userData.setState(UserState.WAITING_PASSPORT);

        String text = "👋 Salom, " + firstName + "!\n\n"
                + "Ro'yxatdan o'tish uchun bir necha ma'lumot kerak bo'ladi.\n\n"
                + "📄 *1-qadam:* Pasport seriya va raqamingizni kiriting.\n"
                + "_(Masalan: AA1234567)_";

        sendMarkdownText(chatId, text, null);
    }

    // =====================================================================
    //  Pasport seriyani qabul qilish
    // =====================================================================
    private void handlePassport(long chatId, Message message, UserData userData) {

        if (!message.hasText()) {
            sendText(chatId, "⚠️ Iltimos, pasport seriya va raqamni *matn* ko'rinishida yuboring.");
            return;
        }

        String passport = message.getText().trim().toUpperCase();

        // Oddiy tekshiruv: 2 harf + 7 raqam (masalan AA1234567)
        if (!passport.matches("[A-Z]{2}\\d{7}")) {
            sendText(chatId, "❌ Noto'g'ri format!\n"
                    + "Pasport seriya 2 lotin harf + 7 raqamdan iborat bo'lishi kerak.\n"
                    + "_(Masalan: AA1234567)_\n\nQaytadan kiriting:");
            return;
        }

        userData.setPassportSeriya(passport);
        userData.setState(UserState.WAITING_JSHSHR);

        sendMarkdownText(chatId,
                "✅ Pasport seriya qabul qilindi: *" + passport + "*\n\n"
                + "🔢 *2-qadam:* JSHSHR (shaxsiy identifikatsiya raqami) ni kiriting.\n"
                + "_(14 ta raqam, masalan: 12345678901234)_",
                null);
    }

    // =====================================================================
    //  JSHSHR qabul qilish
    // =====================================================================
    private void handleJshshr(long chatId, Message message, UserData userData) {

        if (!message.hasText()) {
            sendText(chatId, "⚠️ Iltimos, JSHSHR ni *matn* ko'rinishida yuboring.");
            return;
        }

        String jshshr = message.getText().trim();

        // JSHSHR tekshiruvi: 14 ta raqam
        if (!jshshr.matches("\\d{14}")) {
            sendText(chatId, "❌ Noto'g'ri format!\n"
                    + "JSHSHR 14 ta raqamdan iborat bo'lishi kerak.\n"
                    + "_(Masalan: 12345678901234)_\n\nQaytadan kiriting:");
            return;
        }

        userData.setJshshr(jshshr);
        userData.setState(UserState.WAITING_PHOTO_FRONT); // WAITING_PHOTO emas!

        sendMarkdownText(chatId,
                "✅ JSHSHR qabul qilindi.\n\n"
                        + "📸 *3-qadam:* Pasportning *OLDI* tomonini yuboring.",
                null);
    }

    // =====================================================================
    //  Rasmni qabul qilish
    // =====================================================================
    private void handlePhotoFront(long chatId, Message message, UserData userData) {
        if (!message.hasPhoto()) {
            sendText(chatId, "⚠️ Iltimos, *rasm* yuboring.");
            return;
        }
        List<PhotoSize> photos = message.getPhoto();
        String fileId = photos.get(photos.size() - 1).getFileId();
        userData.setPhotoFrontFileId(fileId);
        userData.setState(UserState.WAITING_PHOTO_BACK);

        sendMarkdownText(chatId,
                "✅ Oldi tomoni qabul qilindi!\n\n"
                        + "📸 *4-qadam:* Pasportning *ORQA* tomonini yuboring.",
                null);
    }

    private void handlePhotoBack(long chatId, Message message, UserData userData) {
        if (!message.hasPhoto()) {
            sendText(chatId, "⚠️ Iltimos, *rasm* yuboring.");
            return;
        }
        List<PhotoSize> photos = message.getPhoto();
        String fileId = photos.get(photos.size() - 1).getFileId();
        userData.setPhotoBackFileId(fileId);
        userData.setState(UserState.COMPLETED);

        // Foydalanuvchiga xulosa
        sendMarkdownText(chatId,
                "🎉 *Barcha ma'lumotlar qabul qilindi!*\n\n"
                        + "📄 Pasport: `" + userData.getPassportSeriya() + "`\n"
                        + "🔢 JSHSHR: `" + userData.getJshshr() + "`\n"
                        + "📸 Oldi tomoni: ✅\n"
                        + "📸 Orqa tomoni: ✅\n\n"
                        + "Tez orada siz bilan bog'lanamiz. Rahmat! 🙏",
                removeKeyboard());

        // Admin ga yuborish
        String adminMsg = "📥 *Yangi foydalanuvchi!*\n\n"
                + "📄 Pasport: `" + userData.getPassportSeriya() + "`\n"
                + "🔢 JSHSHR: `" + userData.getJshshr() + "`";
        sendMarkdownText(ADMIN_CHAT_ID, adminMsg, null);

        // Oldi rasmi
        try {
            SendPhoto front = new SendPhoto();
            front.setChatId(ADMIN_CHAT_ID);
            front.setPhoto(new InputFile(userData.getPhotoFrontFileId()));
            front.setCaption("📸 Oldi tomoni — " + userData.getPassportSeriya());
            execute(front);

            // Orqa rasmi
            SendPhoto back = new SendPhoto();
            back.setChatId(ADMIN_CHAT_ID);
            back.setPhoto(new InputFile(userData.getPhotoBackFileId()));
            back.setCaption("📸 Orqa tomoni — " + userData.getPassportSeriya());
            execute(back);
        } catch (TelegramApiException e) {
            logger.severe("Rasm yuborishda xato: " + e.getMessage());
        }

        logger.info("Yangi foydalanuvchi: " + userData);
    }





    // =====================================================================
    //  Yordamchi metodlar
    // =====================================================================

    private void sendText(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.setParseMode("Markdown");
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.severe("Xabar yuborishda xato: " + e.getMessage());
        }
    }

    private void sendMarkdownText(long chatId, String text, Object keyboard) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.setParseMode("Markdown");

        if (keyboard instanceof ReplyKeyboardMarkup) {
            msg.setReplyMarkup((ReplyKeyboardMarkup) keyboard);
        } else if (keyboard instanceof ReplyKeyboardRemove) {
            msg.setReplyMarkup((ReplyKeyboardRemove) keyboard);
        }

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.severe("Xabar yuborishda xato: " + e.getMessage());
        }
    }

    private ReplyKeyboardRemove removeKeyboard() {
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        return remove;
    }
}
