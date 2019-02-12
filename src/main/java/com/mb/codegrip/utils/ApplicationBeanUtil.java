package com.mb.codegrip.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationBeanUtil implements ApplicationContextAware{

	
	private static  ApplicationContext applicationContext;
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		ApplicationBeanUtil.applicationContext=applicationContext;
	}
	
	
	public static ApplicationContext getApplicationContext()
	{
		return applicationContext;
	}

	public static Object getBean(String name) throws Exception{
		Object object =  applicationContext.getBean(name);
		return object;
	}
}