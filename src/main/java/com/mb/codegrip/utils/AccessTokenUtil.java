package com.mb.codegrip.utils;

import java.util.Base64;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.model.TokenModel;


@Configuration
@PropertySources(value = { @PropertySource("classpath:messages.properties"),

		@PropertySource("classpath:messages.properties"), @PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class AccessTokenUtil implements EnvironmentAware{
	
	private static final Logger LOGGER = Logger.getLogger(AccessTokenUtil.class);
	
	private static Environment environment;

	public static String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}
	
	
	public TokenModel getAccessToken(String email, String password) throws JSONException {
		LOGGER.info("--------------- In get Access token method ----------------");

		String encodedBytes = Base64.getEncoder().encodeToString((environment.getProperty(CodeGripConstants.CLIENT_ID) + ":" + environment.getProperty(CodeGripConstants.CLIENT_SECRET)).getBytes());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
		headers.set("Authorization", CodeGripConstants.BASIC + " " + encodedBytes);
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", environment.getProperty(CodeGripConstants.CLIENT_PASS));
		body.add("username", email);
		body.add("password", password);
		LOGGER.info("Password: "+password);
		LOGGER.info("Email: "+email);
		// Note the body object as first parameter!
		HttpEntity<?> httpEntity = new HttpEntity<>(body, headers);
		RestTemplate restTemplate = new RestTemplate();

		LOGGER.info("Token url: "+environment.getProperty(CodeGripConstants.OAUTH_URL));
		String val = restTemplate.postForObject(environment.getProperty(CodeGripConstants.OAUTH_URL), httpEntity,String.class);
		JSONObject jsonObject = new JSONObject(val);
		TokenModel tokenModel = new TokenModel();
		tokenModel.setAccess_token(jsonObject.getString("access_token"));
		tokenModel.setExpires_in(jsonObject.getInt("expires_in"));
		tokenModel.setRefresh_token(jsonObject.getString("refresh_token"));
		tokenModel.setToken_type(jsonObject.getString("token_type"));
		tokenModel.setScope(jsonObject.getString("scope"));
		return tokenModel;
	}

}
