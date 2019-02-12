package com.mb.codegrip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.Company;

@Repository
public  interface CompanyRepository extends JpaRepository<Company, Integer>{
	
	Company findById(Integer id);
}
