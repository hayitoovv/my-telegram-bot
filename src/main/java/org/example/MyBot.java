package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MyBot extends TelegramLongPollingBot {

    private static final Logger logger = Logger.getLogger(MyBot.class.getName());

    private final String BOT_TOKEN;
    private final String BOT_USERNAME;

    // ← O'z admin chat ID ingizni kiriting
    private static final long ADMIN_CHAT_ID = 8187750931L;

    // Foydalanuvchilar holati
    private final Map<Long, UserData> userDataMap = new HashMap<>();

    // Admin holati
    private final Map<Long, AdminState> adminStateMap = new HashMap<>();
    private final Map<Long, Long> pendingTargetUser = new HashMap<>();

    public MyBot(String botToken, String botUsername) {
        this.BOT_TOKEN = botToken;
        this.BOT_USERNAME = botUsername;
    }

    @Override
    public String getBotToken() { return BOT_TOKEN; }

    @Override
    public String getBotUsername() { return BOT_USERNAME; }

    // =====================================================================
    //  Barcha xabarlarni qabul qilish
    // =====================================================================
    @Override
    public void onUpdateReceived(Update update) {

        // Inline tugma bosilganda
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            String firstName = message.getFrom().getFirstName();

            // Admin tekshiruvi
            if (chatId == ADMIN_CHAT_ID) {
                handleAdmin(chatId, message);
                return;
            }

            // Foydalanuvchi ma'lumotlarini olish
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
    //  /start
    // =====================================================================
    private void handleStart(long chatId, String firstName, UserData userData) {
        userData.setState(UserState.WAITING_PASSPORT);
        sendMarkdownText(chatId,
                "👋 Salom, " + firstName + "!\n\n"
                        + "Ro'yxatdan o'tish uchun bir necha ma'lumot kerak.\n\n"
                        + "📄 *1-qadam:* Pasport seriya va raqamingizni kiriting.\n"
                        + "_(Masalan: AA1234567)_", null);
    }

    // =====================================================================
    //  Pasport seriya
    // =====================================================================
    private void handlePassport(long chatId, Message message, UserData userData) {
        if (!message.hasText()) {
            sendText(chatId, "⚠️ Iltimos, pasport seriya va raqamni *matn* ko'rinishida yuboring.");
            return;
        }

        String passport = message.getText().trim().toUpperCase();

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
                        + "🔢 *2-qadam:* JSHSHR ni kiriting.\n"
                        + "_(14 ta raqam, masalan: 12345678901234)_", null);
    }

    // =====================================================================
    //  JSHSHR
    // =====================================================================
    private void handleJshshr(long chatId, Message message, UserData userData) {
        if (!message.hasText()) {
            sendText(chatId, "⚠️ Iltimos, JSHSHR ni *matn* ko'rinishida yuboring.");
            return;
        }

        String jshshr = message.getText().trim();

        if (!jshshr.matches("\\d{14}")) {
            sendText(chatId, "❌ Noto'g'ri format!\n"
                    + "JSHSHR 14 ta raqamdan iborat bo'lishi kerak.\n"
                    + "_(Masalan: 12345678901234)_\n\nQaytadan kiriting:");
            return;
        }

        userData.setJshshr(jshshr);
        userData.setState(UserState.WAITING_PHOTO_FRONT);
        sendMarkdownText(chatId,
                "✅ JSHSHR qabul qilindi.\n\n"
                        + "📸 *3-qadam:* Pasportning *OLDI* tomonini yuboring.", null);
    }

    // =====================================================================
    //  Pasport oldi tomoni
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
                        + "📸 *4-qadam:* Pasportning *ORQA* tomonini yuboring.", null);
    }

    // =====================================================================
    //  Pasport orqa tomoni + Admin ga yuborish
    // =====================================================================
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

        // Admin ga ma'lumotlar + tugma yuborish
        String adminMsg = "📥 *Yangi foydalanuvchi!*\n\n"
                + "👤 Chat ID: `" + chatId + "`\n"
                + "📄 Pasport: `" + userData.getPassportSeriya() + "`\n"
                + "🔢 JSHSHR: `" + userData.getJshshr() + "`";

        // Inline tugma
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("📄 Shartnoma yuborish");
        button.setCallbackData("send_contract:" + chatId);

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);
        markup.setKeyboard(rows);

        SendMessage adminMessage = new SendMessage();
        adminMessage.setChatId(ADMIN_CHAT_ID);
        adminMessage.setText(adminMsg);
        adminMessage.setParseMode("Markdown");
        adminMessage.setReplyMarkup(markup);

        try {
            execute(adminMessage);

            // Oldi rasmi
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
            logger.severe("Admin ga yuborishda xato: " + e.getMessage());
        }

        logger.info("Yangi foydalanuvchi: chatId=" + chatId + " | " + userData);
    }

    // =====================================================================
    //  Inline tugma callback
    // =====================================================================
    private void handleCallback(CallbackQuery callbackQuery) {
        long adminId = callbackQuery.getFrom().getId();
        String data = callbackQuery.getData();

        if (data.startsWith("send_contract:")) {
            long targetUserId = Long.parseLong(data.split(":")[1]);

            adminStateMap.put(adminId, AdminState.WAITING_CONTRACT_FILE);
            pendingTargetUser.put(adminId, targetUserId);

            // Tugmani olib tashlash
            try {
                EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
                edit.setChatId(adminId);
                edit.setMessageId(callbackQuery.getMessage().getMessageId());
                InlineKeyboardMarkup emptyMarkup = new InlineKeyboardMarkup();
                emptyMarkup.setKeyboard(new ArrayList<>());
                edit.setReplyMarkup(emptyMarkup);
                execute(edit);
            } catch (TelegramApiException e) {
                logger.severe(e.getMessage());
            }

            sendText(adminId, "📤 Endi shartnoma faylini yuboring (PDF yoki Word):");
        }
    }

    // =====================================================================
    //  Admin panel
    // =====================================================================
    private void handleAdmin(long chatId, Message message) {
        AdminState state = adminStateMap.getOrDefault(chatId, AdminState.NONE);

        // Admin fayl yubordi
        if (state == AdminState.WAITING_CONTRACT_FILE && message.hasDocument()) {
            Document doc = message.getDocument();
            String fileName = doc.getFileName() != null ? doc.getFileName().toLowerCase() : "";

            if (fileName.endsWith(".pdf") || fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
                long targetUserId = pendingTargetUser.get(chatId);

                try {
                    SendDocument sendDoc = new SendDocument();
                    sendDoc.setChatId(targetUserId);
                    sendDoc.setDocument(new InputFile(doc.getFileId()));
                    sendDoc.setCaption("📄 Sizga shartnoma yuborildi. Ko'rib chiqing!");
                    execute(sendDoc);

                    sendText(chatId, "✅ Shartnoma muvaffaqiyatli yuborildi!\n"
                            + "👤 User ID: `" + targetUserId + "`");
                } catch (TelegramApiException e) {
                    sendText(chatId, "❌ Yuborishda xato! Foydalanuvchi botni bloklagan bo'lishi mumkin.");
                }

                adminStateMap.put(chatId, AdminState.NONE);
                pendingTargetUser.remove(chatId);
            } else {
                sendText(chatId, "❌ Faqat PDF yoki Word fayl yuboring!");
            }
            return;
        }

        // Admin /start bosdi
        if (message.hasText() && message.getText().equals("/start")) {
            sendText(chatId, "👋 Admin panel!\n\n"
                    + "Foydalanuvchi ma'lumotlari kelganda\n"
                    + "*📄 Shartnoma yuborish* tugmasini bosing.\n\n"
                    + "❌ Bekor qilish: /cancel");
            return;
        }

        // /cancel
        if (message.hasText() && message.getText().equals("/cancel")) {
            adminStateMap.put(chatId, AdminState.NONE);
            pendingTargetUser.remove(chatId);
            sendText(chatId, "❌ Bekor qilindi.");
        }
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

        if (keyboard instanceof ReplyKeyboardRemove) {
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
