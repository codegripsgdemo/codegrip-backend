package com.mb.codegrip.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

import com.mb.codegrip.constants.CodeGripConstants;

@Configuration
@EnableResourceServer
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:messages.properties"), @PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

	@Autowired
	Environment environment;

	public ResourceServerConfiguration() {
		super();
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.antMatcher("/**").authorizeRequests()
				.antMatchers(HttpMethod.GET, 
						environment.getProperty(CodeGripConstants.REGISTRATION),
						environment.getProperty(CodeGripConstants.LOGIN),
						environment.getProperty(CodeGripConstants.HEALTH_CHECK),
						environment.getProperty(CodeGripConstants.SWAGGER),
						environment.getProperty(CodeGripConstants.LANDING_PAGE_EMAIL),
						environment.getProperty(CodeGripConstants.WEBHOOK),"*/webhook/commit")
				.permitAll().antMatchers("/projects/*").authenticated();
	}

}
