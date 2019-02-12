package com.mb.codegrip.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mb.codegrip.dto.CompanySubscription;

public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Integer>{

	CompanySubscription findByCompanyIdAndIsDeleted(Integer id, boolean b);
	
	

}
