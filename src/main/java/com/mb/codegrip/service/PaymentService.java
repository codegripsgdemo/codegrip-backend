package com.mb.codegrip.service;


import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.mb.codegrip.dto.CompanySubscription;
import com.mb.codegrip.dto.Payment;
import com.mb.codegrip.dto.Plans;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.PaymentModel;
import com.mb.codegrip.model.PlansModel;
import com.mb.codegrip.model.ProductModel;
import com.mb.codegrip.model.PurchaseDetailsCustomModel;
import com.mb.codegrip.model.StripeProductModel;
import com.mb.codegrip.model.UsersModel;

public interface PaymentService {
	
	public Payment chargeSubscription(PaymentModel paymentModel) throws CustomException;
	public String createProduct(StripeProductModel stripeProductModel) throws CustomException;
	public Plans saveUpdatePlan(PlansModel plansModel) throws CustomException;
	public Payment editSubscription(PaymentModel paymentModel) throws CustomException;
	public void updateCard(PaymentModel paymentModel)throws CustomException;
	public void handleWebhook(HttpServletRequest request) throws CustomException, IOException;
	public void cancelSubscription(PaymentModel paymentModel) throws CustomException;
	public Object getPlanListByProduct();
	public Plans saveUpdateProductWithPlan(ProductModel prodctModel)throws CustomException;
	public void saveStartupPlan(CompanySubscription createStartupSubscriptionObejct) throws CustomException;
	public List<PurchaseDetailsCustomModel> getPurchaseList(Integer userId)throws CustomException;
	public void createCustomer(UsersModel usersModel)throws CustomException;
	public Payment chargeNewSubscription(PaymentModel paymentModel) throws CustomException;
	public Object getPurchaseDetails(Integer userId) throws CustomException;
	public Plans getPlanByProduct()throws CustomException;
	
}
