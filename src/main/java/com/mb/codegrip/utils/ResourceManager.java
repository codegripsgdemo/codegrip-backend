package com.mb.codegrip.utils;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class ResourceManager {
	private static final Logger LOGGER = Logger.getLogger(ResourceManager.class);

	private ResourceManager() {
	}

	@Autowired
	private static MessageSource messageSource;
	static {
		try {
			messageSource = (MessageSource) ApplicationBeanUtil.getBean("messageSource");
		} catch (Exception e) {
			LOGGER.error(e);

		}
	}

	public static String getProperty(String key) {
		return getProperty(key, null);
	}

	public static String getProperty(String key, Object[] params) {
		Locale locale = getRequestLocale();
		return getProperty(key, params, locale);
	}

	/**
	 * This will return Locale associated with current request. Note: This Note:
	 * This Locale object is saved by spring from request headers.
	 * 
	 * @return Locale
	 */
	public static Locale getRequestLocale() {
		Locale locale = null;

		try {
			locale = LocaleContextHolder.getLocale();
		} catch (Exception ex) {
			LOGGER.error(ex);
		}
		return locale;

	}

	/**
	 * 
	 * @param key
	 * @param params
	 * @param locale
	 * @return
	 */
	public static String getProperty(String key, Object[] params, Locale locale) {
		if (messageSource != null) {
			return messageSource.getMessage(key, params, locale);
		} else {
			LOGGER.info("ResourceManager is null...");
			return "";
		}
	}

	public static MessageSource getMessageSource() {
		return messageSource;
	}

	public static void setMessageSource(MessageSource messageSource) {
		ResourceManager.messageSource = messageSource;
	}

	public static void setData(MessageSource messageData) {
		ResourceManager.messageSource = messageData;
	}

	/**
	 * 
	 * @param key
	 * @param params
	 * @param defaultMessage
	 * @param locale
	 * @return
	 */
	public static String getMessage(String key, Object[] params, String defaultMessage, Locale locale) {
		if (messageSource != null) {
			return messageSource.getMessage(key, params, defaultMessage, locale);
		} else {
			LOGGER.info("ResourceManager is null..");
			return "";
		}
	}

	public static String getMessage(String key, String defaultMessage) {
		if (messageSource != null) {
			return messageSource.getMessage(key, null, defaultMessage, null);
		} else {
			LOGGER.info("ResourceManager is null..");
			return "";
		}
	}
}