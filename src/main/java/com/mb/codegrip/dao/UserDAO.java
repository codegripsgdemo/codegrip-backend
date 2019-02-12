package com.mb.codegrip.dao;

import java.util.List;
import java.util.Set;

import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.InviteUsers;
import com.mb.codegrip.dto.Products;
import com.mb.codegrip.dto.ShareDashboard;
import com.mb.codegrip.dto.UserCompany;
import com.mb.codegrip.dto.UserRoles;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.dto.UsersAccountDetails;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.NotificationModel;

public interface UserDAO {

	public Users getByEmailAndCompanyId(String name);
	
	public Users saveUserRecord(Users user);

	public List<Users> getAdminList(String key) throws CustomException;

	public List<NotificationModel> getNotificationRecord(Integer recieverId, Integer companyId, String role);

	List<Object[]> getEmailAndUserId(String emailId, Company company, int userId) throws CustomException;

	UsersAccountDetails saveUserAccountRecord(UsersAccountDetails usersAccountDetails);

	public List<ShareDashboard> getDashboardSharedUserList(Integer projectId, Integer senderId) throws CustomException;

	public List<Users> getUserListByCompanyAndRole(Company company,String pageNo, List<String> userRole)throws CustomException;

	public Products getRecommendedPlan(Integer maxUserLimit);

	public void cancelInvitation(Integer inviteUserId) throws CustomException;

	public List<InviteUsers> inviteUserList(Integer companyId, String pageNo)throws CustomException;

	public UserCompany saveUserCompany(UserCompany userCompany);

	public UserRoles saveUserRole(UserRoles userRoles);

	Object userCountByCompany(Set<Company> companySet) throws CustomException;

	public InviteUsers getInvitedUserDetails(String email, Integer companyId);

	public void changeRoleOfUser(Users users) throws CustomException;

	public void logout(Integer companyId, Integer userId) throws CustomException;

	public void deleteUserFromCompany(Users users) throws CustomException;

	public void setDefaultImage(Integer id, String val) throws CustomException;

	public List<UserCompany> getUserCompanyRole(Company company);

	public void deactivateUserFromCompany(Users map) throws CustomException;
	
	/****
	 * 	public Users getUserOwnDetails(Integer id)throws CustomException;
	 */


}
