package com.mb.codegrip.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MailerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MailerService.class);

  @Autowired
  private MailerHelper mailerHelper;

  @Async
  public void sendMail(String address, String subject, String message) {
	  LOGGER.info("<----------- Mail sender service ------------->");
    mailerHelper.sendMail(address, subject, message);
  }
}
