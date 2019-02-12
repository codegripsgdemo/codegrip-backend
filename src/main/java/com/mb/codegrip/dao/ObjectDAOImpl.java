package com.mb.codegrip.dao;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dto.CompanySubscription;
import com.mb.codegrip.dto.Plans;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CompanySubscriptionModel;
import com.mb.codegrip.utils.CommonUtil;
import com.mb.codegrip.utils.CustomDozerHelper;

@Transactional
@Repository("ObjectDAO")
@PropertySources(value = { @PropertySource("classpath:messages.properties"),@PropertySource("classpath:exception.properties"),
		@PropertySource("classpath:messages.properties"), @PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class ObjectDAOImpl  implements ObjectDAO{

	private static final Logger LOGGER = Logger.getLogger(ObjectDAOImpl.class);
	
	private SessionFactory sessionFactory;
	
	Mapper mapper = new DozerBeanMapper();
	
	private static CommonUtil commonUtil = new CommonUtil();

	
	@Autowired
	private EntityManager entityManager;
	  
	protected Session getSession() {
		return sessionFactory.getCurrentSession();
	}

	public void persist(Object entity) {
		getSession().persist(entity);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getObjectByParam(Class<T> entity, String param, Object paramValue) {
		Query q = entityManager.createQuery("SELECT e FROM "+ entity.getName() + " e WHERE "+param+" = :value ORDER BY e.id DESC");
			q.setParameter("value", paramValue);
			q.setMaxResults(1);
			try {
				return (T) q.getSingleResult();
			} catch (NoResultException exc) {
				return null;
			}
	}

	
	@Override
	public <T> T getObjectById(Class<T> entity, Serializable id) {
		return entityManager.getReference(entity, id);
	}


	@Override
	public Object saveObject(Object entity)  {
		return entityManager.merge(entity);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T listObject(Class<T> entity, String id) {
		Query q = entityManager.createQuery(
				"SELECT e FROM " + entity.getName() + " e ");
		return (T) q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T listObjectByParam(Class<T> entity, String param, Object obj) {
		Query q = entityManager.createQuery(
				"SELECT e FROM " + entity.getName() + " e WHERE "+param+" = :value ORDER BY e.id DESC");
		q.setParameter("value", obj);
		return (T) q.getResultList();
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T listObjectByParamUserProject(Class<T> entity, String param, Object obj) {
		Query q = entityManager.createQuery(
				"SELECT e FROM " + entity.getName() + " e WHERE "+param+" = :value and isDeleted=false ORDER BY e.id DESC");
		q.setParameter("value", obj);
		return (T) q.getResultList();
	}

	
	
	/*********************************************************************************************************
	 * Get company subscription expiring data.
	 * @throws CustomException 
	 ********************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<CompanySubscriptionModel> getExpiringCompanies() throws CustomException {
		try {
			Timestamp expiryDate = commonUtil.getExpiryDateByTimeInTimestamp(CodeGripConstants.EXPIRING_DAY, new Timestamp(System.currentTimeMillis()));
			String hql = "SELECT cs FROM CompanySubscription cs WHERE cs.endDate BETWEEN now() and :expiryDate ";
			Query query = entityManager.createQuery(hql);
			query.setParameter("expiryDate", expiryDate);
			List<CompanySubscription> companySubscriptions = query.getResultList();
			return CustomDozerHelper.map(mapper, companySubscriptions, CompanySubscriptionModel.class);
		} catch (Exception e) {
			LOGGER.error("Something went wrong.");
			throw new CustomException("Something went wrong.");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CompanySubscription> checkSubscription() throws CustomException {
		try {
			Timestamp expiryDate = new Timestamp(System.currentTimeMillis());
			String hql = "SELECT cs FROM CompanySubscription cs WHERE cs.endDate BETWEEN NOW() and NOW() + INTERVAL 15 MINUTE ";
			Query query = entityManager.createQuery(hql);
			query.setParameter("expiryDate", expiryDate);
			return query.getResultList();
		} catch (Exception e) {
			LOGGER.error("Something went wrong.");
			throw new CustomException("Something went wrong.");
		}
	}

	@Override
	public Plans getMonthlyPlanByBusiness() {
		Plans plan = null;
		try {
			Query query = entityManager.createQuery("SELECT p FROM Plans p WHERE p.products IN ( SELECT pro FROM Products pro WHERE pro.productName='Business')"
					+ " AND p.planInterval='month'");
			plan = (Plans) query.getSingleResult();
		}catch(Exception e) {
			LOGGER.error("Something went wrong.");
		}
		return plan;
		
	}

	@Transactional
	@Override
	public void deleteObject(Object entity)  {
		entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
	}
}
