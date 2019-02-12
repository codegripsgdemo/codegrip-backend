package com.mb.codegrip.service;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dao.ObjectDAO;
import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.CompanySubscription;
import com.mb.codegrip.dto.Payment;
import com.mb.codegrip.dto.Plans;
import com.mb.codegrip.dto.ProductFeatures;
import com.mb.codegrip.dto.Products;
import com.mb.codegrip.dto.StripeWebhook;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.enums.PaymentStatusEnum;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.mail.MailerHelper;
import com.mb.codegrip.model.PaymentModel;
import com.mb.codegrip.model.PlansModel;
import com.mb.codegrip.model.ProductFeaturesModel;
import com.mb.codegrip.model.ProductModel;
import com.mb.codegrip.model.PurchaseDetailsCustomModel;
import com.mb.codegrip.model.StripeProductModel;
import com.mb.codegrip.model.UsersModel;
import com.mb.codegrip.repository.CompanyRepository;
import com.stripe.Stripe;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Plan;
import com.stripe.model.Product;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;

@Service
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:exception.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties"),
		@PropertySource("classpath:notifications.properties") })
public class PaymentServiceImpl implements PaymentService, EnvironmentAware {

	private static final Logger LOGGER = Logger.getLogger(PaymentServiceImpl.class);

	private static Environment environment;

	Mapper mapper = new DozerBeanMapper();

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private MailerHelper mailerHelper;

	public static String getProperty(String key) {
		return environment.getProperty(key);
	}

	@SuppressWarnings("static-access")
	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	@Autowired
	private ObjectDAO objectDao;

	SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

	@Value("${stripe.api.key}")
	private String stripeApiKey;

	/*******************************************************************************************
	 * Charge subscription
	 *******************************************************************************************/

