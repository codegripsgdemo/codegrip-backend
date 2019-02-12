package com.mb.codegrip.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dto.UsersAccountDetails;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CommitModel;
import com.mb.codegrip.model.CommonCommitModel;
import com.mb.codegrip.model.ProjectsModel;

@Configuration
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class GitLabAPIUtil implements EnvironmentAware {

	private static final Logger LOGGER = Logger.getLogger(GitLabAPIUtil.class);

	private static CommonUtil commonUtil = new CommonUtil();

	private static Environment environment;

	public String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	BitbucketAPIUtil bitbucketAPIUtil = new BitbucketAPIUtil();

	/*********************************************************************************************************************
	 * Get new access token API For GitLab.
	 * 
	 * @param string
	 * 
	 * @throws JSONException
	 *********************************************************************************************************************/
	public JSONObject getNewAccessTokenApi(String code, String value) throws JSONException {
		LOGGER.info("<------------ In get gitLab access token method. ----------->");
		String finalURL = environment.getProperty(CodeGripConstants.GITLAB_ACCESS_TOKEN_URL);
		JSONObject jsonObject = new JSONObject();
		try {
			finalURL = finalURL.replace("<CLIENT_ID>", environment.getProperty(CodeGripConstants.GITLAB_CLIENT_ID))
					.replace("<CLIENT_SECRET>", environment.getProperty(CodeGripConstants.GITLAB_CLIENT_SECRET));
			if (value.equals("code"))
				finalURL = finalURL + "&code=" + code + "&grant_type=authorization_code";
			else
				finalURL = finalURL + "&refresh_token=" + code + "&grant_type=refresh_token";

			LOGGER.info("Final GITLAB URL--------->" + finalURL);
			HttpHeaders headers = new HttpHeaders();

			MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
			HttpEntity<?> httpEntity = new HttpEntity<>(body, headers);
			RestTemplate restTemplate = new RestTemplate();
			String val = restTemplate.postForObject(finalURL, httpEntity, String.class);
			jsonObject = new JSONObject(val);
			String accessToken = jsonObject.getString("access_token");

			LOGGER.info("---------------------------------" + accessToken);
			LOGGER.info("--------------------" + jsonObject);
		} catch (Exception e) {
			LOGGER.info(e.getMessage());
		}
		return jsonObject;

	}

	/****************************************************************************
	 * With access token get git lab user id.
	 **************************************************************************/
	public JSONObject getGitLabUserDetails(String accessToken) throws JSONException, CustomException {
		LOGGER.info("-----------------IN gitLabUserDetails method------------------");
		String val = "";
		try {
			String finalUrl = environment.getProperty(CodeGripConstants.GET_ID_FROM_ACCESS_TOKEN);
			finalUrl = finalUrl.replace("<ACCESS_TOKEN>", accessToken);
			LOGGER.info("Final URL--------------" + finalUrl);
			val = bitbucketAPIUtil.callRestAPI(finalUrl);
		} catch (Exception e) {
			LOGGER.info(e);
			throw new CustomException(e.getMessage());
		}
		return new JSONObject(val);
	}

	/***************************************************************************************
	 * With access_token and user_id get project list from gitlab
	 ************************************************************************************/
	public List<ProjectsModel> getProjectListFromGitLab(int id, String token) throws CustomException, JSONException {

		LOGGER.info("--------------------------------------------IN GITLAB ---------------- ");
		String finalUrl = "";

		finalUrl = environment.getProperty(CodeGripConstants.GITLAB_PROJECT_LIST_API);
		finalUrl = finalUrl + id;
		finalUrl = finalUrl + "/projects?access_token=" + token;
		LOGGER.info("Project list url: " + finalUrl);
		String result = callRestAPI(finalUrl);
		JSONArray jsonArray = new JSONArray(result);
		return creatProjectJSONGitLab(jsonArray);
	}

	/*********************************************************************************************************************
	 * Call rest API function.
	 *********************************************************************************************************************/
	public String callRestAPI(String url) throws CustomException {
		try {
			RestTemplate restTemplate = new RestTemplate();
			return restTemplate.getForObject(url, String.class);
		} catch (Exception exception) {
			LOGGER.info(exception);
			throw new CustomException(exception.getMessage());
		}

	}

	/***********************************************************************************
	 * support method to get project list from gitlab
	 *********************************************************************************/
	private List<ProjectsModel> creatProjectJSONGitLab(JSONArray jsonArray) throws CustomException {

		List<ProjectsModel> repoList = new ArrayList<>();
		try {
			for (int i = 0; i < jsonArray.length(); i++) {
				/** map project details with repo */
				JSONObject repoJson = jsonArray.getJSONObject(i);
				ProjectsModel projectsModel = new ProjectsModel();
				projectsModel.setName(repoJson.getString("name"));
				projectsModel.setProjectId(repoJson.getInt("id"));
				projectsModel.setCreatedDate(commonUtil.getCurrentTimeStampInString());
				projectsModel.setGitCloneUrl(repoJson.getString("ssh_url_to_repo"));
				JSONObject owner = repoJson.getJSONObject("owner");
				projectsModel.setUserName(owner.getString("username"));
				projectsModel.setIsAnalyzeStarted(false);
				repoList.add(projectsModel);
			}

		} catch (Exception exception) {
			LOGGER.info(exception);
			throw new CustomException(exception.getMessage());
		}
		return repoList;

	}

	public List<CommitModel> getCommitsFromGitlab(String projectKey, UsersAccountDetails usersAccountDetails) {
		LOGGER.info("------------------------------------IN GITLAB Get commit details---------------------------- ");

		JSONArray jsonArray = new JSONArray();
		try {
			JSONObject jsonObject = getNewAccessTokenApi(usersAccountDetails.getRefreshToken(),
					CodeGripConstants.REFRESH_TOKEN);

			String finalUrl = environment.getProperty(CodeGripConstants.GITLAB_COMMIT_URL);
			finalUrl = finalUrl.replace("<PROJECT_ID>", projectKey).replace("<ACCESS_TOKEN>",
					jsonObject.getString(CodeGripConstants.ACCESS_TOKEN));
			LOGGER.info("Project list url: " + finalUrl);
			String result = callRestAPI(finalUrl);
			jsonArray = new JSONArray(result);
		} catch (JSONException | CustomException e) {
			LOGGER.error(e.getMessage());
		}
		return createCommitJSONGitlab(jsonArray);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<CommitModel> createCommitJSONGitlab(JSONArray jsonArray) {
		List<CommitModel> commitModels = new ArrayList();
		try {
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				CommitModel commitModel = new CommitModel();

				commitModel.setDate(commonUtil.progressDateFormat(jsonObject.getString("committed_date")));
				commitModel.setMessage(jsonObject.getString("message"));

				// get owner details
				commitModel.setAuthor(jsonObject.getString("author_name"));
				// get Project git url
				commitModel.setLink("https://gitlab.com/users/sign_in");
				commitModel.setProvider("gitlab");
				commitModels.add(commitModel);
			}
		} catch (JSONException | ParseException e) {
			LOGGER.error(e.getMessage());
		}
		return commitModels;
	}

	/******************************************************************************************************
	 * Add webhook to Github Method.
	 *******************************************************************************************************/
	public void addWebhookToGitlab(ProjectsModel projectsModel, UsersAccountDetails usersAccountDetails,
			Boolean webhookAdded) {
		/** JSONObject newAccessToken = new JSONObject(); */
		try {
			/**
			 * newAccessToken = getNewAccessTokenApi(usersAccountDetails.getRefreshToken(),
			 * CodeGripConstants.REFRESH_TOKEN);
			 */

			if (!webhookAdded) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				JSONObject jsonObject = new JSONObject();

				/**
				 * Map<String, String> configData = new HashMap<>(); configData.put("url",
				 * environment.getProperty(CodeGripConstants.COMMIT_WEBHOOK_URL_CG_SERVER));
				 * configData.put("content_type", "json");
				 */

				jsonObject.put("push_events", true);
				jsonObject.put("url", environment.getProperty(CodeGripConstants.COMMIT_WEBHOOK_URL_CG_SERVER));
				/**
				 * body.add(CodeGripConstants.ACTIVE, true); body.add(CodeGripConstants.EVENTS,
				 * new String[] { "push" });
				 */
				// Note the body object as first parameter!
				HttpEntity<?> httpEntity = new HttpEntity<>(jsonObject.toString(), headers);
				RestTemplate restTemplate = new RestTemplate();
				String url = "";
				url = environment.getProperty(CodeGripConstants.GITLAB_ADD_WEBHOOK);
				url = url + projectsModel.getProjectId() + "/hooks?access_token="
						+ usersAccountDetails.getAccessToken();
				LOGGER.info("Code url: " + url);
				String val = restTemplate.postForObject(url, httpEntity, String.class);
				LOGGER.info("New access token result :" + val);
			}
		} catch (JSONException e) {
			LOGGER.error(e.getMessage());
		}
		/** return newAccessToken; */

	}

	/******************************************************************************************************
	 * Add ssh/deploy key to gitlab.
	 *******************************************************************************************************/
	public JSONObject addSSHOverGitlab(ProjectsModel projectsModel, UsersAccountDetails usersAccountDetails,
			boolean isSSHAdded) throws IOException {
		JSONObject newAccessToken = new JSONObject();
		/**
		 * newAccessToken = getNewAccessTokenApi(usersAccountDetails.getRefreshToken(),
		 * CodeGripConstants.REFRESH_TOKEN);
		 */
		if (!isSSHAdded) {
			// load ssh file details here
			ClassLoader classLoader = getClass().getClassLoader();
			String sshFile = classLoader.getResource("SSHKeys").getFile();
			sshFile = sshFile + File.separator + (environment.getProperty(CodeGripConstants.SSH_PATH)) + File.separator
					+ "id_rsa.pub";
			sshFile = sshFile.replaceAll("%20", " ");
			File scanner = new File(sshFile);
			BufferedReader br = new BufferedReader(new FileReader(scanner));
			String st;
			StringBuilder sshKey = new StringBuilder();
			try {
				while ((st = br.readLine()) != null)
					sshKey.append(st);
				LOGGER.info("SSH key: " + sshKey);
				newAccessToken.put("sshKey", sshKey);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("title", environment.getProperty(CodeGripConstants.CG_SSH_TITLE));
				jsonObject.put(CodeGripConstants.SSH_KEY_NAME, sshKey);
				jsonObject.put("can_push", true);
				HttpEntity<?> httpEntity = new HttpEntity<>(jsonObject.toString(), headers);
				RestTemplate restTemplate = new RestTemplate();
				String url = "";
				url = environment.getProperty(CodeGripConstants.ADD_SSH_OVER_GITLAB_REPO);
				url = url + projectsModel.getProjectId();
				url = url + "/deploy_keys?access_token=" + usersAccountDetails.getAccessToken();
				LOGGER.info("Body: " + httpEntity.getBody());
				LOGGER.info("SSH url: " + url);
				String val = restTemplate.postForObject(url, httpEntity, String.class);
				LOGGER.info("SSH added response :" + val);
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
				LOGGER.error(environment.getProperty(CodeGripConstants.SSH_FILE_UNABLE_TO_READ) + " - Cause :"
						+ e.getMessage());
			} finally {
				br.close();
			}
		}
		return newAccessToken;
	}

	
	
	/******************************************************************************************************
	 * Save gitlab webhook data.
	 * @throws CustomException 
	 *******************************************************************************************************/
	public CommonCommitModel createCommitJSONofGitlabWebhook(JSONObject jsonObject) throws CustomException {
		CommonCommitModel commonCommitModel = new CommonCommitModel();
		try {
			
			// set user details
			commonCommitModel.setPusherName(jsonObject.getString("user_name"));
			
			//get branch details
			JSONObject repository = jsonObject.getJSONObject("repository");
			String branchName = repository.getString("ref");
			String[] branchNameArr = branchName.split("/");
			commonCommitModel.setBranchName(branchNameArr[2]);
			commonCommitModel.setBranchCloningKey(branchName);
			
			// get project details
			commonCommitModel.setCommitId(jsonObject.getString("checkout_sha"));
			JSONObject project = jsonObject.getJSONObject("project");
			commonCommitModel.setUid(project.getString("id"));
			commonCommitModel.setProjectName(project.getString("name"));
			commonCommitModel.setGitUrl(project.getString("git_ssh_url"));
			
			// set commit details.
			JSONArray commits = jsonObject.getJSONArray("commits");
			JSONObject firstCommit = commits.getJSONObject(0);
			commonCommitModel.setMessage(firstCommit.getString("message"));
			commonCommitModel.setTimestamp(firstCommit.getString("timestamp"));
			
			//set author details.
			JSONObject author = firstCommit.getJSONObject("author");
			commonCommitModel.setAuthor(author.getString("name"));
			
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return commonCommitModel;
	}
}
