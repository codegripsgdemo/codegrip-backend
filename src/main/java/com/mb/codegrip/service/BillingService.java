package com.mb.codegrip.service;

import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.BillingInfoModel;
import com.mb.codegrip.model.CompanySubscriptionModel;

public interface BillingService {

	public void saveBillingInfo(BillingInfoModel billingInfoModel) throws CustomException;

	public BillingInfoModel getBillingInfoByUser(Integer userId) throws CustomException;

	public CompanySubscriptionModel getSubscriptionDetailsByCompany(Integer companyId) throws CustomException;

	Boolean checkIsPlanExpiredOrNot(Integer companyId);

}
