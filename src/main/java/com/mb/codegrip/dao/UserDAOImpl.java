package com.mb.codegrip.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.InviteUsers;
import com.mb.codegrip.dto.Notification;
import com.mb.codegrip.dto.Products;
import com.mb.codegrip.dto.ShareDashboard;
import com.mb.codegrip.dto.UserCompany;
import com.mb.codegrip.dto.UserRoles;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.dto.UsersAccountDetails;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.NotificationModel;
import com.mb.codegrip.utils.CommonUtil;
import com.mb.codegrip.utils.CustomDozerHelper;

@Transactional
@Repository("UserDAO")
public class UserDAOImpl implements UserDAO {

	private static final Logger LOGGER = Logger.getLogger(UserDAOImpl.class);

	@PersistenceContext
	private EntityManager entityManager;

	CommonUtil commonUtil = new CommonUtil();

	Mapper mapper = new DozerBeanMapper();

	@Autowired
	private Environment environment;

	/************************************************************************************************
	 * get email and company id.
	 ************************************************************************************************/
	@Override
	public Users getByEmailAndCompanyId(String email) {
		LOGGER.info("<----- In getByEmailAndCompanyId ------->");
		LOGGER.info("Email: " + email);
		String hql = "FROM Users as u WHERE u.email = :email ";
		Users users = new Users();
		try {
			users = entityManager.createQuery(hql, Users.class).setParameter("email", email).getSingleResult();
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return users;
	}

	/************************************************************************************************
	 * Save or update user record.
	 ************************************************************************************************/
	@Override
	public Users saveUserRecord(Users user) {
		return entityManager.merge(user);
	}

	/**************************************************************************************************
	 * get admin list by companyId.
	 *************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<Users> getAdminList(String key) throws CustomException {

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(
				" From user where isDeleted=false and companyId in (select companyId from Projects where isEmailNotified=true and id in  ");
		stringBuilder.append(
				" ( select projectId from branch where branchId in (select branchID from quality where projectKey=:key group by projectKey))) ");
		try {
			return entityManager.createQuery(stringBuilder.toString()).setParameter("key", key).getResultList();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));

		}

	}

	/**********************************************************************************************
	 * get notification list of logged in user.
	 **********************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<NotificationModel> getNotificationRecord(Integer receiverId, Integer companyId, String role) {
		LOGGER.info("<----- In getNotificationRecord ------->");
		String hql = "FROM Notification n WHERE n.companyId = :companyId";
		if ("ROLE_USER".equals(role)) {
			hql = hql + " and n.receiverId = :receiverId  ";
		} else {
			hql = hql + " and isDeleted=false and isPrivate=false order by n.status desc, n.createdAt desc";
		}
		List<Notification> notifications = new ArrayList<>();
		try {
			Query query = entityManager.createQuery(hql, Notification.class);
			if (role == null || "ROLE_USER".equals(role))
				query.setParameter("receiverId", receiverId);
			query.setParameter(CodeGripConstants.COMPANY_ID, companyId);
			notifications = query.getResultList();
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return CustomDozerHelper.map(mapper, notifications, NotificationModel.class);
	}

	/************************************************************************************************
	 * search email and id.
	 ************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<Object[]> getEmailAndUserId(String emailId, Company company, int userId) throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(
					"select u.email, u.id FROM Users u WHERE (u.email like concat(:email,'%') and u.id in (select users from UserCompany where company=:companyId)) and u.id NOT IN (:id)");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter("email", emailId);
			query.setParameter(CodeGripConstants.COMPANY_ID, company);
			query.setParameter("id", userId);
			return query.getResultList();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/************************************************************************************************
	 * Save or update user account record.
	 ************************************************************************************************/
	@Override
	public UsersAccountDetails saveUserAccountRecord(UsersAccountDetails usersAccountDetails) {
		return entityManager.merge(usersAccountDetails);
	}

	/************************************************************************************************
	 * Get user list whom dashboard is shared already.
	 * 
	 * @throws CustomException
	 ************************************************************************************************/
	@Override
	public List<ShareDashboard> getDashboardSharedUserList(Integer projectId, Integer senderId) throws CustomException {
		LOGGER.info("<----- In getDashboardSharedUserList ------->");
		String hql = "FROM ShareDashboard as s WHERE s.projectId=:projectId and s.senderId=:senderId and isDeleted=false group by s.receiverMailId ";
		List<ShareDashboard> shareDashboards = new ArrayList<>();
		try {
			shareDashboards = entityManager.createQuery(hql, ShareDashboard.class).setParameter("projectId", projectId)
					.setParameter("senderId", senderId).getResultList();
		} catch (Exception e) {
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return shareDashboards;
	}

	/*****************************************************************************************************************
	 * get user list by company and user role.
	 *****************************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<Users> getUserListByCompanyAndRole(Company company, String pageNo, List<String> userRole)
			throws CustomException {
		Query query = entityManager.createQuery(
				"FROM Users u WHERE u.isDeleted=false AND u IN(SELECT uc.users FROM UserCompany uc WHERE  uc.company =:company)"
						+ " AND u IN (SELECT ur.users FROM UserRoles ur JOIN ur.roles r WHERE r.name IN :userRole ) ORDER BY u.id DESC");
		query.setParameter("userRole", userRole);
		query.setParameter("company", company);
		if (!pageNo.equalsIgnoreCase(CodeGripConstants.ALL)) {
			query.setFirstResult(((Integer.parseInt(pageNo) - 1) * CodeGripConstants.TWENTY));
			query.setMaxResults(CodeGripConstants.TWENTY);
		}
		return (List<Users>) query.getResultList();
	}

	/*****************************************************************************************************************
	 * get user count by company
	 * 
	 * @throws CustomException
	 *****************************************************************************************************************/
	@Override
	public Object userCountByCompany(Set<Company> company) throws CustomException {
		try {
			Query query = entityManager.createQuery(
					"SELECT COUNT(u.id) FROM Users u WHERE u.id in (select users from UserCompany where company=:companyId) and u.isDeleted = false ");
			query.setParameter(CodeGripConstants.COMPANY_ID, company.iterator().next());
			return query.getSingleResult();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/*********************************************************************************************
	 * get recommended plan
	 *********************************************************************************************/
	@Override
	public Products getRecommendedPlan(Integer maxUserLimit) {
		try {
			Query query = entityManager.createQuery(
					"FROM Products p WHERE p IN(SELECT pf.product FROM ProductFeatures pf WHERE pf.maxUserLimit > :maxUserLimit ORDER BY pf.maxUserLimit ASC ) ");
			query.setParameter("maxUserLimit", maxUserLimit).setMaxResults(1);
			return (Products) query.getSingleResult();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return null;
		}

	}

	/*********************************************************************************************
	 * Cancel invitation.
	 *********************************************************************************************/
	@Override
	public void cancelInvitation(Integer inviteUserId) throws CustomException {
		try {
			Query query = entityManager.createQuery("DELETE FROM InviteUsers i WHERE i.id =:inviteUserId");
			query.setParameter("inviteUserId", inviteUserId).executeUpdate();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

	}

	/*********************************************************************************************
	 * get invite user list.
	 *********************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<InviteUsers> inviteUserList(Integer companyId, String pageNo) throws CustomException {
		Query query = entityManager.createQuery("FROM InviteUsers u WHERE u.companyId =:companyDetails");
		try {
			query.setParameter("companyDetails", companyId);
			if (!pageNo.equalsIgnoreCase(CodeGripConstants.ALL)) {
				query.setFirstResult(((Integer.parseInt(pageNo) - 1) * CodeGripConstants.TWENTY));
				query.setMaxResults(CodeGripConstants.TWENTY);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return (List<InviteUsers>) query.getResultList();
	}

	/************************************************************************************************
	 * Save or update user role record.
	 ************************************************************************************************/
	@Override
	public UserRoles saveUserRole(UserRoles user) {
		return entityManager.merge(user);
	}

	/************************************************************************************************
	 * Save or update user company record.
	 ************************************************************************************************/
	@Override
	public UserCompany saveUserCompany(UserCompany userCompany) {
		return entityManager.merge(userCompany);
	}

	/*********************************************************************************************
	 * Get invited user details.
	 *********************************************************************************************/
	@Override
	public InviteUsers getInvitedUserDetails(String email, Integer companyId) {
		Query query = entityManager
				.createQuery("FROM InviteUsers u WHERE u.companyId =:companyId and emailId=:emailId");
		try {
			query.setParameter(CodeGripConstants.COMPANY_ID, companyId);
			query.setParameter("emailId", email);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return (InviteUsers) query.getSingleResult();
	}

	/*********************************************************************************************
	 * Change role of user.
	 * 
	 * @throws CustomException
	 *********************************************************************************************/
	@Override
	public void changeRoleOfUser(Users users) throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(
					" UPDATE FROM UserCompany as uc set uc.roleName=:roleName WHERE uc.users=:userId and uc.company=:companyId ");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter(CodeGripConstants.USER_ID, users);
			query.setParameter("roleName", users.getRoles().iterator().next().getName());
			query.setParameter(CodeGripConstants.COMPANY_ID, users.getCompany().iterator().next());
			query.executeUpdate();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/*********************************************************************************************
	 * Change role of user.
	 * 
	 * @throws CustomException
	 *********************************************************************************************/
	@Override
	public void logout(Integer companyId, Integer userId) throws CustomException {
		try {
			StringBuilder hql = new StringBuilder();
			hql.append(
					" UPDATE FROM Users as u set u.lastLogoutCompanyId=:companyId, updatedAt=now() WHERE u.id=:userId ");
			Query query = entityManager.createQuery(hql.toString());
			query.setParameter(CodeGripConstants.USER_ID, userId);
			query.setParameter(CodeGripConstants.COMPANY_ID, companyId);
			query.executeUpdate();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/*********************************************************************************************
	 * Change role of user.
	 *********************************************************************************************/
	@Override
	public void deleteUserFromCompany(Users userModel) throws CustomException {
		try {
			Query query = entityManager.createQuery(
					"DELETE FROM UserCompany uc WHERE uc.users =:userId and uc.company=:companyId and uc.roleName=:roleName");
			query.setParameter(CodeGripConstants.USER_ID, userModel).setParameter(CodeGripConstants.COMPANY_ID, userModel.getCompany().iterator().next())
					.setParameter("roleName", userModel.getRoles().iterator().next().getName()).executeUpdate();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/*********************************************************************************************
	 * Set default image.
	 *********************************************************************************************/
	@Override
	public void setDefaultImage(Integer id, String val) throws CustomException {
		try {
			if ("USER".equalsIgnoreCase(val)) {
				entityManager.createQuery("UPDATE Users u set u.profilePictureUrl = :image WHERE u.id =:id")
						.setParameter("image", commonUtil.getRandomProfilePictureURL("USER")).setParameter("id", id)
						.executeUpdate();
			} else {
				entityManager.createQuery("UPDATE Company c set c.companyLogoUrl = :image WHERE c.id =:id")
						.setParameter("image", commonUtil.getRandomProfilePictureURL("COMPANY")).setParameter("id", id)
						.executeUpdate();
			}
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	
	/*********************************************************************************************
	 * Get users company role.
	 *********************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public List<UserCompany> getUserCompanyRole(Company company) {
		Query query = entityManager
				.createQuery("FROM UserCompany u WHERE u.company =:companyId");
		try {
			query.setParameter(CodeGripConstants.COMPANY_ID, company);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return query.getResultList();
	}

	/*********************************************************************************************
	 * Deactivate user for specific company.
	 *********************************************************************************************/
	@Override
	public void deactivateUserFromCompany(Users userModel) throws CustomException {
		try {
			Boolean flag = false;
			if(userModel.getDeactivated())
				flag=true;
			Query query = entityManager.createQuery(
					"UPDATE UserCompany uc SET deactivated=:flag WHERE uc.users =:userId and uc.company=:companyId");
			query.setParameter(CodeGripConstants.USER_ID, userModel).setParameter(CodeGripConstants.COMPANY_ID, userModel.getCompany().iterator().next())
			.setParameter("flag", flag).executeUpdate();
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/********
	 * @Override public Users getUserOwnDetails(Integer id) throws CustomException {
	 *           String hql = "FROM Users u WHERE u.id =:id "; Users users = new
	 *           Users(); try { users =
	 *           entityManager.createQuery(hql,Users.class).setParameter("id",
	 *           id).getSingleResult(); } catch (Exception e) { LOGGER.error(e); }
	 *           return users; }
	 */

}
