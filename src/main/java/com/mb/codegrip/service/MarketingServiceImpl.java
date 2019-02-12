package com.mb.codegrip.service;

import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dao.ObjectDAO;
import com.mb.codegrip.dto.Promotion;
import com.mb.codegrip.mail.MailerHelper;
import com.mb.codegrip.model.PromotionModel;
import com.mb.codegrip.utils.PasswordUtil;

@Service
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:notifications.properties"), @PropertySource("classpath:exception.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class MarketingServiceImpl implements MarketingService, EnvironmentAware {
	@Autowired
	private MailerHelper mailerHelper;

	private static Environment environment;
	
	Mapper mapper = new DozerBeanMapper();
	
	private static final Logger LOGGER = Logger.getLogger(PaymentServiceImpl.class);
	
	@Autowired
	private ObjectDAO objectDao;

	public String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;

	}

	@Override
	public void registerSGUser(PromotionModel promotionModel) {
		try {
			promotionModel.setCoupon(environment.getProperty(CodeGripConstants.SSGLOBLE_COUPON));
			Promotion promotion = new Promotion();
			promotion = objectDao.getObjectByParam(Promotion.class,"email", promotionModel.getEmail());
			if(null != promotion) {
				promotion.setName(promotionModel.getName());
				promotion.setCoupon(promotionModel.getCoupon());
			}else {
				promotion = mapper.map(promotionModel,Promotion.class);
				promotion.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				promotion.setCoupon(promotionModel.getCoupon());
			}
			promotion.setIsRegistered(false);
			objectDao.saveObject(promotion);
			
			String encodeCoupon = PasswordUtil.encodeUrl(environment.getProperty(CodeGripConstants.CODEGRIP_SECRET_KEY), promotion.getCoupon());
			promotionModel.setEncodedUrl(environment.getProperty(CodeGripConstants.INVITE_LINK)+"pc="+encodeCoupon);
			
			if(null != promotionModel.getEmail()) {
				sendEnquiryMail(promotionModel);
			}
		}catch(Exception e) {
			LOGGER.error(e.getMessage());
		}
		
	}
	
	/*******************************************************************************************
	 * Send email for enquiry
	 *******************************************************************************************/
	private void sendEnquiryMail(PromotionModel promotionModel) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor.execute(() -> mailerHelper.sendEnquiryMail(promotionModel));
		emailExecutor.shutdown();

	}
	

}
