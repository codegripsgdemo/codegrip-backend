package com.mb.codegrip.service;

import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.InviteUsers;
import com.mb.codegrip.dto.Roles;
import com.mb.codegrip.dto.UserCompany;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CompanyModel;
import com.mb.codegrip.model.NotificationModel;
import com.mb.codegrip.model.ShareDashboardModel;
import com.mb.codegrip.model.UsersAccountDetailsModel;
import com.mb.codegrip.model.UsersModel;

public interface UserService {

	public Users saveEmailDetails(Users users) throws NoSuchAlgorithmException;

	void sendMail(String email);

	Users saveGithubAndBitbucketUser(Users users) throws NoSuchAlgorithmException;

	public List<NotificationModel> getNotificationList(Integer userId, Integer companyId, String role) throws CustomException;

	public void sendRegistrationEmailToSAandAccountant(String email, String provider);

	List<Object[]> getEmailAndId(String emailId, Company company, Integer userId) throws CustomException;

	List<ShareDashboardModel> getDashboardSharedUserList(Integer projectId, Integer senderId) throws CustomException;

	public void deleteSharedUsers(List<ShareDashboardModel> shareDashboardModels) throws CustomException;

	public void saveRegisteredUserNotificationDetails(NotificationModel createFreshNotificationModel);

	public List<UsersAccountDetailsModel> getConnectedAccounts(HttpServletRequest request, String pageNo);

	public Users saveUserDetails(List<UsersModel> userModel) throws CustomException;
	public void inviteUsers(ShareDashboardModel shareDashboardModel)throws CustomException;
	
	public List<UsersModel> userListByCompanyAndRole(Integer companyId,String pageNo,List<String> userRole)throws CustomException;
	public Map<String, Object> userCountByCompany(Integer companyId)throws CustomException;
	public CompanyModel updateCompanyDetails(CompanyModel companyModel) throws CustomException;
	public void cancelUserInvitation(Integer inviteUserId)throws CustomException;
	public List<InviteUsers> inviteUserListByCompany(Integer companyId, String pageNo) throws CustomException;
	public UserCompany addUserCompanyAndRole(Set<Roles> roles, Set<Company> company, Users users) throws CustomException;

	public Boolean checkLinkExpiredOrNot(String email, Integer id, Timestamp timestamp) throws CustomException;

	Object updateNotificationStatus(Boolean isRead, Integer id, Boolean isDeleted, String role)
			throws CustomException;
	public List<UsersModel> getUserByProject(Integer projectId) throws CustomException;

	void saveNotification(String message, String status, String title, String reason, String destination,
			Integer companyId, Integer userId, Integer projectId, Integer imageId, String dashboardUrl, Boolean isPrivate) throws CustomException;

	public void changeRole(UsersModel userModel) throws CustomException;

	public void logout(Integer companyId, Integer userId) throws CustomException;

	public Integer calculateNewNotificationCount(List<NotificationModel> notificationModels);

	public Object getNotificationListService(Integer userId, Integer companyId, String role) throws CustomException;

	public void deleteProfileImage(Integer id, String val) throws CustomException;

	public Boolean checkCoupon(UsersModel usersModel)throws CustomException;

}
