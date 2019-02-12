package com.mb.codegrip.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.mail.MailerHelper;
import com.mb.codegrip.model.LandingPageModel;

@Service
@PropertySources(value = { @PropertySource("classpath:messages.properties"),@PropertySource("classpath:notifications.properties"), 
		@PropertySource("classpath:exception.properties"), @PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class LandingPageServiceImpl implements LandingPageService, EnvironmentAware{

	@Autowired
	private MailerHelper mailerHelper;
	
	private static Environment environment;
	
	public String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	
	}
	@Override
	public void sendLandingPageMail(String email) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor.execute(()->
				mailerHelper.sendLandingPageMail(email, environment.getProperty(CodeGripConstants.LANDING_PAGE_USER_SUBJECT),
						environment.getProperty(CodeGripConstants.LANDING_PAGE_USER_MESSAGE))
		);
		emailExecutor.shutdown();
		
	}

	@Override
	public void receiveEmail(LandingPageModel landingPageModel) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor.execute(()-> 
				mailerHelper.receiveLandingPageMail(landingPageModel, environment.getProperty(CodeGripConstants.LANDING_PAGE_USER_SUBJECT),
						environment.getProperty(CodeGripConstants.LANDING_PAGE_USER_MESSAGE))
		);
		emailExecutor.shutdown();
		
	}
}
