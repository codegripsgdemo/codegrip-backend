package com.mb.codegrip.dao;

import java.util.List;

import com.mb.codegrip.dto.ProjectBranch;
import com.mb.codegrip.dto.Projects;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CommonCommitModel;

public interface ProjectDAO {

	ProjectBranch getBranchDetails(String name, String branchKey) throws CustomException;

	Projects saveProjectsRecord(Projects projects);

	void saveQualityConditionRecord(List<ProjectBranch> projectBranchs);

	ProjectBranch getProjectBranchByBranchKeyAndProjectId(Projects dbProjects, CommonCommitModel githubCommitModel);

	Projects getStartedProjectByUidAndUsername(CommonCommitModel githubCommitModel) throws CustomException;

	List<Projects> getAdminProjectList(List<Integer> uniqueUserIds, Integer companyId) throws CustomException;

	void deleteProject(Integer projectId) throws CustomException;

	void updateProjectErrorMessage(Integer projectId, String errorMessage, String staus) throws CustomException;

	void updateBranchErrorMessage(Integer projectBranchId, String errorMessage, String staus) throws CustomException;

}
