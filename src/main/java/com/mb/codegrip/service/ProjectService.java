package com.mb.codegrip.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;

import com.mb.codegrip.dto.Users;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.FilterModel;
import com.mb.codegrip.model.ProjectsModel;
import com.mb.codegrip.model.ShareDashboardModel;
import com.mb.codegrip.model.UserProjectsModel;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface ProjectService {

	public JSONObject getQualityGatesReport(FilterModel filterModel, String projectKey, String pageNo)
			throws IOException;

	public JSONObject getQualityGatesRule(String rule) throws IOException;

	public StringBuilder callGetAPI(String url) throws IOException;

	public JSONObject getFileCode(String projectKey, String startLine, String endLine) throws IOException;

	public JSONArray getIssues(String projectKey) throws IOException;

	public Users getEmailAndCompanyId(String email);
	public void saveCodeGripQuality(Object webhookObject, HttpServletRequest request) throws CustomException, ParseException;
	public ProjectsModel startAnalyzeProject(ProjectsModel projectsModel, HttpServletRequest request) throws CustomException, JSONException;
	public JSONObject getCodeList(String projectDir)throws IOException;
	public Map<String, Object> getProgressOfProject(String projectKey, HttpServletRequest request, Integer userId, Integer companyId, String role) throws  CustomException, JSONException;	
	public JSONObject getDuplicateCode(String projectKey)throws IOException;
	public Map<String, Object> getCommitDetails(String commitHash, String commitHashBottom, HttpServletRequest request);
	void sendProjectAnalysisMail(String email, String projectName, String errorMessage);

	public String sendProjectService(ShareDashboardModel shareDashboardModel, HttpServletRequest request) throws CustomException;

	public List<Object[]> getByEmailId(String emailId, HttpServletRequest request, Integer companyId, Integer userId) throws CustomException;

	public Map<String, Object> getSecurityReportDetails(String projectKey) throws IOException;

	public JSONObject getIssueList(String projectName, int pageNumber, String owaspTop) throws IOException;

	List<Object[]> getAlreadyShareEmailIds(int projectId, int senderId);

	void assignProjectToUser(List<UserProjectsModel> userProjectsModels, Integer companyId, Integer projectId, Integer userId) throws CustomException;

	public void removeProjectFromUser(List<UserProjectsModel> userProjectsModel) throws CustomException;

	public void deleteProject(Integer companyId, Integer userId, Integer projectId) throws CustomException;

	public JSONObject createQualityConditions(String projectName) throws CustomException;

	public Object startedProjects(Integer companyId, Integer userId, String role) throws CustomException;

	public void updateStatusAndSendMailToAdmin(ProjectsModel projectsModel, String output, String string) throws CustomException;

		
}
