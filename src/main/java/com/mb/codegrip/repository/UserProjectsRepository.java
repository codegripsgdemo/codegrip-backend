package com.mb.codegrip.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mb.codegrip.dto.UserProjects;
import com.mb.codegrip.model.UserProjectsModel;

@Repository
public interface UserProjectsRepository extends JpaRepository<UserProjects, Integer>{

	void save(List<UserProjectsModel> userProjectsModels);
	
}
