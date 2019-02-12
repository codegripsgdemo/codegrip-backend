package com.mb.codegrip.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mb.codegrip.dto.Projects;

public interface ProjectsRepository extends JpaRepository<Projects, Integer>{

	Projects findByNameAndCompanyId(String name, Integer companyId);

	@Query("SELECT p.name FROM Projects p where p.id = :id")
	String findNameById(@Param("id")int id);

	Projects findByNameAndUserIdAndCompanyId(String string, Integer id, Integer integer);

	Projects findByCompanyIdAndIdAndUserId(Integer companyId, Integer projectId, Integer userId);

	Projects findById(Integer projectId);

	List<Projects> findByIdAndIsDeleted(Integer projectId, boolean b);

	List<Projects> findByCompanyIdAndUserIdAndIsDeletedAndIsAnalyzeStarted(Integer companyId, Integer userId, boolean b, boolean c);
	
	List<Projects> findByCompanyId(Integer companyId);

}
