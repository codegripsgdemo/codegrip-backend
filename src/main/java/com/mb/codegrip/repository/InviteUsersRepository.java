package com.mb.codegrip.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.InviteUsers;

@Repository
public interface InviteUsersRepository extends JpaRepository<InviteUsers, Integer>{
	
	List<InviteUsers> findByCompanyIdAndUserId(Integer companyId, Integer userId);

	InviteUsers findByCompanyIdAndEmailId(Integer id, String email);

}
