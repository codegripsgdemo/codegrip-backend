package com.mb.codegrip.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class PasswordUtil {
	
	private PasswordUtil() {
		
	}
	public static String encode(String rawPassword) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] hashInBytes = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));

		// bytes to hex
		StringBuilder sb = new StringBuilder();
		for (byte b : hashInBytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();

	}
	
	
	public static String encodeToBase64(String value) {
		byte[] byteArray = org.apache.commons.codec.binary.Base64.encodeBase64((value.getBytes()));
		return new String(byteArray);
	}
	
	public static String encodeUrl(String key, String text) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {

		 Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
		 Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
         cipher.init(Cipher.ENCRYPT_MODE, aesKey);
         String encryptedKey= Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes("UTF-8")));
		 return encryptedKey;
	}
	 
	public static String decodeUrl(String key, String encrepatedText) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
			Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher =  Cipher.getInstance("AES/ECB/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, aesKey);
	        String decrypted = new String(cipher.doFinal(Base64.getDecoder().decode(encrepatedText)));
			return decrypted;
	}
}
