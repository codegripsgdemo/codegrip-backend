package com.mb.codegrip.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.Quality;

@Repository
public interface QualityRepository extends JpaRepository<Quality, Integer>{

	List<Quality> findByCommitIdIn(List<String> commitHash);
	Quality findByProjectBranchIdOrderByAnalyzeAtDesc(Integer id);
	Quality findByCommitId(String string);
	Quality findById(Integer id);
	List<Quality> findByProjectBranchIdIn(List<Integer> ids);
	
	
	@Modifying
	@Transactional
	@Query(" delete from Quality q where q.projectBranchId in (select pb.id from ProjectBranch pb where pb.projects in (select p.id from Projects p where p.userId= :userId))")
	Integer deleteQualityCondition(@Param("userId") Integer id);
}
