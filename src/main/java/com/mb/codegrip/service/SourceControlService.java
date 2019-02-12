package com.mb.codegrip.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.springframework.stereotype.Service;

import com.mb.codegrip.dto.Projects;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CompanySubscriptionModel;
import com.mb.codegrip.model.ProjectsModel;
import com.mb.codegrip.model.SourceControlModel;

@Service
public interface SourceControlService {

	Map<String, Object> getSourceControlRepository(SourceControlModel sourceControlModel, HttpServletRequest request, String pageNo) throws CustomException, JSONException;
	Map<String, Object> getStartedProjectList(Integer companyId, String pageNo, String role, Integer user, Integer projectId) throws CustomException ;
	String getFileContnet(SourceControlModel sourceControlModel);
	void hardDeleteUserData(List<String> email);
	Object adminDashboardData(ProjectsModel projectsModel) throws CustomException, ParseException;
	org.json.JSONObject getBlockerAndSecurityCriticalOfProject(String projectName, String severity, String severityVal)
			throws IOException, JSONException;
	CompanySubscriptionModel getSubscriptionAndProjectDetails(Integer ownerCompanyId) throws CustomException;
	void deleteProject(List<Projects> projectListt);
	
	
	
}
