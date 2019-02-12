package com.mb.codegrip.service;

import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.dozer.MappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dao.ObjectDAO;
import com.mb.codegrip.dao.UserDAO;
import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.CompanySubscription;
import com.mb.codegrip.dto.InviteUsers;
import com.mb.codegrip.dto.Notification;
import com.mb.codegrip.dto.ProductFeatures;
import com.mb.codegrip.dto.Products;
import com.mb.codegrip.dto.Promotion;
import com.mb.codegrip.dto.Roles;
import com.mb.codegrip.dto.ShareDashboard;
import com.mb.codegrip.dto.UserCompany;
import com.mb.codegrip.dto.UserProjects;
import com.mb.codegrip.dto.UserRoles;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.mail.MailerHelper;
import com.mb.codegrip.model.CompanyModel;
import com.mb.codegrip.model.NotificationModel;
import com.mb.codegrip.model.ShareDashboardModel;
import com.mb.codegrip.model.ShareDashbordRequestModel;
import com.mb.codegrip.model.UserCompanyModel;
import com.mb.codegrip.model.UsersAccountDetailsModel;
import com.mb.codegrip.model.UsersModel;
import com.mb.codegrip.repository.CompanyRepository;
import com.mb.codegrip.repository.InviteUsersRepository;
import com.mb.codegrip.repository.NotificationRepository;
import com.mb.codegrip.repository.ShareDashbordRepository;
import com.mb.codegrip.repository.UserRepository;
import com.mb.codegrip.utils.CommonUtil;
import com.mb.codegrip.utils.CustomDozerHelper;
import com.mb.codegrip.utils.PasswordUtil;

@Service
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties"),
		@PropertySource("classpath:notifications.properties") })
public class UserServiceImpl implements UserService, EnvironmentAware {

	private static final Logger LOGGER = Logger.getLogger(UserServiceImpl.class);

	Mapper mapper = new DozerBeanMapper();

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private UserDAO userDAO;

	@Autowired
	private ShareDashbordRepository shareDashbordRepository;

	@Autowired
	private InviteUsersRepository inviteUsersRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@PersistenceContext
	private EntityManager entityManager;

	private static Environment environment;

	CommonUtil commonUtil = new CommonUtil();

	@Autowired
	private MailerHelper mailerHelper;

	@Autowired
	private ObjectDAO objectDao;

	public String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	/*************************************************************************************************
	 * Save github and bitbucket user.
	 *************************************************************************************************/
	@Override
	public Users saveGithubAndBitbucketUser(Users users) throws NoSuchAlgorithmException {
		LOGGER.info("------------ Save github user --------------");
		return userDAO.saveUserRecord(users);
	}

	/*************************************************************************************************
	 * Save email details.
	 *************************************************************************************************/
	@Override
	public Users saveEmailDetails(Users users) throws NoSuchAlgorithmException {
		if (users.getPassword() == null) {
			users.setPassword(PasswordUtil.encode(users.getProvider()));
		}
		return userDAO.saveUserRecord(users);
	}

