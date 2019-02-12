package com.mb.codegrip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.CustomPlan;

@Repository
public interface CustomPlanRepository extends JpaRepository<CustomPlan, Integer>{

}
