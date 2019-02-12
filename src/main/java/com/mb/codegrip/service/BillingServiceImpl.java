package com.mb.codegrip.service;

import java.sql.Timestamp;

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
import com.mb.codegrip.dto.BillingInfo;
import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.CompanySubscription;
import com.mb.codegrip.dto.ProductFeatures;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.BillingInfoModel;
import com.mb.codegrip.model.CompanyActivePlanDetailsModel;
import com.mb.codegrip.model.CompanySubscriptionModel;
import com.mb.codegrip.model.ProductFeaturesModel;
import com.mb.codegrip.utils.CommonUtil;

@Service
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:exception.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties"),
		@PropertySource("classpath:notifications.properties") })
public class BillingServiceImpl implements BillingService, EnvironmentAware {

	private static final Logger LOGGER = Logger.getLogger(PaymentServiceImpl.class);

	Mapper mapper = new DozerBeanMapper();

	private static Environment environment;

	@Autowired
	private SourceControlServiceImpl sourceControlServiceImpl;

	@Autowired
	private UserService userService;

	CommonUtil commonUtil = new CommonUtil();

	public static String getProperty(String key) {
		return environment.getProperty(key);
	}

	@SuppressWarnings("static-access")
	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	@Autowired
	private ObjectDAO objectDAO;

	/***********************************************************************************************************
	 * save billing information
	 ***********************************************************************************************************/
	@Override
	public void saveBillingInfo(BillingInfoModel billingInfoModel) throws CustomException {
		try {
			BillingInfo billingInfo = mapper.map(billingInfoModel, BillingInfo.class);
			Company company = objectDAO.getObjectById(Company.class, billingInfoModel.getCompanyId());
			billingInfo.setCompany(company);
			Users user = objectDAO.getObjectById(Users.class, billingInfoModel.getUserId());
			billingInfo.setUser(user);
			objectDAO.saveObject(billingInfo);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/***********************************************************************************************************
	 * save billing information
	 ***********************************************************************************************************/
	@Override
	public BillingInfoModel getBillingInfoByUser(Integer userId) throws CustomException {
		BillingInfoModel billingInfoModel = null;
		try {
			Users user = objectDAO.getObjectById(Users.class, userId);
			BillingInfo billingInfo = objectDAO.getObjectByParam(BillingInfo.class, "user", user);
			if (null != billingInfo) {
				billingInfoModel = mapper.map(billingInfo, BillingInfoModel.class);
			} else {
				LOGGER.info("No Record found" + userId);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return billingInfoModel;
	}

	/***********************************************************************************************************
	 * get subscription details by company
	 ***********************************************************************************************************/
	@Override
	public CompanySubscriptionModel getSubscriptionDetailsByCompany(Integer companyId) throws CustomException {
		CompanySubscriptionModel comSubModel = new CompanySubscriptionModel();
		try {
			Company company = objectDAO.getObjectById(Company.class, companyId);
			CompanySubscription comSub = objectDAO.getObjectByParam(CompanySubscription.class, "company", company);
			if (null != comSub) {
				comSubModel = mapper.map(comSub, CompanySubscriptionModel.class);
			}
			if (null != comSub && null != comSub.getCardLastDigit()) {
				comSubModel.setIsCardDetails(true);
			} else {
				comSubModel.setIsCardDetails(false);
			}

			ProductFeatures proFeatures = null;
			if (null != comSub && null != comSub.getProducts()) {
				proFeatures = objectDAO.getObjectByParam(ProductFeatures.class, "product", comSub.getProducts());
			}

			if (null != proFeatures) {
				ProductFeaturesModel proFea = mapper.map(proFeatures, ProductFeaturesModel.class);
				comSubModel.getProducts().setProductFeatures(proFea);
				comSubModel.getProducts().setIsPurchased(comSub.getIsPurchased());
				LOGGER.info("Current time: " + new Timestamp(System.currentTimeMillis()));
				LOGGER.info("Days remaining: " + commonUtil.getDayDifference(new Timestamp(System.currentTimeMillis()),
						comSubModel.getEndDate()));

				int day = commonUtil.getDayDifference(new Timestamp(System.currentTimeMillis()),
						comSubModel.getEndDate());
				String val = "";
				if (day == 0)
					val = "Expiring today";
				else if (day < 0)
					val = "Trial expired";
				else
					val = Integer.toString(day);

				comSubModel.getProducts().setDayRemaining(val);
			}
			CompanyActivePlanDetailsModel companyActivePlanDetailsModel = new CompanyActivePlanDetailsModel();
			companyActivePlanDetailsModel.setTotalUserDetails(userService.userCountByCompany(companyId));
			comSubModel.setCompanyActivePlanDetailsModel(
					sourceControlServiceImpl.filterUserSubscriptionData(comSubModel, companyActivePlanDetailsModel));
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return comSubModel;
	}

	/***************************************************************************************************************
	 * Common method to check plan is expired or not.
	 ***************************************************************************************************************/
	@Override
	public Boolean checkIsPlanExpiredOrNot(Integer companyId) {
		Boolean flag = false;
		Company company = objectDAO.getObjectById(Company.class, companyId);
		CompanySubscription comSub = objectDAO.getObjectByParam(CompanySubscription.class, "company", company);
		if (comSub.getId() != null) {
			int day = commonUtil.getDayDifference(new Timestamp(System.currentTimeMillis()), comSub.getEndDate());
			if (day < 0)
				flag = true;
		}
		return flag;
	}

}
