package com.mb.codegrip.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
import com.mb.codegrip.dto.ProjectBranch;
import com.mb.codegrip.dto.Projects;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CommonCommitModel;

@Transactional
@Repository("ProjectDAO")
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:notifications.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class ProjectDAOImpl implements ProjectDAO {

	private static final Logger LOGGER = Logger.getLogger(ProjectDAOImpl.class);

	@Autowired
	private Environment environment;

	@PersistenceContext
	private EntityManager entityManager;

	/*********************************************************************************************************************
	 * Get branch key name API.
	 *********************************************************************************************************************/
	@Override
	public ProjectBranch getBranchDetails(String name, String branchKey) throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(" FROM ProjectBranch as pb WHERE pb.name = :name and pb.branchKey=:key ");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter("name", name);
			query.setParameter("key", branchKey);
			return (ProjectBranch) query.getSingleResult();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/*****************************************************************************************************************
	 * Save or update project record.
	 * 
	 * @return
	 *****************************************************************************************************************/
	@Override
	public Projects saveProjectsRecord(Projects projects) {
		return entityManager.merge(projects);
	}

	/*********************************************************************************************
	 * Save or update quality condition record.
	 *********************************************************************************************/
	@Override
	public void saveQualityConditionRecord(List<ProjectBranch> projectBranchs) {
		entityManager.merge(projectBranchs);
	}

	/*********************************************************************************************
	 * Get started project list by uid.
	 *********************************************************************************************/
	@Override
	public Projects getStartedProjectByUidAndUsername(CommonCommitModel githubCommitModel) throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(" FROM Projects as p WHERE p.uid = :uid and p.isAnalyzeStarted=true and p.userName = :userName and isDeleted=false");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter("uid", githubCommitModel.getUid());
			query.setParameter("userName", githubCommitModel.getOwnerLoginId());
			return (Projects) query.getSingleResult();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/*********************************************************************************************
	 * Get project branch details by project id and branch key .
	 *********************************************************************************************/
	@Override
	public ProjectBranch getProjectBranchByBranchKeyAndProjectId(Projects dbProjects,
			CommonCommitModel githubCommitModel) {
		StringBuilder hql = new StringBuilder();
		hql.append(" FROM ProjectBranch as pb WHERE pb.branchKey = :branchKey and pb.projects = :projectId");
		Query query = entityManager.createQuery(hql.toString());
		query.setParameter("branchKey", githubCommitModel.getBranchName());
		query.setParameter("projectId", dbProjects);
		ProjectBranch projectBranch = new ProjectBranch();
		try {
			projectBranch = (ProjectBranch) query.getSingleResult();
		} catch (NoResultException e) {
			LOGGER.error(e.getMessage());
		}
		if (projectBranch.getId() != null)
			return projectBranch;
		else
			return null;
	}

	/*********************************************************************************************
	 * Get started project list of admin.
	 *********************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<Projects> getAdminProjectList(List<Integer> uniqueUserIds, Integer companyId) throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(
					" FROM Projects as p WHERE p.userId in (:userIds) and p.isAnalyzeStarted=true and p.companyId = :companyId");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter("userIds", uniqueUserIds);
			query.setParameter("companyId", companyId);
			return query.getResultList();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/*********************************************************************************************
	 * Delete project.
	 * 
	 * @throws CustomException
	 *********************************************************************************************/
	@Override
	public void deleteProject(Integer projectId) throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(" UPDATE FROM Projects as p set p.isDeleted=true,  WHERE p.id=:id ");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter("id", projectId);
			query.executeUpdate();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/*********************************************************************************************
	 * Update project status.
	 *********************************************************************************************/
	@Override
	public void updateProjectErrorMessage(Integer projectId, String errorMessage, String status)
			throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			if (errorMessage.length() > 98)
				errorMessage = errorMessage.substring(0, 98);
			hql.append(
					" UPDATE Projects as p set p.analyzingStatus= :analyzing_status,p.statusDetail= :status_detail WHERE p.id= :id ");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter("id", projectId);
			query.setParameter("status_detail", errorMessage);
			query.setParameter("analyzing_status", status);
			query.executeUpdate();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/*********************************************************************************************
	 * Update project branch status.
	 *********************************************************************************************/
	@Override
	public void updateBranchErrorMessage(Integer projectBranchId, String errorMessage, String staus)
			throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			if (errorMessage.length() > 98)
				errorMessage = errorMessage.substring(0, 98);
			hql.append(
					" UPDATE FROM ProjectBranch as p set p.analyzingStatus= :analyzing_status,p.statusDetail= :status_detail WHERE p.id=:id ");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter("id", projectBranchId);
			query.setParameter("status_detail", errorMessage);
			query.setParameter("analyzing_status", staus);
			query.executeUpdate();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

}
