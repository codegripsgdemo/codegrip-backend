package com.mb.codegrip.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mb.codegrip.dto.Company;
import com.mb.codegrip.utils.CommonUtil;

@Transactional
@Repository("CompanyDAO")
public class CompanyDAOImpl implements CompanyDAO {
	private static final Logger LOGGER = Logger.getLogger(CompanyDAOImpl.class);
	
	@PersistenceContext	
	private EntityManager entityManager;
	
	CommonUtil commonUtil = new CommonUtil();
	
	/**@Autowired
	private Environment environment;*/
	
	/*****************************************************************************************************************
	 * Save or update user record.
	 *****************************************************************************************************************/
	@Override
	public Company saveCompanyRecord(Company company) {
		LOGGER.info("------- In save company record ------");
		// Set default profile picture if not set.
		company.setCompanyLogoUrl(commonUtil.getRandomProfilePictureURL("COMPANY"));
		return entityManager.merge(company);
	}
	
}
