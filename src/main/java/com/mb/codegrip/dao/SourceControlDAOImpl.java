package com.mb.codegrip.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dto.Projects;
import com.mb.codegrip.exception.CustomException;

@Transactional
@Repository("SourceControlDAO")
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:notifications.properties"), @PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class SourceControlDAOImpl implements SourceControlDAO {
	
	@Autowired
	private Environment environment;

	private static final Logger LOGGER = Logger.getLogger(SourceControlDAOImpl.class);

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public String getRefreshTokenFromDB(Integer id) {
		return null;
	}

	
	/**********************************************************************************************************************
	 * Get started project list.
	 **********************************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<Projects> getStartedProjectList(Integer companyId, String pageNo, List<Integer> ids) throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(" FROM Projects as p WHERE p.companyId = :companyId and p.isAnalyzeStarted=true and p.isDeleted=false ");
			
			if(ids!=null) {
				hql.append(" and id in (:ids)");
			}
			hql.append(" order by p.analyzeStartTime desc ");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter(CodeGripConstants.COMPANY_ID, companyId);
			
			if(ids!=null) {
				query.setParameter("ids", ids);	
			}
			
			if (!pageNo.equalsIgnoreCase(CodeGripConstants.ALL)) {
				query.setFirstResult(((Integer.parseInt(pageNo) - 1) * CodeGripConstants.TWENTY));
				query.setMaxResults(CodeGripConstants.TWENTY);
			}
			return (List<Projects>)query.getResultList();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/**********************************************************************************************************************
	 * Get single project by companyId and userId.
	 **********************************************************************************************************************/
	@Override
	public Projects getSingleStartedProjectDetailsByCompanyIdAndUserId(Integer companyId, Integer userId) throws CustomException {
			String hql = "FROM Projects as p WHERE p.companyId = :companyId and p.userId=:userId and p.isAnalyzeStarted=true ";
			Query query = entityManager.createQuery(hql);
			query.setParameter(CodeGripConstants.COMPANY_ID, companyId);
			query.setParameter("userId", userId);
			Projects projects = new Projects();
			try {
				projects = (Projects) query.setMaxResults(1).getSingleResult();
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
			if (projects.getId() != null)
				return projects;
			else
				return null;
	}


	/**********************************************************************************************************************
	 * Get project list of user.
	 **********************************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<Projects> getStartedProjectListOfUser(Integer userId, String pageNo, Integer companyId, List<Integer> ids) throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(" FROM Projects as p WHERE p.isAnalyzeStarted=true and p.companyId = :companyId and p.isDeleted=false and p.id in (select projectId from UserProjects where userId = :userId) ");
			
			if(ids!=null) {
				hql.append(" and id in (:ids)");
			}
			hql.append(" order by p.analyzeStartTime desc ");
			
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter("userId", userId);
			query.setParameter(CodeGripConstants.COMPANY_ID, companyId);
			if(ids!=null) {
				query.setParameter("ids", ids);	
			}
			
			if (!pageNo.equalsIgnoreCase(CodeGripConstants.ALL)) {
				query.setFirstResult(((Integer.parseInt(pageNo) - 1) * CodeGripConstants.TWENTY));
				query.setMaxResults(CodeGripConstants.TWENTY);
			}
			return (List<Projects>)query.getResultList();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}


	/**********************************************************************************************************************
	 * Hard delete 
	 **********************************************************************************************************************/
	/**@Override
	public void deleteQualityCondition(Integer userId) {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(" FROM Projects as p WHERE p.isAnalyzeStarted=true and p.companyId = :companyId and p.isDeleted=false and p.id in (select projectId from UserProjects where userId = :userId)");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter("userId", userId);
			query.setParameter(CodeGripConstants.COMPANY_ID, companyId);
			if (!pageNo.equalsIgnoreCase(CodeGripConstants.ALL)) {
				query.setFirstResult(((Integer.parseInt(pageNo) - 1) * CodeGripConstants.TWENTY));
				query.setMaxResults(CodeGripConstants.TWENTY);
			}
			return (List<Projects>)query.getResultList();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		
	}	*/
}