	@SuppressWarnings("unchecked")
	@Override
	public Payment chargeSubscription(PaymentModel paymentModel) throws CustomException {
		Stripe.apiKey = stripeApiKey;
		Payment payment = null;
		try {

			/** Create subscription on stripe */
			if (null != paymentModel.getStripeCustomerId()) {
				Map<String, Object> customerParams = new HashMap<>();
				customerParams.put(CodeGripConstants.SOURCE, paymentModel.getPurchaseToken());
				Customer stripeCustomer = Customer.retrieve(paymentModel.getStripeCustomerId());
				if (null != paymentModel.getPurchaseToken()) {
					stripeCustomer.update(customerParams);
				}
				Subscription sub = Subscription.retrieve(paymentModel.getStripeSubscriptionId());
				Map<String, Object> item = new HashMap<>();
				item.put("id", sub.getSubscriptionItems().getData().get(0).getId());
				if (null != paymentModel.getStripePlanId()) {
					item.put("plan", paymentModel.getStripePlanId());
				}
				if (null != paymentModel.getQuantity()) {
					item.put(CodeGripConstants.STRIPE_QUANTITY_STRING, paymentModel.getQuantity());
				}
				Map<String, Object> items = new HashMap<>();
				items.put("0", item);

				Map<String, Object> params = new HashMap<>();
				params.put("prorate", true);
				params.put("cancel_at_period_end", false);
				params.put(CodeGripConstants.ITEMS, items);
				Subscription subscription = sub.update(params);
				if (subscription.getPlan().getId().equals(paymentModel.getStripePlanId())) {
					Map<String, Object> invoiceParams = new HashMap<>();
					invoiceParams.put(CodeGripConstants.CUSTOMER, subscription.getCustomer());
					Invoice inv = Invoice.create(invoiceParams);
					if (null != inv) {
						Invoice invoice = Invoice.retrieve(inv.getId());
						invoice.pay();
					}

				}
				LOGGER.info("stripe subscription" + subscription);

				/** save details in the database */
				payment = objectDao.getObjectByParam(Payment.class, CodeGripConstants.STRIPE_CUSTOMER_ID,
						paymentModel.getStripeCustomerId());

				if (payment == null || payment.getCompany() == null) {
					payment = new Payment();
				}
				payment.setStripeCustomerId(paymentModel.getStripeCustomerId());
				payment.setBillingDate(new Timestamp((long) subscription.getCurrentPeriodStart() * 1000));
				payment.setAmount(Double
						.parseDouble("" + ((subscription.getSubscriptionItems().getData().get(0).getPlan().getAmount()
								* subscription.getSubscriptionItems().getData().get(0).getQuantity()) / 100)));
				payment.setExpiresDate(new Timestamp((long) subscription.getCurrentPeriodEnd() * 1000));
				payment.setQuantity(subscription.getQuantity().intValue());
				payment.setStripeSubscriptionId(subscription.getId());

				Users users = objectDao.getObjectById(Users.class, paymentModel.getUserId());
				payment.setUsers(users);

				Company company = objectDao.getObjectById(Company.class, paymentModel.getCompanyId());
				payment.setCompany(company);
				Payment pay = (Payment) objectDao.saveObject(payment);
				CompanySubscription companySubscription = null;
				companySubscription = objectDao.getObjectByParam(CompanySubscription.class,
						CodeGripConstants.STRIPE_SUBSCRIPTION_ID, paymentModel.getStripeSubscriptionId());
				if (null == companySubscription) {
					companySubscription = new CompanySubscription();
				}
				companySubscription.setPayment(pay);

				Plans plans = objectDao.getObjectByParam(Plans.class, CodeGripConstants.STRIPE_PLAN_ID,
						paymentModel.getStripePlanId());
				if (null != plans) {
					Products product = objectDao.getObjectById(Products.class, plans.getProducts().getId());
					if (null != product) {
						companySubscription.setProducts(product);
						pay.setProduct(product);
						objectDao.saveObject(pay);
					}
					companySubscription.setStripeSubscriptionId(subscription.getId());
					companySubscription.setStartDate(new Timestamp((long) subscription.getCurrentPeriodStart() * 1000));
					companySubscription.setEndDate(new Timestamp((long) subscription.getCurrentPeriodEnd() * 1000));
					Object source = stripeCustomer.getSources().getData().get(0);
					if (null != source) {
						ObjectMapper objectMapper = new ObjectMapper();
						Map<String, Object> sourceMapper = objectMapper.convertValue(source, Map.class);
						companySubscription.setCardLastDigit(sourceMapper.get("last4").toString());
						companySubscription.setExpMonth(Long.parseLong(sourceMapper.get("expMonth").toString()));
						companySubscription.setExpYear(Long.parseLong(sourceMapper.get("expYear").toString()));
						companySubscription
								.setFunding(sourceMapper.get(CodeGripConstants.STRIPE_FUNDING_STRING).toString());
						companySubscription.setCardBrand(sourceMapper.get("brand").toString());

					}
					companySubscription.setCompany(company);
					companySubscription.setCreatedAt(new Timestamp(System.currentTimeMillis()));
					companySubscription.setStatus(PaymentStatusEnum.SUBSCRIPTION_ACTIVE.toString());
					companySubscription.setPlans(plans);
					companySubscription.setIsPurchased(true);
					objectDao.saveObject(companySubscription);

					/**
					 * userService.saveNotification(environment.getProperty(CodeGripConstants.SUBSCRIPTION_CREATE_NOTIFICATION),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_UNREAD_STATUS),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_TITLE),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_REASON),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_DESTINATION_PAGE),
					 * company.getId(),users.getId(),null, CodeGripConstants.IMAGE_PAYMENT_SUCCESS);
					 */

				}

			}
		} catch (CardException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.CARD_NOT_VALID));
		} catch (RateLimitException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.TOO_MANY_REQUESTS));
		} catch (InvalidRequestException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_PARAMETERS));
		} catch (AuthenticationException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_STRIPE_API));
		} catch (StripeException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_GENERIC_ERROR));
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
		return payment;
	}

	/*******************************************************************************************
	 * Create product
	 *******************************************************************************************/

	@Override
	public String createProduct(StripeProductModel stripeProductModel) throws CustomException {
		try {
			Map<String, Object> productParam = new HashMap<>();
			productParam.put("name", stripeProductModel.getProductName());
			productParam.put("type", stripeProductModel.getProductType());
			productParam.put("status", stripeProductModel.getIsActive());
			Product.create(productParam);
		} catch (RateLimitException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.TOO_MANY_REQUESTS));
		} catch (InvalidRequestException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_PARAMETERS));
		} catch (AuthenticationException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_STRIPE_API));
		} catch (StripeException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_GENERIC_ERROR));
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
		return null;
	}

	/*******************************************************************************************
	 * update plan
	 *******************************************************************************************/

	@Override
	public Plans saveUpdatePlan(PlansModel plansModel) throws CustomException {
		Plans plans = new Plans();
		try {
			Plan plan = new Plan();
			Map<String, Object> params = new HashMap<>();
			if (null != plansModel.getStripePlanId()) {
				plan = Plan.retrieve(plansModel.getStripePlanId());
				params.put(CodeGripConstants.NICKNAME, plansModel.getPlanName());
				plan.update(params);
			} else {
				params.put("currency", "usd");
				params.put("interval", plansModel.getPlanInterval());
				params.put(CodeGripConstants.PRODUCT, plansModel.getStripeProductId());
				params.put("nickname", plansModel.getPlanName());
				params.put(CodeGripConstants.AMOUNT, plansModel.getAmount() * 100);
				plan = Plan.create(params);
			}
			plans = mapper.map(plansModel, Plans.class);
			if (null != plan) {
				plans.setStripePlanId(plan.getId());
				if (null != plansModel.getStripePlanId()) {
					plans.setUpdatedAt(new Date());
				} else {
					plans.setCreatedAt(new Date());
				}
				objectDao.saveObject(plans);
			}

		} catch (CardException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.CARD_NOT_VALID));
		} catch (RateLimitException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.TOO_MANY_REQUESTS));
		} catch (InvalidRequestException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_PARAMETERS));
		} catch (AuthenticationException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_STRIPE_API));
		} catch (StripeException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_GENERIC_ERROR));
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
		return plans;
	}

	/*******************************************************************************************
	 * Update subscription
	 *******************************************************************************************/

	@SuppressWarnings("unchecked")
	@Override
	public Payment editSubscription(PaymentModel paymentModel) throws CustomException {
		Stripe.apiKey = stripeApiKey;
		Payment payment = new Payment();
		try {
			Customer stripeCustomer = null;
			if (null != paymentModel.getPurchaseToken() && null != paymentModel.getStripeCustomerId()) {
				Map<String, Object> customerParams = new HashMap<>();
				customerParams.put(CodeGripConstants.SOURCE, paymentModel.getPurchaseToken());
				stripeCustomer = Customer.retrieve(paymentModel.getStripeCustomerId());
				stripeCustomer.update(customerParams);
			}
			Subscription sub = Subscription.retrieve(paymentModel.getStripeSubscriptionId());
			Map<String, Object> item = new HashMap<>();
			item.put("id", sub.getSubscriptionItems().getData().get(0).getId());
			Plans plan = null;
			if (null != paymentModel.getStripePlanId()) {
				item.put(CodeGripConstants.PLAN, paymentModel.getStripePlanId());
				plan = objectDao.getObjectByParam(Plans.class, "stripePlanId", paymentModel.getStripePlanId());
				if (null != plan.getProducts()) {
					payment.setProduct(plan.getProducts());
				}
			} else {
				plan = new Plans();
			}
			if (null != paymentModel.getQuantity()) {
				item.put(CodeGripConstants.STRIPE_QUANTITY_STRING, paymentModel.getQuantity());
			}
			Map<String, Object> items = new HashMap<>();
			items.put("0", item);

			Map<String, Object> params = new HashMap<>();
			params.put("prorate", true);
			params.put("cancel_at_period_end", false);
			params.put(CodeGripConstants.ITEMS, items);

			Subscription subscription = sub.update(params);
			if (sub.getPlan().getId().equals(paymentModel.getStripePlanId())) {
				Map<String, Object> invoiceParams = new HashMap<>();
				invoiceParams.put(CodeGripConstants.CUSTOMER, subscription.getCustomer());
				Invoice inv = Invoice.create(invoiceParams);
				if (null != inv) {
					Invoice invoice = Invoice.retrieve(inv.getId());
					invoice.pay();
					payment.setStripeInvoiceId(invoice.getId());
				}
			} else {
				payment.setBillingDate(new Timestamp(System.currentTimeMillis()));
			}
			payment.setStripeCustomerId(subscription.getCustomer());
			payment.setExpiresDate(new Timestamp((long) subscription.getCurrentPeriodEnd() * 1000));
			payment.setStripeSubscriptionId(subscription.getId());
			payment.setQuantity(subscription.getQuantity().intValue());

			Company company = objectDao.getObjectById(Company.class, paymentModel.getCompanyId());
			payment.setCompany(company);
			Users users = objectDao.getObjectById(Users.class, paymentModel.getUserId());
			payment.setUsers(users);

			CompanySubscription compSub = objectDao.getObjectByParam(CompanySubscription.class,
					CodeGripConstants.COMPANY, company);
			Timestamp currentDate = new Timestamp(System.currentTimeMillis());
			LocalDateTime firstDate = currentDate.toLocalDateTime();
			LocalDate date = firstDate.toLocalDate();
			LocalDateTime secondDate = compSub.getEndDate().toLocalDateTime();
			LocalDate date2 = secondDate.toLocalDate();
			if (date2.isAfter(date)) {
				payment.setStripeStatus("SUBSCRIPTION_UPDATED");
			}
			payment.setBillingDate(new Timestamp(System.currentTimeMillis()));
			payment.setIsDeleted(false);
			Payment pay = (Payment) objectDao.saveObject(payment);

			if (null != compSub) {
				compSub.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
				compSub.setPayment(pay);
				compSub.setPlans(plan);
				compSub.setProducts(plan.getProducts());
				if (null != stripeCustomer) {
					Object source = stripeCustomer.getSources().getData().get(0);
					if (null != source) {
						ObjectMapper objectMapper = new ObjectMapper();
						Map<String, Object> sourceMapper = objectMapper.convertValue(source, Map.class);
						compSub.setCardLastDigit(sourceMapper.get("last4").toString());
						compSub.setExpMonth(Long.parseLong(sourceMapper.get("expMonth").toString()));
						compSub.setExpYear(Long.parseLong(sourceMapper.get("expYear").toString()));
						compSub.setFunding(sourceMapper.get(CodeGripConstants.STRIPE_FUNDING_STRING).toString());
						compSub.setCardBrand(sourceMapper.get("brand").toString());
						pay.setCrardType(sourceMapper.get(CodeGripConstants.STRIPE_FUNDING_STRING).toString());
						pay.setStripeCardId(sourceMapper.get("id").toString());
						objectDao.saveObject(pay);

					}

				}
				objectDao.saveObject(compSub);
			}

			/**
			 * Notification notification = new Notification();
			 * notification.setCompanyId(company.getId());
			 * notification.setReceiverId(users.getId()); Calendar calendar =
			 * Calendar.getInstance(); Timestamp currentTimestamp = new
			 * java.sql.Timestamp(calendar.getTime().getTime());
			 * notification.setCreatedAt(currentTimestamp);
			 * notification.setMessage(environment.getProperty(CodeGripConstants.SUBSCRIPTION_EDIT_NOTIFICATION));
			 * notification.setStatus(environment.getProperty(CodeGripConstants.NOTIFICATION_UNREAD_STATUS));
			 * notification.setTitle(environment.getProperty(CodeGripConstants.NOTIFICATION_EDIT_SUBSCRIPTION_TITLE));
			 * notification.setReason(environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_REASON));
			 * notification.setDestinationPage(
			 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_DESTINATION_PAGE));
			 * notification.setImageId(CodeGripConstants.IMAGE_PAYMENT_SUCCESS);
			 * notification.setIsDeleted(false); notification.setIsBroadcast(false);
			 * notification.setIsPrivate(false); objectDao.saveObject(notification);
			 */

		} catch (CardException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.CARD_NOT_VALID));
		} catch (RateLimitException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.TOO_MANY_REQUESTS));
		} catch (InvalidRequestException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_PARAMETERS));
		} catch (AuthenticationException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_STRIPE_API));
		} catch (StripeException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_GENERIC_ERROR));
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
		return payment;
	}

	/*******************************************************************************************
	 * Cancel subscription
	 *******************************************************************************************/

	@Override
	public void cancelSubscription(PaymentModel paymentModel) throws CustomException {
		try {
			Stripe.apiKey = stripeApiKey;
			Subscription sub = Subscription.retrieve(paymentModel.getStripeSubscriptionId());
			Company company = objectDao.getObjectById(Company.class, paymentModel.getCompanyId());
			if (null != company) {
				CompanySubscription compSub = objectDao.getObjectByParam(CompanySubscription.class, "company", company);
				if (null != compSub) {
					compSub.setStatus(PaymentStatusEnum.SUBSCRIPTION_CANCELED.toString());
					compSub.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
					objectDao.saveObject(compSub);
				}
				sub.cancel(null);
				mailerHelper.sendCancelSubscriptionMailToOrg(compSub);
				mailerHelper.sendCancelSubscriptionMailToAdmin(compSub);
				/**
				 * userService.saveNotification(environment.getProperty(CodeGripConstants.SUBSCRIPTION_CANCEL_NOTIFICATION),
				 * environment.getProperty(CodeGripConstants.NOTIFICATION_UNREAD_STATUS),
				 * environment.getProperty(CodeGripConstants.NOTIFICATION_CANCEL_SUBSCRIPTION_TITLE),
				 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_REASON),
				 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_DESTINATION_PAGE),
				 * paymentModel.getCompanyId(),paymentModel.getUserId(),null,
				 * CodeGripConstants.IMAGE_CANCEL_SUBSCRIPTION);
				 */
			}

		} catch (RateLimitException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.TOO_MANY_REQUESTS));
		} catch (InvalidRequestException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_PARAMETERS));
		} catch (AuthenticationException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_STRIPE_API));
		} catch (StripeException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_GENERIC_ERROR));
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
	}

	/*******************************************************************************************
	 * Update card
	 *******************************************************************************************/

	@SuppressWarnings("unchecked")
	@Override
	public void updateCard(PaymentModel paymentModel) throws CustomException {
		Customer cust = null;
		try {
			Stripe.apiKey = stripeApiKey;
			cust = Customer.retrieve(paymentModel.getStripeCustomerId());
			Map<String, Object> updateParams = new HashMap<>();
			updateParams.put("source", paymentModel.getPurchaseToken());
			cust = cust.update(updateParams);
			Object source = cust.getSources().getData().get(0);
			ObjectMapper objectMapper = new ObjectMapper();
			Company company = objectDao.getObjectById(Company.class, paymentModel.getCompanyId());
			CompanySubscription comSub = objectDao.getObjectByParam(CompanySubscription.class, "company", company);
			if (null != comSub) {
				Map<String, Object> sourceMapper = objectMapper.convertValue(source, Map.class);
				comSub.setCardLastDigit(sourceMapper.get("last4").toString());
				comSub.setExpMonth(Long.parseLong(sourceMapper.get("expMonth").toString()));
				comSub.setExpYear(Long.parseLong(sourceMapper.get("expYear").toString()));
				comSub.setFunding(sourceMapper.get(CodeGripConstants.STRIPE_FUNDING_STRING).toString());
				comSub.setCardBrand(sourceMapper.get("brand").toString());
				objectDao.saveObject(comSub);
			}

			/**
			 * userService.saveNotification(environment.getProperty(CodeGripConstants.CARD_UPDATE_NOTIFICATION),
			 * environment.getProperty(CodeGripConstants.NOTIFICATION_UNREAD_STATUS),
			 * environment.getProperty(CodeGripConstants.CARD_UPDATE_TITLE),
			 * environment.getProperty(CodeGripConstants.CARD_UPDATE_CHARGED_REASON),
			 * environment.getProperty(CodeGripConstants.CARD_UPDATE_DESTINATION_PAGE),
			 * paymentModel.getCompanyId(),paymentModel.getUserId(),null);
			 */

		} catch (CardException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.CARD_NOT_VALID));
		} catch (RateLimitException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.TOO_MANY_REQUESTS));
		} catch (InvalidRequestException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_PARAMETERS));
		} catch (AuthenticationException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_STRIPE_API));
		} catch (StripeException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_GENERIC_ERROR));
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
	}

	/*******************************************************************************************
	 * Manage webhook
	 * 
	 * @throws IOException
	 *******************************************************************************************/

	@SuppressWarnings({ "unchecked" })
	@Override
	public void handleWebhook(HttpServletRequest request) throws CustomException, IOException {
		String rawJson = "";
		Stripe.apiKey = stripeApiKey;
		String sigHeader = request.getHeader("Stripe-Signature");
		Event event = null;
		try {
			rawJson = IOUtils.toString(request.getInputStream());
			event = Webhook.constructEvent(rawJson, sigHeader,
					environment.getProperty(CodeGripConstants.STRIPE_SIGNING_SECRET));

		} catch (IOException ex) {
			LOGGER.error(ex.getMessage());
		} catch (JsonSyntaxException e) {
			LOGGER.error(e.getMessage());
		} catch (SignatureVerificationException e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_SIGNATURE_NOT_VALID));
		}
		event = Event.GSON.fromJson(rawJson, Event.class);
		// Converting event object to map
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> props = objectMapper.convertValue(event.getData(), Map.class);

		// Getting required data
		Object dataMap = props.get("object");
		Map<String, String> dataMapper = objectMapper.convertValue(dataMap, Map.class);
		try {
			Object source = dataMapper.get(CodeGripConstants.SOURCE);
			Map<String, String> sourceMapper = objectMapper.convertValue(source, Map.class);

			if (event.getType().equals("charge.succeeded")) {
				Payment payment = null;
				payment = objectDao.getObjectByParam(Payment.class, CodeGripConstants.STRIPE_CUSTOMER_ID,
						dataMapper.get(CodeGripConstants.CUSTOMER));
				if (payment == null || payment.getStripeStatus() != null) {
					payment = new Payment();
				}
				if (null != sourceMapper.get("funding")) {
					payment.setCrardType(sourceMapper.get("funding"));
				}
				Object amt = dataMapper.get(CodeGripConstants.AMOUNT);
				payment.setStripeCardId(sourceMapper.get("id"));
				payment.setTransactionId(dataMapper.get("id"));
				payment.setAmount(Double.parseDouble(amt.toString()) / 100);
				payment.setStripeStatus(PaymentStatusEnum.CHARGE_SUCCEEDED.toString());
				payment.setBillingDate(new Timestamp(System.currentTimeMillis()));
				payment.setStripeInvoiceId(dataMapper.get("invoice"));
				Invoice invoice = Invoice.retrieve(dataMapper.get("invoice"));
				payment.setStripeInvoiceId(invoice.getId());
				payment.setInvoiceLink(invoice.getInvoicePdf());
				Customer cust = Customer.retrieve(dataMapper.get(CodeGripConstants.CUSTOMER));
				CompanySubscription comSub = objectDao.getObjectByParam(CompanySubscription.class,
						CodeGripConstants.STRIPE_SUBSCRIPTION_ID, cust.getSubscriptions().getData().get(0).getId());
				if (null != comSub) {
					payment.setCompany(comSub.getCompany());
					Users user = objectDao.getObjectById(Users.class, comSub.getUserId());
					payment.setUsers(user);
					payment.setProduct(comSub.getProducts());
				}
				payment.setExpiresDate(
						new Timestamp(cust.getSubscriptions().getData().get(0).getCurrentPeriodEnd() * 1000));
				payment.setStripeSubscriptionId(cust.getSubscriptions().getData().get(0).getId());
				payment.setStripeCustomerId(dataMapper.get(CodeGripConstants.CUSTOMER));
				Payment pay = (Payment) objectDao.saveObject(payment);

				if (null != comSub) {
					comSub.setEndDate(
							new Timestamp(cust.getSubscriptions().getData().get(0).getCurrentPeriodEnd() * 1000));
					comSub.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
					comSub.setPayment(pay);
					comSub.setIsPurchased(true);
					objectDao.saveObject(comSub);
				}
				mailerHelper.sendOrgPaymentRecivedMail(pay);
				mailerHelper.sendAdminPaymentRecivedMail(pay);

				/**
				 * userService.saveNotification(
				 * environment.getProperty(CodeGripConstants.SUBSCRIPTION_CHARGED_SUCCESSFULL_NOTIFICATION),
				 * environment.getProperty(CodeGripConstants.NOTIFICATION_UNREAD_STATUS),
				 * environment.getProperty(CodeGripConstants.NOTIFICATION_CHARGED_SUBSCRIPTION_TITLE),
				 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_CHARGED_REASON),
				 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_CHARGE_DESTINATION_PAGE),
				 * payment.getCompany().getId(), payment.getUsers().getId(), null, null,
				 * "payment", false);
				 */

				LOGGER.info("charged successfully");
			} else if (event.getType().equals("charge.failed")) {
				LOGGER.info("charged failed");
				Payment payment = new Payment();

				Object amt = dataMapper.get(CodeGripConstants.AMOUNT);
				payment.setStripeCardId(sourceMapper.get("id"));
				payment.setTransactionId(dataMapper.get("id"));
				payment.setAmount(Double.parseDouble(amt.toString()) / 100);
				payment.setStripeStatus(PaymentStatusEnum.CHARGE_FAIL.toString());
				payment.setBillingDate(new Timestamp(System.currentTimeMillis()));
				Customer cust = Customer.retrieve(dataMapper.get(CodeGripConstants.CUSTOMER));
				CompanySubscription comSub = objectDao.getObjectByParam(CompanySubscription.class,
						CodeGripConstants.STRIPE_SUBSCRIPTION_ID, cust.getSubscriptions().getData().get(0).getId());
				payment.setCompany(comSub.getCompany());

				Users user = objectDao.getObjectById(Users.class, comSub.getUserId());
				payment.setUsers(user);
				payment.setProduct(comSub.getProducts());
				Payment pay = (Payment) objectDao.saveObject(payment);

				sendPaymentFailedEmailToSAandManager(user, comSub);
				sendPaymentFailedEmailToAdmin(user, comSub);

				// send notification for admin.
				userService.saveNotification(environment.getProperty(CodeGripConstants.PAYMENT_FAILURE_MESSAGE),
						CodeGripConstants.UNREAD, environment.getProperty(CodeGripConstants.PAYMENT_FAILURE),
						environment.getProperty(CodeGripConstants.PAYMENT_FAILURE), null, user.getOwnerCompanyId(),
						user.getId(), null, null, null, false);

				// save notification for super admin.
				userService.saveNotification(environment.getProperty(CodeGripConstants.PAYMENT_FAILURE_MESSAGE),
						CodeGripConstants.UNREAD, environment.getProperty(CodeGripConstants.PAYMENT_FAILURE),
						environment.getProperty(CodeGripConstants.PAYMENT_FAILURE), null, user.getOwnerCompanyId(),
						user.getId(), null, CodeGripConstants.IMAGE_PAYMENT_FAILED, "payment", false);

				if (null != comSub) {
					comSub.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
					comSub.setStatus(PaymentStatusEnum.SUBSCRIPTION_INACTIVE.toString());
					comSub.setPayment(pay);
					comSub.setIsPurchased(false);
					objectDao.saveObject(comSub);
					/**
					 * userService.saveNotification(environment.getProperty(CodeGripConstants.SUBSCRIPTION_CHARGED_FAIL_NOTIFICATION),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_UNREAD_STATUS),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_CHARGE_FAIL_SUBSCRIPTION_TITLE),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_CHARGE_FAIL_REASON),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_CHARGE_FAIL_DESTINATION_PAGE),
					 * payment.getCompany().getId(),payment.getUsers().getId(),null);
					 */
				}

			} else if (event.getType().equals("customer.subscription.deleted")) {
				LOGGER.info("subscription canceled");
				Payment payment = objectDao.getObjectByParam(Payment.class, CodeGripConstants.STRIPE_CUSTOMER_ID,
						dataMapper.get(CodeGripConstants.CUSTOMER));
				if (null != payment) {
					CompanySubscription comSub = objectDao.getObjectByParam(CompanySubscription.class,
							CodeGripConstants.STRIPE_SUBSCRIPTION_ID, payment.getStripeSubscriptionId());
					if (null != comSub) {
						comSub.setStatus(PaymentStatusEnum.SUBSCRIPTION_CANCELED.toString());
						comSub.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
						comSub.setIsPurchased(false);
						objectDao.saveObject(comSub);
					}

				}
			} else if (event.getType().equals("invoice.payment_succeeded")) {
				LOGGER.info("invoice payment succeeded");
				Payment payment = objectDao.getObjectByParam(Payment.class, "stripeCustomerId",
						dataMapper.get("customer"));
				payment.setInvoiceLink(dataMapper.get("invoicePdf"));
				objectDao.saveObject(payment);
			} else if (event.getType().equals("customer.subscription.trial_will_end")) {
				LOGGER.info("customer trial period will end in 3 days");
				Payment payment = objectDao.getObjectByParam(Payment.class, CodeGripConstants.STRIPE_CUSTOMER_ID,
						dataMapper.get(CodeGripConstants.CUSTOMER));
				Users user = null;
				if (null != payment) {
					user = objectDao.getObjectById(Users.class, payment.getUsers().getId());
					mailerHelper.threeDaysRemainder(user.getEmail(), user.getName());
				}

				// save notification for super admin.
				/**
				 * userService.saveNotification(environment.getProperty(CodeGripConstants.TRIAL_END_MESSAGE),
				 * CodeGripConstants.UNREAD,
				 * environment.getProperty(CodeGripConstants.TRIAL_END_TITLE),
				 * environment.getProperty(CodeGripConstants.PAYMENT_FAILURE), null,
				 * user.getOwnerCompanyId(), user.getId(), null,
				 * CodeGripConstants.IMAGE_PAYMENT_FAILED, "payment", false);
				 */

			} else {
				StripeWebhook stripeWebhook = new StripeWebhook();
				stripeWebhook.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				stripeWebhook.setStripeCustomerId(dataMapper.get(CodeGripConstants.CUSTOMER));
				stripeWebhook.setData(event.getType());
				stripeWebhook.setStripeEventId(event.getId());
				objectDao.saveObject(stripeWebhook);
			}

		} catch (Exception exc) {
			LOGGER.error(exc.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/*******************************************************************************************
	 * Send email to super admin and manager if payment get failed.
	 *******************************************************************************************/
	private void sendPaymentFailedEmailToSAandManager(Users user, CompanySubscription comSub) {
		Company company = companyRepository.findById(user.getOwnerCompanyId());
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor.execute(() -> mailerHelper.sendPaymentFailureEmailToSAandManager(company, user, comSub));
		emailExecutor.shutdown();

	}

	/*******************************************************************************************
	 * Send email to admin if payment get failed.
	 *******************************************************************************************/
	private void sendPaymentFailedEmailToAdmin(Users user, CompanySubscription comSub) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor.execute(() -> mailerHelper.sendPaymentFailureEmailToAdmin(user, comSub));
		emailExecutor.shutdown();

	}

	/*******************************************************************************************
	 * Plan, Product list
	 *******************************************************************************************/

	@SuppressWarnings("unchecked")
	@Override
	public Object getPlanListByProduct() {
		Map<Object, Object> productPlan = null;
		List<Object> result = new ArrayList<>();
		try {
			List<Products> list = (List<Products>) objectDao.listObject(Products.class, "id");
			if (null != list) {
				PlansModel planModel = null;
				ProductModel proModel = null;
				Map<String, Object> proPlan = null;
				for (Products pro : list) {
					proPlan = new HashMap<>();
					productPlan = new HashMap<>();
					proModel = new ProductModel();
					proModel.setProductName(pro.getProductName());
					proModel.setId(pro.getId());
					proModel.setStripeProductId(pro.getStripeProductId());
					proModel.setDescription(pro.getDescription());
					List<Plans> planList = (List<Plans>) objectDao.listObjectByParam(Plans.class, "products", pro);
					for (Plans plan : planList) {
						planModel = new PlansModel();
						planModel.setId(plan.getId());
						planModel.setAmount(plan.getAmount());
						planModel.setPlanName(plan.getPlanName());
						planModel.setStripePlanId(plan.getStripePlanId());
						proPlan.put(plan.getPlanInterval(), planModel);

					}

					ProductFeatures productFeatures = objectDao.getObjectByParam(ProductFeatures.class, "product", pro);
					ProductFeaturesModel proFeature = mapper.map(productFeatures, ProductFeaturesModel.class);
					proFeature.setProduct(null);
					proModel.setProductFeatures(proFeature);

					proModel.setPlansModelMap(proPlan);
					productPlan.put("product", proModel);

					result.add(productPlan);
				}

			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		return result;
	}

	/*******************************************************************************************
	 * update product with plan
	 *******************************************************************************************/
	@Override
	public Plans saveUpdateProductWithPlan(ProductModel productModel) throws CustomException {
		try {
			Products productDTO = new Products();
			Map<String, Object> productParams = new HashMap<>();
			productParams.put("name", productModel.getProductName());
			productParams.put("type", "service");
			Product product = Product.create(productParams);
			productDTO.setCreatedAt(new Date());
			productDTO.setProductName(productModel.getProductName());
			productDTO.setStripeProductId(product.getId());
			productDTO.setDescription(productModel.getDescription());
			Products pro = (Products) objectDao.saveObject(productDTO);

			Plans planDto = null;
			if (null != productModel.getPlansModel()) {
				for (PlansModel plansModel : productModel.getPlansModel()) {
					planDto = new Plans();
					Map<String, Object> params = new HashMap<>();
					params.put("currency", "usd");
					params.put("interval", plansModel.getPlanInterval());
					params.put("product", product.getId());
					params.put(CodeGripConstants.NICKNAME, plansModel.getPlanName());
					params.put("amount", plansModel.getAmount() * 100);
					Plan stripePlan = Plan.create(params);
					planDto = mapper.map(plansModel, Plans.class);
					planDto.setStripePlanId(stripePlan.getId());
					planDto.setCreatedAt(new Date());
					planDto.setProducts(pro);
					planDto.setPlanStatus("ACTIVE");
					planDto.setPlanInterval(plansModel.getPlanInterval());
					objectDao.saveObject(planDto);
				}

			}
		} catch (StripeException e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}

	/*******************************************************************************************
	 * add startup plan to when new registration of admin.
	 * 
	 * @throws CustomException
	 *******************************************************************************************/
	@Override
	public void saveStartupPlan(CompanySubscription createStartupSubscriptionObejct) throws CustomException {
		try {
			objectDao.saveObject(createStartupSubscriptionObejct);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/*******************************************************************************************
	 * Purchase details list by user
	 * 
	 * @throws CustomException
	 *******************************************************************************************/

	@SuppressWarnings("unchecked")
	@Override
	public List<PurchaseDetailsCustomModel> getPurchaseList(Integer userId) throws CustomException {
		List<PurchaseDetailsCustomModel> purchaseDetailsList = new ArrayList<>();
		try {
			Users user = objectDao.getObjectById(Users.class, userId);
			PurchaseDetailsCustomModel purchaseModel = null;
			List<Payment> paymentList = (List<Payment>) objectDao.listObjectByParam(Payment.class, "users", user);
			for (Payment payment : paymentList) {
				purchaseModel = new PurchaseDetailsCustomModel();
				purchaseModel.setAmount(payment.getAmount());
				purchaseModel.setPaymentDate(formatter.format(payment.getBillingDate()));
				if (null != payment.getProduct()) {
					purchaseModel.setPlanName(payment.getProduct().getProductName());
				}
				purchaseModel.setTransactionId(payment.getTransactionId());
				purchaseModel.setUserCount(payment.getQuantity());
				purchaseModel.setInvoiceLink(payment.getInvoiceLink());
				purchaseModel.setBillingPeriod(formatter.format(payment.getBillingDate()) + " to "
						+ formatter.format(payment.getExpiresDate()));
				purchaseDetailsList.add(purchaseModel);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return purchaseDetailsList;
	}

	@Override
	public void createCustomer(UsersModel usersModel) throws CustomException {
		try {
			Stripe.apiKey = stripeApiKey;
			/** Create customer on stripe */
			Map<String, Object> customerParams = new HashMap<>();

			if (null != usersModel.getEmail()) {
				customerParams.put("email", usersModel.getEmail());
			}
			Map<String, Object> shipping = new HashMap<>();
			Map<String, String> address = new HashMap<>();
			if (null != usersModel.getAddress()) {
				address.put("line1", usersModel.getAddress());
				shipping.put("address", address);
				customerParams.put("shipping", shipping);
			}

			shipping.put("name", usersModel.getName());
			Customer stripeCustomer = Customer.create(customerParams);
			Map<String, Object> item = new HashMap<>();
			item.put("plan", usersModel.getStripePlanId());
			if (usersModel.getQuantity() != null)
				item.put("quantity", usersModel.getQuantity());

			Map<String, Object> items = new HashMap<>();
			items.put("0", item);

			Map<String, Object> params = new HashMap<>();
			params.put("customer", stripeCustomer.getId());
			params.put(CodeGripConstants.ITEMS, items);
			if (null != usersModel.getTrailPeriodDays()) {
				params.put("trial_period_days", usersModel.getTrailPeriodDays());
			} else {
				params.put("trial_period_days", CodeGripConstants.TRAIL_PERIOD_DAYS);
			}
			Subscription sub = Subscription.create(params);

			CompanySubscription companySubscription = null;
			Products oldProeduct = null;
			Company company = objectDao.getObjectById(Company.class, usersModel.getCompanyId());
			companySubscription = objectDao.getObjectByParam(CompanySubscription.class, "company", company);
			if (null == companySubscription) {
				companySubscription = new CompanySubscription();
			} else {

				oldProeduct = objectDao.getObjectById(Products.class, companySubscription.getProducts().getId());
			}
			companySubscription.setCompany(company);
			companySubscription.setStripeSubscriptionId(sub.getId());
			companySubscription.setStartDate(new Timestamp((long) sub.getCurrentPeriodStart() * 1000));
			companySubscription.setEndDate(new Timestamp((long) sub.getCurrentPeriodEnd() * 1000));
			companySubscription.setCompany(company);
			companySubscription.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			companySubscription.setStatus(PaymentStatusEnum.SUBSCRIPTION_ACTIVE.toString());
			companySubscription.setIsPurchased(false);
			companySubscription.setIsDeleted(false);

			Users user = objectDao.getObjectById(Users.class, usersModel.getUserId());
			companySubscription.setUserId(user.getId());

			Plans plans = objectDao.getObjectByParam(Plans.class, CodeGripConstants.STRIPE_PLAN_ID,
					usersModel.getStripePlanId());
			companySubscription.setPlans(plans);

			Products product = objectDao.getObjectById(Products.class, plans.getProducts().getId());
			companySubscription.setProducts(product);

			Payment payment = new Payment();
			payment.setAmount(0.0);
			payment.setBillingDate(new Timestamp(System.currentTimeMillis()));
			payment.setExpiresDate(new Timestamp((long) sub.getCurrentPeriodEnd() * 1000));
			payment.setCompany(company);
			payment.setQuantity(usersModel.getQuantity());
			payment.setProduct(product);
			payment.setStripeCustomerId(stripeCustomer.getId());
			payment.setStripeSubscriptionId(sub.getId());
			payment.setStripeStatus(PaymentStatusEnum.CHARGE_SUCCEEDED.toString());
			payment.setUsers(user);
			Payment pay = (Payment) objectDao.saveObject(payment);

			companySubscription.setPayment(pay);
			objectDao.saveObject(companySubscription);

			user.setIsFreeTrialStarted(true);
			user.setTrialStartDate(new Timestamp((long) sub.getCurrentPeriodStart() * 1000));
			user.setTialExpiresDate(new Timestamp((long) sub.getCurrentPeriodEnd() * 1000));
			objectDao.saveObject(user);

			if (null != oldProeduct) {
				usersModel.setOldPlan(oldProeduct.getProductName());
			}
			mailerHelper.sendSubscriptionSubscribeMail(usersModel, companySubscription);
			mailerHelper.sendSubscriptionSubscribeAdminMail(usersModel, companySubscription);
		} catch (CardException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.CARD_NOT_VALID));
		} catch (RateLimitException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.TOO_MANY_REQUESTS));
		} catch (InvalidRequestException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_PARAMETERS));
		} catch (AuthenticationException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_STRIPE_API));
		} catch (StripeException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_GENERIC_ERROR));
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	@Override
	public Payment chargeNewSubscription(PaymentModel paymentModel) throws CustomException {
		Stripe.apiKey = stripeApiKey;
		Payment payment = null;
		try {
			/** Create customer on stripe */
			Map<String, Object> customerParams = new HashMap<>();

			customerParams.put(CodeGripConstants.SOURCE, paymentModel.getPurchaseToken());
			Customer stripeCustomer = Customer.retrieve(paymentModel.getStripeCustomerId());
			stripeCustomer.update(customerParams);
			/** Create subscription on stripe */
			if (null != stripeCustomer.getId()) {
				Map<String, Object> item = new HashMap<>();
				item.put("plan", paymentModel.getStripePlanId());
				item.put("quantity", paymentModel.getQuantity());
				Map<String, Object> items = new HashMap<>();
				items.put("0", item);
				Map<String, Object> params = new HashMap<>();
				params.put(CodeGripConstants.CUSTOMER, stripeCustomer.getId());
				params.put(CodeGripConstants.ITEMS, items);
				Subscription subscription = Subscription.create(params);

				LOGGER.info("stripe subscription" + subscription);

				/** save details in the database */
				payment = objectDao.getObjectByParam(Payment.class, CodeGripConstants.STRIPE_CUSTOMER_ID,
						stripeCustomer.getId());

				if (payment == null || payment.getCompany() == null) {
					payment = new Payment();
				}
				payment.setStripeCustomerId(stripeCustomer.getId());
				payment.setBillingDate(new Timestamp((long) subscription.getCurrentPeriodStart() * 1000));
				payment.setAmount(Double
						.parseDouble("" + ((subscription.getSubscriptionItems().getData().get(0).getPlan().getAmount()
								* subscription.getSubscriptionItems().getData().get(0).getQuantity()) / 100)));
				payment.setExpiresDate(new Timestamp((long) subscription.getCurrentPeriodEnd() * 1000));
				payment.setQuantity(subscription.getQuantity().intValue());
				payment.setStripeSubscriptionId(subscription.getId());

				Users users = objectDao.getObjectById(Users.class, paymentModel.getUserId());
				payment.setUsers(users);

				Company company = objectDao.getObjectById(Company.class, paymentModel.getCompanyId());
				payment.setCompany(company);
				Payment pay = (Payment) objectDao.saveObject(payment);
				CompanySubscription companySubscription = new CompanySubscription();
				companySubscription.setPayment(pay);

				Plans plans = objectDao.getObjectByParam(Plans.class, CodeGripConstants.STRIPE_PLAN_ID,
						paymentModel.getStripePlanId());
				if (null != plans) {
					Products product = objectDao.getObjectById(Products.class, plans.getProducts().getId());
					if (null != product) {
						companySubscription.setProducts(product);
						pay.setProduct(product);
						objectDao.saveObject(pay);
					}
					companySubscription.setStripeSubscriptionId(subscription.getId());
					companySubscription.setStartDate(new Timestamp((long) subscription.getCurrentPeriodStart() * 1000));
					companySubscription.setEndDate(new Timestamp((long) subscription.getCurrentPeriodEnd() * 1000));

					companySubscription.setCompany(company);
					companySubscription.setCreatedAt(new Timestamp(System.currentTimeMillis()));
					companySubscription.setStatus(PaymentStatusEnum.SUBSCRIPTION_ACTIVE.toString());
					companySubscription.setPlans(plans);
					objectDao.saveObject(companySubscription);

					/**
					 * userService.saveNotification(environment.getProperty(CodeGripConstants.SUBSCRIPTION_CREATE_NOTIFICATION),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_UNREAD_STATUS),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_TITLE),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_REASON),
					 * environment.getProperty(CodeGripConstants.NOTIFICATION_SUBSCRIPTION_DESTINATION_PAGE),
					 * company.getId(),users.getId(),null, CodeGripConstants.IMAGE_PAYMENT_SUCCESS);
					 */

				}

			}
		} catch (CardException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.CARD_NOT_VALID));
		} catch (RateLimitException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.TOO_MANY_REQUESTS));
		} catch (InvalidRequestException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_PARAMETERS));
		} catch (AuthenticationException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.INVALID_STRIPE_API));
		} catch (StripeException exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_GENERIC_ERROR));
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return payment;
	}

	@Override
	public Object getPurchaseDetails(Integer userId) throws CustomException {
		PurchaseDetailsCustomModel purchaseDetails = new PurchaseDetailsCustomModel();
		try {
			CompanySubscription companySubscription = objectDao.getObjectByParam(CompanySubscription.class, "userId",
					userId);

			if (!companySubscription.getProducts().getProductName().equalsIgnoreCase("STARTUP")) {
				purchaseDetails.setCurrenPlanName(companySubscription.getProducts().getProductName() + "("
						+ companySubscription.getPlans().getPlanName() + ")");
				purchaseDetails.setStartDate(formatter.format(companySubscription.getStartDate()));
				purchaseDetails.setEndDate(formatter.format(companySubscription.getEndDate()));

				LocalDateTime date = companySubscription.getEndDate().toLocalDateTime();
				LocalDate nextBillingDate = date.toLocalDate();
				Date nextDate = Date.from(nextBillingDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
				purchaseDetails.setNextBillingDate(formatter.format(nextDate));
				Map<String, Object> userCount = userService
						.userCountByCompany(companySubscription.getCompany().getId());
				Double amount = (Long) userCount.get("currentUserCount") * companySubscription.getPlans().getAmount();
				purchaseDetails.setNextPaymentAmount(amount.toString());
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
		return purchaseDetails;
	}

	@Override
	public Plans getPlanByProduct() throws CustomException {
		Plans plan = null;
		try {
			plan = objectDao.getMonthlyPlanByBusiness();

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
		return plan;
	}
}
