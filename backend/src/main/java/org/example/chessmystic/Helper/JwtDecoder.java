package org.example.chessmystic.Helper;

import com.google.gson.JsonParser;

import java.util.Base64;

public class JwtDecoder {
    public static String getKeycloakId(String token) {
        String payload = token.split("\\.")[1];
        String json = new String(Base64.getDecoder().decode(payload));
        return JsonParser.parseString(json).getAsJsonObject().get("sub").getAsString();
    }
}