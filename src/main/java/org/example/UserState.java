package org.example;

public enum UserState {
    START,
    WAITING_PHONE,        // 1-qadam: Telefon raqam
    WAITING_PASSPORT,     // 2-qadam: Pasport seriya
    WAITING_JSHSHR,       // 3-qadam: JSHSHR
    WAITING_PHOTO_FRONT,  // 4-qadam: Oldi rasmi
    WAITING_PHOTO_BACK,   // 5-qadam: Orqa rasmi
    COMPLETED
}
