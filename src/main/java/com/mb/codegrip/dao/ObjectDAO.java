package com.mb.codegrip.dao;

import java.io.Serializable;
import java.util.List;

import com.mb.codegrip.dto.CompanySubscription;
import com.mb.codegrip.dto.Plans;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CompanySubscriptionModel;

public interface ObjectDAO {

		
	public <T> T getObjectByParam(Class<T> entity,String param,Object obj);
	
	public <T> T getObjectById(Class<T> entity,Serializable id);
	
	public Object saveObject(Object entity);
	
	public <T> T listObject(Class<T> entity, String id);
	
	public <T> T listObjectByParam(Class<T> entity, String param, Object obj);

	public List<CompanySubscriptionModel> getExpiringCompanies() throws CustomException;

	public List<CompanySubscription> checkSubscription()throws CustomException;
	public <T> T listObjectByParamUserProject(Class<T> entity, String param, Object obj);

	public Plans getMonthlyPlanByBusiness();
	
	public void deleteObject(Object entity);
}
