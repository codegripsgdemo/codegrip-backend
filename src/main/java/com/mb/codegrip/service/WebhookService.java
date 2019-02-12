package com.mb.codegrip.service;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CustomPlanModel;

public interface WebhookService {

	void saveCommitDetails(Object webhookObject, HttpServletRequest request, String sourceControll) throws CustomException, JsonProcessingException, JSONException;

	void stripeChargeWebhook(HttpServletRequest request) throws CustomException;

	void contactForCustomPlan(HttpServletRequest request, CustomPlanModel customPlanModel);

}
