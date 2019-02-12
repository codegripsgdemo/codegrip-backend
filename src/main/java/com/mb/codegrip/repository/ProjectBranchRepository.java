package com.mb.codegrip.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.ProjectBranch;


@Repository
public interface ProjectBranchRepository extends JpaRepository<ProjectBranch, Integer>{
	
	public ProjectBranch findByNameAndBranchKey(String name, String branchKey);

}
