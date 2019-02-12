package com.mb.codegrip.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.UserCompany;
import com.mb.codegrip.dto.Users;


@Repository
public interface UserCompanyRepository extends JpaRepository<UserCompany, Integer>{

	UserCompany findByUsersAndCompanyAndDeactivated(Users saveUsers, Company company, boolean b);

	List<UserCompany> findByUsersAndDeactivated(Users saveUsers, boolean b);

}