	/***********************************************************************************************
	 * Send email public method.
	 ***********************************************************************************************/
	@Override
	public void sendMail(String email) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor
				.execute(() -> mailerHelper.sendMail(email, environment.getProperty(CodeGripConstants.SENDGRID_SUBJECT),
						environment.getProperty(CodeGripConstants.SENDGRID_MESSAGE)));
		emailExecutor.shutdown();
	}

	/***********************************************************************************************
	 * get notification details.
	 ***********************************************************************************************/
	@Override
	public List<NotificationModel> getNotificationList(Integer receiverId, Integer companyId, String role)
			throws CustomException {
		try {
			return CustomDozerHelper.map(mapper, userDAO.getNotificationRecord(receiverId, companyId, role),
					NotificationModel.class);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/*************************************************************************************************
	 * Calculate new notification count.
	 *************************************************************************************************/
	@Override
	public Integer calculateNewNotificationCount(List<NotificationModel> notificationModels) {
		int count = 0;
		for (NotificationModel notificationModel : notificationModels) {
			if ("UNREAD".equals(notificationModel.getStatus()))
				count++;
		}
		return count;
	}

	/***********************************************************************************************
	 * update notification status.
	 ***********************************************************************************************/
	@Override
	public Object updateNotificationStatus(Boolean isRead, Integer id, Boolean isDeleted, String role)
			throws CustomException {
		try {
			Notification notification = notificationRepository.findById(id);
			notificationRepository.updateNotificationFlag(id, (isRead ? "READ" : "UNREAD"), isDeleted);
			Map<String, Object> finalResult = new HashMap<>();
			List<NotificationModel> notificationModels = CustomDozerHelper.map(mapper,
					userDAO.getNotificationRecord(notification.getReceiverId(), notification.getCompanyId(), role),
					NotificationModel.class);
			finalResult.put("notificationList", notificationModels);
			finalResult.put("newNotificationsCount", calculateNewNotificationCount(notificationModels));
			return finalResult;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/*****************************************************************************************************
	 * Send mail to super admin when new user registration to portal
	 *************************************************************************************************/
	@Override
	public void sendRegistrationEmailToSAandAccountant(String email, String provider) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor.execute(() -> mailerHelper.sendRegistrationEmailToSAandAccountant(email,
				environment.getProperty(CodeGripConstants.SENDGRID_SUBJECT), provider));
		emailExecutor.shutdown();

	}

	/*************************************************************************************************
	 * search email and id using email and companyId.
	 *************************************************************************************************/
	@Override
	public List<Object[]> getEmailAndId(String emailId, Company company, Integer userId) throws CustomException {
		return userDAO.getEmailAndUserId(emailId, company, userId);
	}

	/*************************************************************************************************
	 * get already shared dashboard user list.
	 *************************************************************************************************/
	@Override
	public List<ShareDashboardModel> getDashboardSharedUserList(Integer projectId, Integer senderId)
			throws CustomException {
		List<ShareDashboard> shareDashboards = new ArrayList<>();
		try {
			shareDashboards = userDAO.getDashboardSharedUserList(projectId, senderId);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return CustomDozerHelper.map(mapper, shareDashboards, ShareDashboardModel.class);
	}

	/*************************************************************************************************
	 * delete users from delete list.
	 *************************************************************************************************/
	@Override
	public void deleteSharedUsers(List<ShareDashboardModel> shareDashboardModels) throws CustomException {
		try {
			for (ShareDashboardModel shareDashboardModel : shareDashboardModels) {
				shareDashbordRepository.deleteSharedDashboardUsers(shareDashboardModel.getReceiverMailId(),
						shareDashboardModel.getProjectId(), shareDashboardModel.getSenderId());
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/*************************************************************************************************
	 * Save new user registered notification details.
	 *************************************************************************************************/
	@Override
	public void saveRegisteredUserNotificationDetails(NotificationModel createFreshNotificationModel) {
		notificationRepository.save(mapper.map(createFreshNotificationModel, Notification.class));

	}

	/***************************************************************************************************
	 * User details
	 *****************************************************************************************************/
	/*********
	 * @Override public Object getUserOwnDetails(HttpServletRequest request) throws
	 *           CustomException{ Principal principal = request.getUserPrincipal();
	 *           Users users = new Users(); UsersModel userModel = new UsersModel();
	 *           users.setId(39); try { // if (principal != null) { //users =
	 *           userDAO.getByEmailAndCompanyId(principal.getName()); users =
	 *           userDAO.getUserOwnDetails(users.getId()); userModel =
	 *           mapper.map(users, UsersModel.class); LOGGER.info("Username: " +
	 *           users.getEmail()); //}
	 * 
	 * 
	 *           }catch (Exception e) { LOGGER.error(e); throw new
	 *           CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
	 *           } return userModel; }
	 */

	/***************************************************************************************************
	 * Get connected account service.
	 *****************************************************************************************************/
	@Override
	public List<UsersAccountDetailsModel> getConnectedAccounts(HttpServletRequest request, String pageNo) {
		return new ArrayList<>();
	}

	/***************************************************************************************************
	 * Update user record.
	 *****************************************************************************************************/
	@Override
	public Users saveUserDetails(List<UsersModel> userModelList) throws CustomException {
		Users users = new Users();
		try {
			for (UsersModel userModel : userModelList) {
				if (userModel.getIsDeleted()) {
					userDAO.deleteUserFromCompany(mapper.map(userModel, Users.class));
				} else if (userModel.getIsDeactivateRequest() != null && userModel.getIsDeactivateRequest()) {
					userDAO.deactivateUserFromCompany(mapper.map(userModel, Users.class));
				} else {
					userModel.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
					Optional<Users> dbUser = userRepository.findById(userModel.getId());
					if (dbUser.isPresent()) {
						userModel.setPassword(dbUser.get().getPassword());
					} else {
						throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
					}
					users = userDAO.saveUserRecord(mapper.map(userModel, Users.class));
				}

			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return users;
	}

	/***************************************************************************************************
	 * Invite Users to the company.
	 *****************************************************************************************************/
	@Override
	public void inviteUsers(ShareDashboardModel shareDashboardModel) throws CustomException {
		try {
			InviteUsers inviteUser = new InviteUsers();

			Map<String, Object> userCount = userCountByCompany(shareDashboardModel.getCompanyId());

			if (userCount.get("remainingUserCount") != null
					&& Integer.parseInt(userCount.get("remainingUserCount").toString()) < shareDashboardModel
							.getShareDashboardRequestModel().size()) {
				throw new CustomException(environment.getProperty(CodeGripConstants.USER_LIMIT_EXCEEDED));
			}

			List<InviteUsers> dbInviteUsers = inviteUsersRepository
					.findByCompanyIdAndUserId(shareDashboardModel.getCompanyId(), shareDashboardModel.getUserId());

			for (ShareDashbordRequestModel shareDashboardModels : shareDashboardModel.getShareDashboardRequestModel()) {
				inviteUser.setCompanyId(shareDashboardModel.getCompanyId());
				Calendar calendar = Calendar.getInstance();
				Timestamp currentTimestamp = new java.sql.Timestamp(calendar.getTime().getTime());
				inviteUser.setInvitedAt(currentTimestamp);
				inviteUser.setEmailId(shareDashboardModels.getReceiverMailId());
				inviteUser.setUserId(shareDashboardModel.getUserId());

				for (InviteUsers dbInviteUser : dbInviteUsers) {
					if (dbInviteUser.getCompanyId().equals(shareDashboardModel.getCompanyId())
							&& dbInviteUser.getUserId().equals(shareDashboardModel.getUserId())
							&& dbInviteUser.getEmailId().equals(shareDashboardModels.getReceiverMailId()))
						inviteUser.setId(dbInviteUser.getId());
				}

				objectDao.saveObject(inviteUser);

				Optional<Users> users = userRepository.findById(shareDashboardModel.getUserId());
				String adminName = "";
				if (users.isPresent())
					adminName = users.get().getName() != null ? users.get().getName() : users.get().getEmail();
				mailerHelper.inviteUser(shareDashboardModels.getReceiverMailId(),
						shareDashboardModel.getEncodedCompanyId(), adminName);
			}
			/**
			 * saveNotification(environment.getProperty(CodeGripConstants.NOTIFICATION_INVITE_USER_MESSAGE),
			 * environment.getProperty(CodeGripConstants.NOTIFICATION_UNREAD_STATUS),
			 * environment.getProperty(CodeGripConstants.NOTIFICATION_INVITE_USER_TITLE),
			 * environment.getProperty(CodeGripConstants.NOTIFICATION_INVITE_USER_REASON),
			 * environment.getProperty(CodeGripConstants.NOTIFICATION_INVITE_USER_DESTINATION_PAGE),
			 * shareDashboardModel.getCompanyId(), shareDashboardModel.getUserId());
			 */
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(e.getMessage());
		}

	}

	/***************************************************************************************************
	 * Save notification
	 *****************************************************************************************************/
	@Override
	public void saveNotification(String message, String status, String title, String reason, String destination,
			Integer companyId, Integer userId, Integer projectId, Integer imageId, String dashboardUrl,
			Boolean isPrivate) throws CustomException {
		try {
			Notification notification = new Notification();
			notification.setCompanyId(companyId);
			notification.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			notification.setDestinationPage(destination);
			notification.setMessage(message);
			notification.setReason(reason);
			notification.setReceiverId(userId);
			notification.setStatus(status);
			notification.setTitle(title);
			notification.setIsDeleted(false);
			notification.setProjectId(projectId);
			notification.setIsBroadcast(false);
			notification.setImageId(imageId);
			notification.setDashboardUrl(dashboardUrl);
			notification.setIsPrivate(isPrivate);
			objectDao.saveObject(notification);

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/***************************************************************************************************
	 * User list by company and user role
	 *****************************************************************************************************/
	@Override
	public List<UsersModel> userListByCompanyAndRole(Integer companyId, String pageNo, List<String> userRole)
			throws CustomException {
		List<Users> userList = null;
		List<UsersModel> usersModelList = new ArrayList<>();
		try {
			Company company = objectDao.getObjectById(Company.class, companyId);
			if (null != company) {
				userList = userDAO.getUserListByCompanyAndRole(company, pageNo, userRole);
			}

			// get company role of user.
			List<UserCompany> userCompanies = userDAO.getUserCompanyRole(company);

			usersModelList = mapCompanyRoleToUser(CustomDozerHelper.map(mapper, userList, UsersModel.class),
					CustomDozerHelper.map(mapper, userCompanies, UserCompanyModel.class), companyId);

			/**
			 * UsersModel usersModel = null; for (Users user : userList) { usersModel =
			 * mapper.map(user, UsersModel.class); usersModelList.add(usersModel);
			 * 
			 * }
			 */
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return usersModelList;
	}

	/***************************************************************************************************
	 * Function to assign user company role to user.
	 *****************************************************************************************************/
	private List<UsersModel> mapCompanyRoleToUser(List<UsersModel> map, List<UserCompanyModel> map2,
			Integer companyId) {

		// remove user other company details.
		for (UsersModel usersModel : map) {
			usersModel.getCompany().removeIf(s -> !s.getId().equals(companyId));
		}

		// assign user role of the company.
		for (UsersModel usersModel : map) {
			for (UserCompanyModel userCompanyModel : map2) {
				if (usersModel.getId().equals(userCompanyModel.getUsers().getId())
						&& userCompanyModel.getRoleName() != null) {
					usersModel.getCompany().iterator().next().setUserCompanyRole(userCompanyModel.getRoleName());
				}
			}

		}
		return map;
	}

	/***************************************************************************************************
	 * User count (Like remaining user count,recommended plan limit )service
	 *****************************************************************************************************/
	@Override
	public Map<String, Object> userCountByCompany(Integer companyId) throws CustomException {
		Map<String, Object> result = new HashMap<>();
		try {
			Company company = objectDao.getObjectById(Company.class, companyId);
			Set<Company> companySet = new HashSet<>();
			companySet.add(company);
			Object userCount1 = userDAO.userCountByCompany(companySet);
			CompanySubscription comSub = objectDao.getObjectByParam(CompanySubscription.class, "company", company);
			if (null != comSub) {
				Products pro = objectDao.getObjectById(Products.class, comSub.getProducts().getId());
				ProductFeatures proFeature = objectDao.getObjectByParam(ProductFeatures.class, "product", pro);
				Long userCount = (Long) userCount1;
				Integer remainingUserCount = proFeature.getMaxUserLimit() - userCount.intValue();
				result.put("remainingUserCount", remainingUserCount);
				result.put("currentUserCount", userCount1);
				result.put("currentPlanUserLimit", proFeature.getMaxUserLimit());
				if (proFeature.getMaxUserLimit() < 1000 && proFeature.getMaxUserLimit() > 0) {
					Products product = userDAO.getRecommendedPlan(proFeature.getMaxUserLimit());
					ProductFeatures proFea = objectDao.getObjectByParam(ProductFeatures.class, "product", product);
					result.put("recommendedPlanLimit", proFea.getMaxUserLimit());
				}

			} else {
				LOGGER.info("Comapny id " + companyId + " Has no subscription");
			}

		} catch (Exception e) {

			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return result;
	}

	/***************************************************************************************************
	 * Update company details
	 *****************************************************************************************************/
	@Override
	public CompanyModel updateCompanyDetails(CompanyModel companyModel) throws CustomException {
		Company company = new Company();
		CompanyModel dbCompanyModel = new CompanyModel();
		try {
			Optional<Users> users = userRepository.findById(companyModel.getUserId());
			companyModel.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
			companyModel.setIsOrgProfileCompleted(true);
			company = (Company) objectDao.saveObject(mapper.map(companyModel, Company.class));
			dbCompanyModel = mapper.map(company, CompanyModel.class);

			if (users.isPresent()) {
				if (dbCompanyModel.getId().equals(users.get().getOwnerCompanyId()))
					dbCompanyModel.setUserCompanyRole("ROLE_ADMIN");
				else
					dbCompanyModel.setUserCompanyRole("ROLE_USER");
			}
			String[] profilePictures = environment.getProperty(CodeGripConstants.COMPANY_AVATAR).split(",");
			if (Arrays.stream(profilePictures).anyMatch(dbCompanyModel.getCompanyLogoUrl()::equals))
				dbCompanyModel.setIsDefaultImage(true);

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return dbCompanyModel;

	}

	/***************************************************************************************************
	 * Cancel invitation of user
	 *****************************************************************************************************/

	@Override
	public void cancelUserInvitation(Integer inviteUserId) throws CustomException {
		try {
			userDAO.cancelInvitation(inviteUserId);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/***************************************************************************************************
	 * Invite user list by company
	 *****************************************************************************************************/
	@Override
	public List<InviteUsers> inviteUserListByCompany(Integer companyId, String pageNo) throws CustomException {
		List<InviteUsers> inviteUserList1 = new ArrayList<>();
		try {
			List<InviteUsers> inviteUserList = userDAO.inviteUserList(companyId, pageNo);
			InviteUsers invUserDTO = null;
			for (InviteUsers inviteUsers : inviteUserList) {
				invUserDTO = mapper.map(inviteUsers, InviteUsers.class);
				inviteUserList1.add(invUserDTO);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return inviteUserList1;
	}

	/***************************************************************************************************
	 * Add user company and role.
	 *****************************************************************************************************/
	@Override
	public UserCompany addUserCompanyAndRole(Set<Roles> roles, Set<Company> company, Users users)
			throws CustomException {
		UserCompany userCompany = new UserCompany();
		try {
			UserRoles userRoles = new UserRoles();

			// set user roles data.
			userRoles.setRoles(roles.iterator().next());
			userRoles.setUsers(users);

			// set user company data.
			userCompany.setCompany(company.iterator().next());
			userCompany.setUsers(users);
			userCompany.setRoleName(roles.iterator().next().getName());

			userCompany = userDAO.saveUserCompany(userCompany);
			userDAO.saveUserRole(userRoles);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return userCompany;

	}

	/***************************************************************************************************
	 * Check link is expired or not.
	 * 
	 * @throws CustomException
	 *****************************************************************************************************/
	@Override
	public Boolean checkLinkExpiredOrNot(String email, Integer companyId, Timestamp createdAt) throws CustomException {
		Boolean flag = false;
		try {
			InviteUsers inviteUsers = userDAO.getInvitedUserDetails(email, companyId);
			if (inviteUsers == null || inviteUsers.getId() == null)
				flag = true;
			return flag;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/***************************************************************************************************
	 * User list by project
	 *****************************************************************************************************/

	@SuppressWarnings("unchecked")
	@Override
	public List<UsersModel> getUserByProject(Integer projectId) throws CustomException {
		List<UsersModel> userList = new ArrayList<>();
		try {
			List<UserProjects> userPro = (List<UserProjects>) objectDao.listObjectByParamUserProject(UserProjects.class,
					"projectId", projectId);
			UsersModel userModel = null;
			for (UserProjects userProject : userPro) {
				userModel = new UsersModel();
				Users users = objectDao.getObjectById(Users.class, userProject.getUserId());
				userModel.setName(users.getName());
				userModel.setEmail(users.getEmail());
				userModel.setId(users.getId());
				userModel.setContactNo(users.getContactNo());
				userModel.setUserProjectId(userProject.getId());
				userList.add(userModel);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return userList;
	}

	/**
	 * *************************************************************************************************
	 * User list by project @throws CustomException @throws
	 *****************************************************************************************************/
	@Override
	public void changeRole(UsersModel userModel) throws CustomException {
		try {
			userDAO.changeRoleOfUser(mapper.map(userModel, Users.class));
			Company company = companyRepository.findById(userModel.getCompany().iterator().next().getId());
			saveNotification(
					"Your role for organization " + company.getName() + "has been changed to "
							+ userModel.getRoles().iterator().next().getName(),
					CodeGripConstants.UNREAD, "New Role", "Role changed by admin", null,
					userModel.getCompany().iterator().next().getId(), userModel.getId(), null,
					CodeGripConstants.IMAGE_NEW_USER_JOINED, null, true);

		} catch (MappingException | CustomException e) {
			LOGGER.info(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/****************************************************************************************************
	 * Logout from company.
	 *****************************************************************************************************/
	@Override
	public void logout(Integer companyId, Integer userId) throws CustomException {
		userDAO.logout(companyId, userId);

	}

	/****************************************************************************************************
	 * Get notification list and new notification count service.
	 *****************************************************************************************************/
	@Override
	public Object getNotificationListService(Integer userId, Integer companyId, String role) throws CustomException {
		Map<String, Object> finalResult = new HashMap<>();
		List<NotificationModel> notificationModels = getNotificationList(userId, companyId, role);
		finalResult.put("notificationList", notificationModels);
		finalResult.put("newNotificationsCount", calculateNewNotificationCount(notificationModels));
		return finalResult;
	}

	/****************************************************************************************************
	 * Delete profile image of user or company.
	 * 
	 * @throws CustomException
	 *****************************************************************************************************/
	@Override
	public void deleteProfileImage(Integer id, String val) throws CustomException {
		userDAO.setDefaultImage(id, val);
	}

	@Override
	public Boolean checkCoupon(UsersModel usersModel) throws CustomException {
		Boolean result = false;
		try {
			String sDate1 = environment.getProperty(CodeGripConstants.SSGLOBLE_DATE);

			if (null != usersModel.getCouponKey() && new Timestamp(System.currentTimeMillis())
					.before(new SimpleDateFormat("dd/MM/yyyy").parse(sDate1))) {
				String decodedCoupon = PasswordUtil.decodeUrl(
						environment.getProperty(CodeGripConstants.CODEGRIP_SECRET_KEY), usersModel.getCouponKey());
				String code[] = environment.getProperty(CodeGripConstants.COUPON_CODE).split(",");
				for (String codeStr : code) {
					if (codeStr.equals(decodedCoupon)) {
						Promotion promotion = null;
						promotion = objectDao.getObjectByParam(Promotion.class, "email", usersModel.getEmail());
						if (promotion == null) {
							promotion = new Promotion();
						}
						promotion.setEmail(usersModel.getEmail());
						promotion.setIsRegistered(true);
						promotion.setName(usersModel.getName());
						promotion.setCreatedAt(new Timestamp(System.currentTimeMillis()));
						objectDao.saveObject(promotion);
						return true;
					}
				}

			}
		} catch (Exception e) {
			LOGGER.error(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return result;
	}

}
