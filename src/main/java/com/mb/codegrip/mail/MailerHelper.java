package com.mb.codegrip.mail;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dao.ObjectDAO;
import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.CompanySubscription;
import com.mb.codegrip.dto.Payment;
import com.mb.codegrip.dto.Plans;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.model.CustomPlanModel;
import com.mb.codegrip.model.LandingPageModel;
import com.mb.codegrip.model.PromotionModel;
import com.mb.codegrip.model.UsersModel;
import com.sendgrid.Attachments;
import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Personalization;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.SubscriptionTrackingSetting;
import com.sendgrid.TrackingSettings;

@Component
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:exception.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties"),
		@PropertySource("classpath:notifications.properties"), @PropertySource("classpath:sendgrid.properties"), })
public class MailerHelper {
	private static final Logger LOGGER = Logger.getLogger(MailerHelper.class);

	@Autowired
	private Environment environment;

	@Autowired
	private ObjectDAO objectDao;

	SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

	public void sendMail(String address, String subject, String message) {
		LOGGER.info(subject);
		LOGGER.info(message);
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(address);

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addDynamicTemplateData("project_dashbord_url",
				environment.getProperty(CodeGripConstants.PROJECT_DASHBOARD_URL));

		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		mail.addPersonalization(personalization);
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_WELCOME_TEMPLATE));
		setContentForMail(mail);
	}

	public void sendTokenToMail(String receiverEmailId, String projectName, String subject, String tokenUrl,
			String dashbordImage, String message, String ownerName) {

		Email to = new Email(receiverEmailId);
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		TrackingSettings trackingSettings = new TrackingSettings();
		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		Mail mail = new Mail();
		mail.setSubject(subject);
		Attachments attachment = new Attachments();
		attachment.setContent(dashbordImage);
		attachment.setType("image/jpeg");
		attachment.setFilename("dashbord_screen.jpg");
		attachment.setDisposition("inline");
		attachment.setContentId("Banner");
		mail.addAttachments(attachment);

		SubscriptionTrackingSetting subscriptionTrackingSettingSetting = new SubscriptionTrackingSetting();
		subscriptionTrackingSettingSetting.setEnable(true);

		String dashbordImgUrl = "data:image/jpeg;base64," + dashbordImage;

		trackingSettings.setSubscriptionTrackingSetting(subscriptionTrackingSettingSetting);
		mail.setTrackingSettings(trackingSettings);
		personalization.addTo(to);
		mail.setFrom(from);
		/** mail.setFrom(environment.getProperty(CodeGripConstants.TEAM_CODEGRIP)); */
		personalization.addDynamicTemplateData("project_name", projectName);
		personalization.addDynamicTemplateData("Name_of_user_who_shared", ownerName);
		personalization.addDynamicTemplateData("login_portal_url", tokenUrl);
		personalization.addDynamicTemplateData("dashbord_image", dashbordImgUrl);
		personalization.addDynamicTemplateData("message", message);
		LOGGER.info("Dash Bord image:" + dashbordImgUrl);
		mail.addPersonalization(personalization);
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SEND_PROJECT_TEMPLATE_ID));

		setContentForMail(mail);
	}

	/**************************************************************************************
	 * Get Mail Object set request and response object for mail sending
	 ***************************************************************************************/
	private void setContentForMail(Mail mail) {
		SendGrid sendgrid = new SendGrid(environment.getProperty(CodeGripConstants.SENDGRID_API_KEY));
		Request request = new Request();
		try {
			request.setMethod(Method.POST);
			request.setEndpoint(CodeGripConstants.SENDGRID_ENDPOINT);
			request.setBody(mail.build());
			Response response = sendgrid.api(request);
			LOGGER.info("--> Status: " + response.getStatusCode());
			LOGGER.info("--> Body: " + response.getBody());
			LOGGER.info("--> Header: " + response.getHeaders());
		} catch (IOException ex) {
			LOGGER.error(ex.getMessage());
		}

	}

	/*******************************************************************************************
	 * Send mail to super admin after user registration
	 *******************************************************************************************/
	public void sendRegistrationEmailToSAandAccountant(String email, String subject, String provider) {
		LOGGER.info(subject);
		DateFormat dateFormate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email();

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		Calendar createdAt = Calendar.getInstance();
		to.setEmail(environment.getProperty(CodeGripConstants.SUPER_ADMIN_MAIL));
		Mail mail = new Mail();
		personalization.addTo(to);
		to.setEmail(environment.getProperty(CodeGripConstants.ACCOUNTANT_MAIL));
		personalization.addTo(to);
		mail.setFrom(from);

		personalization.addDynamicTemplateData("username_or_email", email);
		personalization.addDynamicTemplateData("signup_source", provider);
		personalization.addDynamicTemplateData("signup_date_and_time", dateFormate.format(createdAt.getTime())+" (UTC)");
		personalization.addDynamicTemplateData("subscription_plan",
				environment.getProperty(CodeGripConstants.SUBSCRIPTION_PLAN));
		mail.addPersonalization(personalization);
		mail.setTemplateId(environment.getProperty(CodeGripConstants.NEW_USER_SIGN_UP_TEMPLATE));

		LOGGER.info("Mail Send to super admin");
		setContentForMail(mail);
	}

	/**************************************************************************************
	 * Send landing page email to admin.
	 ***************************************************************************************/
	public void sendLandingPageMail(String address, String subject, String message) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(address);
		Content emailContent = new Content(CodeGripConstants.EMAIL_FORMAT_TYPE, message);
		Mail mail = new Mail(from, subject, to, emailContent);
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_LANDING_PAGE_USER_TEMPLATE));
		setContentForMail(mail);
	}

	/**************************************************************************************
	 * Send landing page email to SA.
	 ***************************************************************************************/
	public void receiveLandingPageMail(LandingPageModel landingPageModel, String property, String message) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL));
		LOGGER.info(property);
		LOGGER.info(message);
		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		personalization.addDynamicTemplateData("user_name", landingPageModel.getName());
		personalization.addDynamicTemplateData("user_email", landingPageModel.getReceiverEmail());
		personalization.addDynamicTemplateData("user_phone", landingPageModel.getPhone());
		mail.addPersonalization(personalization);
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_RECEIVE_TEMPLATE));

		setContentForMail(mail);

	}

	/**************************************************************************************
	 * Dynamic template personalization method.
	 ***************************************************************************************/
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

	/**************************************************************************************
	 * Send mail to admin once project get analysed.
	 ***************************************************************************************/
	public void sendProjectScannedMail(String address, String subject, String message) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(address);
		Content emailContent = new Content(CodeGripConstants.EMAIL_FORMAT_TYPE, message);
		Mail mail = new Mail(from, subject, to, emailContent);
		setContentForMail(mail);
	}

	/************************************************************************************
	 * invite user
	 * 
	 * @param receiverMailId
	 * @param companyId
	 * @param name 
	 ************************************************************************************/
	public void inviteUser(String receiverMailId, String companyId, String name) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(receiverMailId);

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		personalization.addDynamicTemplateData(CodeGripConstants.LOGIN_PAGE_URL, companyId);
		personalization.addDynamicTemplateData("inviter_name", name);
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_INVITE_USER_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);

	}

	/************************************************************************************
	 * Send assign project email to user.
	 * 
	 * @param url2
	 ************************************************************************************/
	public void sendAssignProjectMailToUser(String receiverEmailId, String name, String projectName, String url) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(receiverEmailId);

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		String subject = environment.getProperty(CodeGripConstants.SENDGRID_ASSIGN_PROJECT_SUBJECT);
		subject = subject.replace("{{project_name}}", projectName);
		mail.setSubject(subject);
		personalization.addDynamicTemplateData("project_name", projectName);
		personalization.addDynamicTemplateData("invitee_name", name);
		personalization.addDynamicTemplateData(CodeGripConstants.LOGIN_PAGE_URL, url);

		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_INVITE_USER_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);

	}

	/*****************************************************************************************************
	 * Three days before remainder mail
	 * 
	 * @param email
	 ***************************************************************************************************/
	public void threeDaysRemainder(String email,String name) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(email);
		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		personalization.addDynamicTemplateData("x_days", "3");
		personalization.addDynamicTemplateData("orgAdmin_first_name",name);
		personalization.addDynamicTemplateData(CodeGripConstants.LOGIN_PAGE_URL,
				environment.getProperty(CodeGripConstants.UPGRADE_PLAN_TEMPLATE_URL));
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_TRAIL_PERIOD_END_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);

	}

	/*****************************************************************************************************
	 * send mail to user subscribe plan
	 * 
	 * @param email
	 ***************************************************************************************************/
	public void sendSubscriptionSubscribeMail(UsersModel usersModel, CompanySubscription companySubscription) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(usersModel.getEmail());
		Company company = objectDao.getObjectById(Company.class, usersModel.getCompanyId());
		Plans plan = objectDao.getObjectByParam(Plans.class, "stripePlanId", usersModel.getStripePlanId());

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		personalization.addDynamicTemplateData(CodeGripConstants.SEDNGRID_ORG_NAME, company.getName());
		personalization.addDynamicTemplateData(CodeGripConstants.SEDNGRID_ORG_EMAIL, usersModel.getEmail());
		
		personalization.addDynamicTemplateData("old_subscription_plan",usersModel.getOldPlan());
		 
		personalization.addDynamicTemplateData(CodeGripConstants.SENDGRID_NEW_SUBSCRIPTION_PLAN,
				plan.getProducts().getProductName());
		personalization.addDynamicTemplateData(CodeGripConstants.SENDGRID_DATE_AND_TIME,
				formatter.format(companySubscription.getStartDate())+" (UTC)");
		personalization.addDynamicTemplateData(CodeGripConstants.SENDGRID_END_DATE_AND_TIME,
				formatter.format(companySubscription.getEndDate())+" (UTC)");
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_ORG_NEW_PLAN_SUBSCRIBE_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);

	}

	/*****************************************************************************************************
	 * send mail to user canceled subscription
	 * 
	 * @param email
	 ***************************************************************************************************/
	public void sendCancelSubscriptionMailToOrg(CompanySubscription sub) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Users user = objectDao.getObjectById(Users.class, sub.getUserId());
		Email to = new Email(user.getEmail());

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		personalization.addDynamicTemplateData(CodeGripConstants.SEDNGRID_ORG_NAME, sub.getCompany().getName());
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_ORG_PLAN_CANCELED_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);
	}

	/*****************************************************************************************************
	 * send mail to SA/Accounts canceled subscription
	 * 
	 * @param email
	 ***************************************************************************************************/

	public void sendCancelSubscriptionMailToAdmin(CompanySubscription compSub) {

		Users user = objectDao.getObjectById(Users.class, compSub.getUserId());
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL));

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		to = new Email(environment.getProperty(CodeGripConstants.ACCOUNTANT_MAIL));
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		personalization.addDynamicTemplateData(CodeGripConstants.SEDNGRID_ORG_NAME, compSub.getCompany().getName());
		personalization.addDynamicTemplateData("org_admin_username_or_email", user.getEmail());
		personalization.addDynamicTemplateData("old_subscription_plan", compSub.getProducts().getProductName());
		personalization.addDynamicTemplateData(CodeGripConstants.SENDGRID_END_DATE_AND_TIME,
				formatter.format(compSub.getEndDate())+" (UTC)");
		personalization.addDynamicTemplateData(CodeGripConstants.SENDGRID_DATE_AND_TIME,
				formatter.format(new Timestamp(System.currentTimeMillis()))+" (UTC)");
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_ADMIN_PLAN_CANCEL_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);

	}

	/*****************************************************************************************************
	 * send mail to SA subscribe plan
	 * 
	 * @param email
	 ***************************************************************************************************/

	public void sendSubscriptionSubscribeAdminMail(UsersModel usersModel, CompanySubscription companySubscription) {

		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(environment.getProperty(CodeGripConstants.ACCOUNTANT_MAIL));

		Company company = objectDao.getObjectById(Company.class, usersModel.getCompanyId());
		Plans plan = objectDao.getObjectByParam(Plans.class, "stripePlanId", usersModel.getStripePlanId());

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);

		to = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL));
		personalization.addTo(to);

		Mail mail = new Mail();
		mail.setFrom(from);
		personalization.addDynamicTemplateData(CodeGripConstants.SEDNGRID_ORG_NAME, company.getName());
		personalization.addDynamicTemplateData("org_admin_username_or_email", usersModel.getEmail());
		personalization.addDynamicTemplateData("old_subscription_plan",usersModel.getOldPlan());
		personalization.addDynamicTemplateData("new_subscription_plan", plan.getProducts().getProductName());
		personalization.addDynamicTemplateData(CodeGripConstants.SENDGRID_DATE_AND_TIME,
				formatter.format(companySubscription.getStartDate())+" (UTC)");
		personalization.addDynamicTemplateData(CodeGripConstants.SENDGRID_END_DATE_AND_TIME,
				formatter.format(companySubscription.getEndDate())+" (UTC)");
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_ADMIN_NEW_PLAN_SUBSCRIBE_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);

	}

	/*****************************************************************************************************
	 * send mail to user payment received
	 * 
	 * @param email
	 ***************************************************************************************************/

	public void sendOrgPaymentRecivedMail(Payment pay) {
		Plans plan = objectDao.getObjectByParam(Plans.class, "products", pay.getProduct());

		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(pay.getUsers().getEmail());

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);

		personalization.addDynamicTemplateData(CodeGripConstants.SEDNGRID_ORG_NAME, pay.getCompany().getName());
		personalization.addDynamicTemplateData("billing_yearly_or_monthly", plan.getPlanInterval());
		personalization.addDynamicTemplateData(CodeGripConstants.PLAN_NAME, plan.getProducts().getProductName());
		personalization.addDynamicTemplateData("amount_billed", formatter.format(pay.getAmount()));
		personalization.addDynamicTemplateData(CodeGripConstants.BILLING_DATE, formatter.format(pay.getBillingDate())+" (UTC)");
		personalization.addDynamicTemplateData("next_billing_date", formatter.format(pay.getExpiresDate())+" (UTC)");
		personalization.addDynamicTemplateData("purchases_page_url", pay.getInvoiceLink());
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_ORG_PAYMENT_RECIVED_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);

	}

	/*****************************************************************************************************
	 * send mail to SA/Accounts payment received
	 * 
	 * @param email
	 ***************************************************************************************************/

	public void sendAdminPaymentRecivedMail(Payment pay) {
		Plans plan = objectDao.getObjectByParam(Plans.class, "products", pay.getProduct());
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL));

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);

		to = new Email(environment.getProperty(CodeGripConstants.ACCOUNTANT_MAIL));
		personalization.addTo(to);

		Mail mail = new Mail();
		mail.setFrom(from);

		personalization.addDynamicTemplateData(CodeGripConstants.SEDNGRID_ORG_NAME, pay.getCompany().getName());
		personalization.addDynamicTemplateData("billing_yearly_or_monthly", plan.getPlanInterval());
		personalization.addDynamicTemplateData(CodeGripConstants.PLAN_NAME, plan.getProducts().getProductName());
		personalization.addDynamicTemplateData(CodeGripConstants.SENDGRID_AMOUNT_BILLED, pay.getAmount().toString());
		personalization.addDynamicTemplateData(CodeGripConstants.BILLING_DATE, formatter.format(pay.getBillingDate())+" (UTC)");
		personalization.addDynamicTemplateData("next_billing_date", formatter.format(pay.getExpiresDate())+" (UTC)");
		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_ADMIN_PAYMENT_RECIVED_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);

	}

	/************************************************************************************
	 * Send custom plan inquiry email.
	 ************************************************************************************/
	public void sendCustomPlanInquiryEmail(CustomPlanModel customPlanModel) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL));

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		String subject = environment.getProperty(CodeGripConstants.SENDGRID_CUSTOM_INQUIRY_EMAIL_SUBJECT);
		/** subject = subject.replace("{{project_name}}", projectName); */
		mail.setSubject(subject);
		personalization.addDynamicTemplateData("contact_user_email", customPlanModel.getEmail());
		personalization.addDynamicTemplateData("org_name", customPlanModel.getOrganization());
		personalization.addDynamicTemplateData("user_name", customPlanModel.getName());
		personalization.addDynamicTemplateData("message", customPlanModel.getMsg());
		personalization.addDynamicTemplateData("contact_number", customPlanModel.getContactNumber());
		personalization.addDynamicTemplateData("org_size", customPlanModel.getOrgSize().toString());

		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_CUSTOM_PLAN_INQUIRY_ID));
		mail.addPersonalization(personalization);
		setContentForMail(mail);
	}

	/************************************************************************************
	 * Send payment failure email to sa ad manager.
	 ************************************************************************************/
	public void sendPaymentFailureEmailToSAandManager(Company company, Users user, CompanySubscription comSub) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL));

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);

		to = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL_MANAGER));
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		/**
		 * String subject = environment.getProperty(CodeGripConstants.PAYMENT_FAILURE)+"
		 * : "+company.getName();
		 */
		/**
		 * subject = subject.replace("{{project_name}}", projectName);
		 * mail.setSubject(subject);
		 */
		personalization.addDynamicTemplateData("org_name", company.getName());
		personalization.addDynamicTemplateData(CodeGripConstants.PLAN_NAME, comSub.getProducts().getProductName());
		personalization.addDynamicTemplateData("plan_end_date", formatter.format(comSub.getEndDate())+" (UTC)");
		personalization.addDynamicTemplateData("org_admin_email", user.getEmail());
		personalization.addDynamicTemplateData("time_stamp",
				formatter.format(new Timestamp(System.currentTimeMillis()))+" (UTC)");

		mail.setTemplateId(environment.getProperty(CodeGripConstants.PAYMENT_FAILURE_ID_SA));
		mail.addPersonalization(personalization);
		setContentForMail(mail);
	}

	/************************************************************************************
	 * Send payment failure email to admin.
	 ************************************************************************************/
	public void sendPaymentFailureEmailToAdmin(Users user, CompanySubscription comSub) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(user.getEmail());

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		personalization.addDynamicTemplateData("orgAdmin_first_name", user.getName());
		personalization.addDynamicTemplateData("payment_term", comSub.getPlans().getPlanName());

		mail.setTemplateId(environment.getProperty(CodeGripConstants.PAYMENT_FAILURE_ID_ADMIN));
		mail.addPersonalization(personalization);
		setContentForMail(mail);
	}

	
	/************************************************************************************
	 * Send analyzing error mail.
	 ************************************************************************************/
	public void sendEnquiryMail(PromotionModel promotionModel) {
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email(promotionModel.getEmail());

		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addTo(to);
		Mail mail = new Mail();
		mail.setFrom(from);
		personalization.addDynamicTemplateData("signup_url_with_code",
				promotionModel.getEncodedUrl());
		personalization.addDynamicTemplateData("name_invitee",promotionModel.getName());

		mail.setTemplateId(environment.getProperty(CodeGripConstants.SENDGRID_SSGLOBAL_INVITE_TEMPLATE));
		mail.addPersonalization(personalization);
		setContentForMail(mail);
	}

	public void sendExceptionEmail(String subject, int projectBranchId , String message) {
		LOGGER.info(subject);
		LOGGER.info(message);
		Email from = new Email(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL), CodeGripConstants.CODEGRIP);
		Email to = new Email();
		to.setEmail(environment.getProperty(CodeGripConstants.SENDGRID_EMAIL));
		
		DynamicTemplatePersonalization personalization = new DynamicTemplatePersonalization();
		personalization.addDynamicTemplateData("error_subject",subject);
		personalization.addDynamicTemplateData("error_message",message);
		personalization.addDynamicTemplateData("project_branch_id",Integer.toString(projectBranchId));
		personalization.addTo(to);
		
		
		String emailIds[] = environment.getProperty(CodeGripConstants.SEND_ERROR_EMAIL).split(",");
		for (String emailId : emailIds) {
			Email cc = new Email(); 
			cc.setEmail(emailId);
			personalization.addCc(cc);	
		}
		Mail mail = new Mail();
		mail.setFrom(from);
		mail.addPersonalization(personalization);
		mail.setTemplateId(environment.getProperty(CodeGripConstants.ERROR_EMAIL_TEMPLATE_ID));
		setContentForMail(mail);
	}

}
