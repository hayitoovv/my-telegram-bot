package org.example;

/**
 * Foydalanuvchining joriy holati (qaysi bosqichda ekanligi)
 */
public enum UserState {
    START,
    WAITING_PASSPORT,
    WAITING_JSHSHR,
    WAITING_PHOTO_FRONT,  // ← oldi tomoni
    WAITING_PHOTO_BACK,   // ← orqa tomoni
    COMPLETED
}
