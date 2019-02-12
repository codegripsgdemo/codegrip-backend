package com.mb.codegrip.utils;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

public class LocaleConverter
{
	 private LocaleConverter(){
		 
	 }
	public static Locale getLocaleFromRequest(HttpServletRequest request)
	{
		String acceptLanguage = request.getHeader("Accept-Language");
		String[] lang = acceptLanguage.split(",");
		String language = lang[0];
		
		return new Locale(language.trim());

	}

}
