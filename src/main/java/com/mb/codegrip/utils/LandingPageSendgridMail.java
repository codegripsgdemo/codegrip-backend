package com.mb.codegrip.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sendgrid.Personalization;

public class LandingPageSendgridMail {
	
	private LandingPageSendgridMail() {}
	
	public static class DynamicTemplatePersonalization extends Personalization {

	        @JsonProperty(value = "dynamic_template_data")
	        private Map<String, String> dynamic_template_data;

	        @JsonProperty("dynamic_template_data")
	        public Map<String, String> getDynamicTemplateData() {
	            if (dynamic_template_data == null) {
	                return Collections.<String, String>emptyMap();
	            }
	            return dynamic_template_data;
	        }

	        public void addDynamicTemplateData(String key, String value) {
	            if (dynamic_template_data == null) {
	                dynamic_template_data = new HashMap<>();
	                dynamic_template_data.put(key, value);
	            } else {
	                dynamic_template_data.put(key, value);
	            }
	        }

	    }

}
