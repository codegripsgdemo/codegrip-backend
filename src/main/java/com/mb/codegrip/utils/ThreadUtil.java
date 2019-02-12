package com.mb.codegrip.utils;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transactional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dao.ObjectDAO;
import com.mb.codegrip.dto.CompanySubscription;
import com.mb.codegrip.dto.Plans;
import com.mb.codegrip.dto.ProductFeatures;
import com.mb.codegrip.dto.Products;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CompanySubscriptionModel;
import com.mb.codegrip.model.PaymentModel;
import com.mb.codegrip.repository.UserRepository;
import com.mb.codegrip.service.PaymentService;
import com.mb.codegrip.service.UserService;

@Configuration
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
@EnableScheduling
@Transactional
public class ThreadUtil implements EnvironmentAware {

	private static final Logger LOGGER = Logger.getLogger(ThreadUtil.class);

	private static Environment environment;

	@Autowired
	private ObjectDAO objectDao;

	@Autowired
	private UserRepository userRepository;
	
	CommonUtil commonUtil = new CommonUtil();

	@Autowired
	private UserService userService;
	
	@Autowired
	private PaymentService paymentService;

	public static String getProperty(String key) {
		return environment.getProperty(key);
	}

	@SuppressWarnings("static-access")
	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	/*********************************************************************************************************
	 * Thread to check user subscription is expired or not before following days -
	 * 3,2,1.
	 ********************************************************************************************************/
//	@Scheduled(fixedRate = 10000)
	public void threadToCheckExpiringAccounts() throws CustomException {
		LOGGER.info("---------------- threadToCheckExpiringAccounts started ----------------");
		 List<CompanySubscriptionModel> companySubscriptionModels = objectDao.getExpiringCompanies();
		if (!companySubscriptionModels.isEmpty()) {
			for (CompanySubscriptionModel companySubscriptionModel : companySubscriptionModels) {
				Optional<Users> users = userRepository.findById(companySubscriptionModel.getUserId());

				Integer dayDifference = commonUtil.getDayDifference(new Timestamp(System.currentTimeMillis()),
						companySubscriptionModel.getEndDate());

				String message = environment.getProperty(CodeGripConstants.EXPIRING_SUBSCRIPTION_MESSAGE);
				message = message.replace("<day>", dayDifference.toString());
				
				// Save notification of particular admin.
				userService.saveNotification(message, CodeGripConstants.UNREAD, CodeGripConstants.EXPIRING_SUBSCRIPTION_TITLE, CodeGripConstants.EXPIRING_SUBSCRIPTION_REASON, CodeGripConstants.EXPIRING_SUBSCRIPTION_DESTINATION_PAGE, users.get().getOwnerCompanyId(), users.get().getId(), null,
						CodeGripConstants.IMAGE_CANCEL_SUBSCRIPTION, null,false);

				// Send email to user.
				 /**sendExpiringSubscriptionMail(userProjectsModel.getEmail(), name,
				 projects.getName(),
				 environment.getProperty(CodeGripConstants.ADDED_PROJECT_DASHBOARD_URL));*/

			}
		}
	}

	
	/*********************************************************************************************************
	 * Get day difference method.
	 ********************************************************************************************************/
	private Integer getDayDifference(Timestamp currentDate, Timestamp endDate) {
		long milliseconds = endDate.getTime() - currentDate.getTime();
		int seconds = (int) milliseconds / 1000;
		return (int) TimeUnit.SECONDS.toDays(seconds);
	}
	
	
	/*********************************************************************************************************
	 * check current plan subscription if user count not match then down-grade plan
	 * @throws CustomException
	 *********************************************************************************************************/
	public void checkSubscription() throws CustomException {
		LOGGER.info("---------------- threadToCheckSubscription started ----------------");
		 List<CompanySubscription> companySubscriptionList = objectDao.checkSubscription();
		 if (!companySubscriptionList.isEmpty()) {
			 for (CompanySubscription companySubscription : companySubscriptionList) {
				/** Users users = objectDao.getObjectById(Users.class, companySubscription.getUserId());*/
				 ProductFeatures proFeatures = objectDao.getObjectByParam(ProductFeatures.class,"product", companySubscription.getProducts() );
				 Map<String, Object> userMap = userService.userCountByCompany(companySubscription.getCompany().getId());
				 Integer count =(Integer) userMap.get("currentPlanUserLimit");
				 if(count < proFeatures.getMinUserLimit()) {
					 if(companySubscription.getProducts().getProductName().equalsIgnoreCase(CodeGripConstants.ENTERPRISE_PLAN)) {
						 PaymentModel paymentModel = new PaymentModel();
						 paymentModel.setStripeSubscriptionId(companySubscription.getStripeSubscriptionId());
						 paymentModel.setQuantity(count);
						 paymentModel.setUserId(companySubscription.getUserId());
						 paymentModel.setCompanyId(companySubscription.getCompany().getId());
						 Products product = objectDao.getObjectByParam(Products.class, "productName", CodeGripConstants.BUSINESS_PLAN);
						 Plans plan = objectDao.getObjectByParam(Plans.class, "products", product);
						 paymentModel.setStripePlanId(plan.getStripePlanId());
						 paymentService.editSubscription(paymentModel);
						 
						 /**
						  * Send email when plan down-grade
						  * sendEmailDowngrade(users.getEmail,product.getName());
						  */
						 
					 }
				 }
			 }
			 
		 }
		
		
	}

}
