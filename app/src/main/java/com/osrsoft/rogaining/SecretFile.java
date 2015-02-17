package com.osrsoft.rogaining;

import android.util.Log;
import java.util.Random;

public class SecretFile {

    public final static int LENGTH_KEY = 16;

    public String init() { // Инициализация. Возвращает ключ
        StringBuilder key = new StringBuilder(LENGTH_KEY);
        Random rnd = new Random();
        for (int i = 0; i < LENGTH_KEY; i++) {
            int ch = 0x41 + rnd.nextInt(26);
            key.append((char)ch);
        }
        return key.toString();
    }

    public String encode(String text, String secret) {  // Шифрует строку XOR-кодированием
        StringBuilder sb = new StringBuilder();
        while (secret.length() < text.length()) { // Сделали ключ >= длины строки
            secret+= secret;
        }
        for (int i = 0; i < text.length(); i++) { // XOR кодирование
            int n = text.charAt(i) ^ secret.charAt(i);
            sb.append((char) n);
        }
        return sb.toString();
    }


}
