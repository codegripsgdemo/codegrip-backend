package com.mb.codegrip.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.QualityCondition;

@Repository
public interface QualityConditionRepository extends JpaRepository<QualityCondition, Integer> {

	@Modifying
	@Transactional
	@Query("delete from QualityCondition u where u.quality in (Select q.id from Quality q where q.projectBranchId in (select pb.id from ProjectBranch pb where pb.projects in (select p.id from Projects p where p.userId= :userId)))")
	Integer deleteQualityCondition(@Param("userId") Integer id);

}
