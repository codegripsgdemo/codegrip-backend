package com.mb.codegrip.service;

import com.mb.codegrip.model.LandingPageModel;

public interface LandingPageService {

	void sendLandingPageMail(String email);

	void receiveEmail(LandingPageModel landingPageModel);
}
