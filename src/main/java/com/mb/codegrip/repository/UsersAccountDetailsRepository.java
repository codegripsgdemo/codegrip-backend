package com.mb.codegrip.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.UsersAccountDetails;

@Repository
public interface UsersAccountDetailsRepository extends JpaRepository<UsersAccountDetails, Integer> {
	
	UsersAccountDetails findByAccountUsernameAndUserId(String acountUserName, int userId);
	List<UsersAccountDetails> findByUserId(Integer userId);
	UsersAccountDetails findByUserIdAndSourceControlNameAndAccountUsernameAndCompanyId(Integer id, String provider,
			String userName, Integer companyId);
	UsersAccountDetails findByUserIdAndSourceControlName(Integer id, String provider);
	List<UsersAccountDetails> findByUserIdAndCompanyId(Integer integer, Integer integer2);
	UsersAccountDetails findByUserIdAndCompanyIdAndSourceControlNameAndAccountUsername(Integer userId,
			Integer companyId, String sourceControl, String userName);
	UsersAccountDetails findByAccountUsernameAndUserIdAndSourceControlName(String accountUsername, Integer id,
			String sourceControlName);
}
