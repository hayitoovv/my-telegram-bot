# 🤖 Telegram Bot — Java

Pasport seriya, JSHSHR va rasm so'rovchi Telegram bot.

---

## 📂 Loyiha tuzilmasi

```
telegram-bot/
├── pom.xml
└── src/main/java/uz/bot/
    ├── Main.java        ← Dastur kirish nuqtasi
    ├── MyBot.java       ← Asosiy bot logikasi
    ├── UserState.java   ← Foydalanuvchi holatlari
    └── UserData.java    ← Foydalanuvchi ma'lumotlari
```

---

## 🚀 Ishga tushirish

### 1. BotFather orqali bot yaratish
1. Telegramda `@BotFather` ni oching
2. `/newbot` yozing
3. Bot nomi va username bering
4. **Token** ni nusxalab oling

### 2. Loyihani build qilish
```bash
mvn clean package -DskipTests
```
Bu `target/telegram-bot-1.0-SNAPSHOT.jar` faylini yaratadi.

### 3. Botni ishga tushirish

**Variant A — argument orqali:**
```bash
java -jar target/telegram-bot-1.0-SNAPSHOT.jar 1234567890:AAHxxx... mybot_uz
```

**Variant B — environment variable orqali:**
```bash
export BOT_TOKEN=1234567890:AAHxxx...
export BOT_USERNAME=mybot_uz
java -jar target/telegram-bot-1.0-SNAPSHOT.jar
```

---

## 💬 Bot qanday ishlaydi?

| Qadam | Bot so'rovi | Tekshiruv |
|-------|-------------|-----------|
| `/start` | Salomlashadi | — |
| 1 | Pasport seriya | 2 harf + 7 raqam (AA1234567) |
| 2 | JSHSHR | 14 ta raqam |
| 3 | Rasm | Fotosuratni yuborish |
| ✅ | Xulosa xabari | Barcha ma'lumot ko'rsatiladi |

---

## 🗄️ Ma'lumotlarni bazaga saqlash

`MyBot.java` dagi `handlePhoto()` metodida shu yerga baza kodi qo'shiladi:

```java
// ✅ Bu yerda ma'lumotlarni bazaga saqlash mumkin
logger.info("Yangi foydalanuvchi: " + userData);

// Misol: PostgreSQL, MySQL, MongoDB yoki fayl
// userRepository.save(userData);
```

---

## 📦 Talablar

- Java 17+
- Maven 3.6+
- Internet ulanish
