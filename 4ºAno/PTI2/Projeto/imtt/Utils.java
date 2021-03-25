package imtt;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    public static final double R_EARTH = 6371e3;
    public static final String NF = "\u001b[0m", BOLD = "\u001b[1m", ITALIC = "\u001b[3m", UNDRLN = "\u001b[4m",
            RVRSD = "\u001b[7m", STRIKE = "\u001b[9m", BLACK = "\u001b[30m", RED = "\u001b[31;1m",
            GREEN = "\u001b[32;1m", YELLOW = "\u001b[33;1m", BLUE = "\u001b[34;1m", PURPLE = "\u001b[35;1m",
            CYAN = "\u001b[36;1m", WHITE = "\u001b[37m", B_BLACK = "\u001b[40m", B_RED = "\u001b[41m",
            B_GREEN = "\u001b[42m", B_YELLOW = "\u001b[43m", B_BLUE = "\u001b[44m", B_PURPLE = "\u001b[45m",
            B_CYAN = "\u001b[46m", B_WHITE = "\u001b[47m", SAVE = "\u001b[s", LOAD = "\u001b[u", CLRLN = "\u001b[0K",
            UP = "\u001b[1A";

    public static String format(int number) {
        return new DecimalFormat("00").format(number);
    }

    public static String stylePlate(String plate) {
        return Utils.B_BLUE + " " + Utils.B_WHITE + Utils.BLACK + " " + plate + " " + Utils.B_YELLOW + " " + Utils.NF;
    }

    public static String date(long time) {
        return DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                .format(Instant.ofEpochMilli(time * 1000).atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    public static long unix(String date) {
        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            return sdFormat.parse(date).getTime() / 1000;
        }

        catch (ParseException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static long now() {
        return Instant.now().getEpochSecond();
    }

    public static int rand(int bits) {
        return new Random().nextInt((int) Math.pow(2, bits));
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static final float avg(byte[] values, int offset, int length) {
        int total = 0;
        int offLen = offset + length;

        for (int i = offset; i < offLen; i++) {
            total += values[i];
        }

        return total / (float) length;
    }

    public static int ordinalIndexOf(String str, String substr, int n) {
        int pos = str.indexOf(substr);

        while (--n > 0 && pos != -1) {
            pos = str.indexOf(substr, pos + 1);
        }

        return pos;
    }

    public static void printon(int lines, String s) {
        System.out.print(SAVE);

        for (int i = 0; i < lines; i++) {
            System.out.println();
        }

        System.out.print(s + LOAD + UP);
    }

    public static String execReadToString(String execCommand) throws IOException {
        try (Scanner s = new Scanner(Runtime.getRuntime().exec(execCommand).getInputStream()).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    public static int getDistance(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        double latA = latitudeA * Math.PI / 180;
        double lngA = longitudeA * Math.PI / 180;
        double latB = latitudeB * Math.PI / 180;
        double lngB = longitudeB * Math.PI / 180;

        return (int) (Math.acos(
                Math.sin(latA) * Math.sin(latB) + Math.cos(latA) * Math.cos(latB) * Math.cos(lngB - lngA)) * R_EARTH);
    }

    public static int getBearing(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        double latA = latitudeA * Math.PI / 180;
        double lngA = longitudeA * Math.PI / 180;
        double latB = latitudeB * Math.PI / 180;
        double lngB = longitudeB * Math.PI / 180;

        return (int) (Math.atan2(Math.sin(lngB - lngA) * Math.cos(latB),
                Math.cos(latA) * Math.sin(latB) - Math.sin(latA) * Math.cos(latB) * Math.cos(lngB - lngA))
                * (180 / Math.PI));

    }

    public static byte[] encrypt(String data, PublicKey key) throws InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data.getBytes());
    }

    public static byte[] encrypt(byte[] data, PublicKey key) throws InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static String decrypt(byte[] encrypted, PrivateKey key) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(encrypted));
    }

    public static byte[] decryptBytes(byte[] encrypted, PrivateKey key) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encrypted);
    }

    public static byte[] generateSymKey() throws NoSuchAlgorithmException {
        KeyGenerator kGenerator = KeyGenerator.getInstance("AES");
        kGenerator.init(128);
        return kGenerator.generateKey().getEncoded();
    }

    public static byte[] symEncrypt(byte[] data, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        // Generating IV.
        //int ivSize = 16;
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Hashing key.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(key);
        byte[] digestedKey = new byte[16];
        System.arraycopy(digest.digest(), 0, digestedKey, 0, digestedKey.length);
        SecretKeySpec secretKeySpec = new SecretKeySpec(digestedKey, "AES");

        // Encrypt.
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] encrypted = cipher.doFinal(data);

        // Combine IV and encrypted part.
        byte[] encryptedIVAndText = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, encryptedIVAndText, 0, iv.length);
        System.arraycopy(encrypted, 0, encryptedIVAndText, iv.length, encrypted.length);
        //System.out.println("~ Decrypted (" + encryptedIVAndText.length + "): " + new String(encryptedIVAndText));

        return encryptedIVAndText;
    }

    public static byte[] symDecrypt(byte[] encryptedIVAndText, byte[] key) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        // Extract IV.
        byte[] iv = new byte[16];
        System.arraycopy(encryptedIVAndText, 0, iv, 0, iv.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Extract encrypted part.
        byte[] encrypted = new byte[encryptedIVAndText.length - iv.length];
        System.arraycopy(encryptedIVAndText, iv.length, encrypted, 0, encrypted.length);

        // Hash key.
        byte[] digestedKey = new byte[16];
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(key);
        System.arraycopy(md.digest(), 0, digestedKey, 0, digestedKey.length);
        SecretKeySpec secretKeySpec = new SecretKeySpec(digestedKey, "AES");

        // Decrypt.
        Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        return cipherDecrypt.doFinal(encrypted);
    }
}