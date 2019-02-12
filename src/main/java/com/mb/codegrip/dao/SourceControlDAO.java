package com.mb.codegrip.dao;

import java.util.List;

import com.mb.codegrip.dto.Projects;
import com.mb.codegrip.exception.CustomException;

public interface SourceControlDAO {

	String getRefreshTokenFromDB(Integer id);

	List<Projects> getStartedProjectList(Integer companyId, String pageNo, List<Integer> list) throws CustomException;

	Projects getSingleStartedProjectDetailsByCompanyIdAndUserId(Integer companyId, Integer userId)
			throws CustomException;

	List<Projects> getStartedProjectListOfUser(Integer userId, String pageNo, Integer companyId, List<Integer> list) throws CustomException;


}
