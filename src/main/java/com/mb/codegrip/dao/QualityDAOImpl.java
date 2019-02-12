package com.mb.codegrip.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mb.codegrip.dto.Quality;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CommitModel;

@Transactional
@Repository("QualityDAO")
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:notifications.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class QualityDAOImpl implements QualityDAO {
	private static final Logger LOGGER = Logger.getLogger(QualityDAOImpl.class);

	/**
	 * @Autowired private Environment environment;
	 */
	@PersistenceContext
	private EntityManager entityManager;

	/*********************************************************************************************************************
	 * Get quality details by project branch id API.
	 *********************************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<Quality> getQualityDetails(Integer branchId) throws CustomException {
		StringBuilder hql = new StringBuilder();
		hql.append(" FROM Quality as q WHERE q.projectBranchId = :branchId order by q.analyzeAt desc ");
		Query query = entityManager.createQuery(hql.toString());
		query.setParameter("branchId", branchId);
		List<Quality> quality = new ArrayList<>();
		try {
				quality = query.setMaxResults(2).getResultList();
			return quality;
		} catch (NoResultException e) {
			LOGGER.error(e.getMessage());
			return quality;
		}
	}
	
	
	
	/*********************************************************************************************************************
	 * Get quality details by project branch id API.
	 *********************************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<Quality> getQualityDetailsAsPerDays(Integer branchId, Integer days) throws CustomException {
		StringBuilder hql = new StringBuilder();
		hql.append(" FROM Quality as q WHERE q.projectBranchId = :branchId group by  DATE_FORMAT(q.analyzeAt,'%Y-%m-%d') order by id desc ");
		Query query = entityManager.createQuery(hql.toString());
		query.setParameter("branchId", branchId);
		List<Quality> quality = new ArrayList<>();
		try {
				quality = query.setMaxResults(days).getResultList();
			return quality;
		} catch (NoResultException e) {
			LOGGER.error(e.getMessage());
			return quality;
		}
	}

	

	/*********************************************************************************************************************
	 * Update commit id in quality record.
	 *********************************************************************************************************************/
	@Override
	public void updateCommitId(Quality quality, CommitModel commitModel) {
		try {
			Query query = entityManager.createQuery("UPDATE Quality q set commitId=:commitId WHERE q.id =:qualityId");
			query.setParameter("qualityId", quality.getId()).setParameter("commitId", commitModel.getCommitHash())
					.executeUpdate();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

	}
	
	
	/*****************************************************************************************************************
	 * Update quality records.
	 *****************************************************************************************************************/
	@Override
	public List<Quality> saveQualityRecord(List<Quality> qualities) {
		List<Quality> qualities2 = new ArrayList<>();
		for (Quality quality : qualities) {
			qualities2.add(entityManager.merge(quality));
		}
		return qualities2;
	}


}
