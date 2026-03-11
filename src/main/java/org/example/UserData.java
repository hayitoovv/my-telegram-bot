package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Har bir foydalanuvchi uchun to'plangan ma'lumotlar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserData {

    private String passportSeriya;
    private String jshshr;
    private String photoFrontFileId;  // oldi
    private String photoBackFileId;   // orqa
    private UserState state;

}
