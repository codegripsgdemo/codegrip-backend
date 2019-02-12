package com.mb.codegrip.utils;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dto.UsersAccountDetails;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.CommitModel;
import com.mb.codegrip.model.CommonCommitModel;
import com.mb.codegrip.model.ProjectsModel;
import com.mb.codegrip.model.UsersModel;

@Configuration
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:exception.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class GithubAPIUtil implements EnvironmentAware {

	private static final Logger LOGGER = Logger.getLogger(GithubAPIUtil.class);

	private static Environment environment;

	private static CommonUtil commonUtil = new CommonUtil();

	public static String getProperty(String key) {
		return environment.getProperty(key);
	}

	@SuppressWarnings("static-access")
	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	private static BitbucketAPIUtil bitbucketAPIUtil = new BitbucketAPIUtil();

	/*********************************************************************************************************************
	 * Get new access token API.
	 * 
	 * @throws JSONException
	 * @throws CustomException
	 *********************************************************************************************************************/
	public JSONObject getNewAccesstokenGithub(String code, String type) throws JSONException, CustomException {
		LOGGER.info("<------------ In get github access token method. ----------->");
		String finalURL = environment.getProperty(CodeGripConstants.GITHUB_ACCESS_TOKEN_URL);
		try {
			finalURL = finalURL.replace("<CLIENT_ID>", environment.getProperty(CodeGripConstants.GITHUB_CLIENT_ID))
					.replace("<CLIENT_SECRET>", environment.getProperty(CodeGripConstants.GITHUB_SECRET))
					.replace("<SCOPE>", environment.getProperty(CodeGripConstants.GITHUB_SCOPE))
					.replace("<CODE>", code);
			LOGGER.info("github access token URL: " + finalURL);
		} catch (Exception e) {
			LOGGER.error(e);
		}
		String json = bitbucketAPIUtil.callRestAPI(finalURL);
		LOGGER.info("Github token: " + json);
		LOGGER.info("type: " + type);
		String[] jsonResponse = json.split("&");
		if (jsonResponse[0].equals("error")) {
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("access_token", jsonResponse[0].substring(13));
		return jsonObject;
	}

	/*********************************************************************************************************************
	 * Get user from github.
	 * 
	 * @throws JSONException
	 *********************************************************************************************************************/
	public JSONObject getUserDataFromGithub(String token) throws JSONException, CustomException {
		return new org.json.JSONObject(
				bitbucketAPIUtil.callRestAPI(environment.getProperty(CodeGripConstants.GITHUB_USER_URL) + "" + token));
	}

	/***********************************************************************************************************************
	 * get repolist from bitbucket.
	 * 
	 * @throws JSONException
	 ************************************************************************************************************************/
	public List<ProjectsModel> getRepositorylistFromGithub(String url, String accessToken, Integer pageNumber)
			throws CustomException, JSONException {
		List<ProjectsModel> projectsModels = new ArrayList<>();
		url = url + "?access_token" + accessToken + "&per_page=" + CodeGripConstants.REPO_PAGE_SIZE;
		LOGGER.info("Repo url: " + url);
		ResponseEntity<String> result = bitbucketAPIUtil.callRestAPIGithubRepo(url);
		JSONObject jsonArrayHeaders = new JSONObject(result.getHeaders());
		Integer size = 0;
		if (jsonArrayHeaders.has("Link")) {
			size = getRepoCount(jsonArrayHeaders);
			if (size > 1) {
				for (int i = 1; i <= size; i++) {
					StringBuilder newUrl = new StringBuilder();
					newUrl.append(url);
					newUrl.append("&page="+i);
					String res = bitbucketAPIUtil.callRestAPI(url);
					JSONArray jsonArray = new JSONArray(res);
					projectsModels.addAll(creatProjectJSONGithub(jsonArray));
				}
			}

		} else {
			JSONArray jsonArray = new JSONArray(result.getBody());
			LOGGER.info("Headers: " + jsonArrayHeaders);
			LOGGER.info("body: " + jsonArray);
			projectsModels.addAll(creatProjectJSONGithub(jsonArray));
		}
		return projectsModels;
	}

	private Integer getRepoCount(JSONObject jsonArrayHeaders) throws JSONException {
		JSONArray jsonArray = jsonArrayHeaders.getJSONArray("Link");
		LOGGER.info("Link data: " + jsonArray.get(0));

		String[] val = jsonArray.get(0).toString().split(",");
		LOGGER.info(val);
		Integer size = val.length;
		LOGGER.info(val[size - 1]);
		if (val[size - 1].contains("last")) {
			String newStr = val[size - 1].substring(val[size - 1].indexOf("&page="),
					val[size - 1].indexOf('>', val[size - 1].indexOf("&page=")));
			LOGGER.info(newStr);
			String[] finalStr = newStr.split("=");
			LOGGER.info(finalStr[1]);
			return Integer.parseInt(finalStr[1]);
		} else
			return 1;

	}

	/*********************************************************************************************************************
	 * Create project JSON function.
	 *********************************************************************************************************************/
	private List<ProjectsModel> creatProjectJSONGithub(JSONArray resultJson) throws CustomException {

		List<ProjectsModel> repoList = new ArrayList<>();
		try {
			for (int i = 0; i < resultJson.length(); i++) {

				JSONObject repoJson = resultJson.getJSONObject(i);
				ProjectsModel projectsModel = new ProjectsModel();
				projectsModel.setName(repoJson.getString("name"));
				projectsModel.setUid(repoJson.getString(CodeGripConstants.NODE_ID));
				projectsModel.setCreatedDate(commonUtil.getCurrentTimeStampInString());
				projectsModel.setGitCloneUrl(repoJson.getString("git_url"));
				JSONObject owner = repoJson.getJSONObject("owner");
				projectsModel.setUserName(owner.getString(CodeGripConstants.LOGIN));
				projectsModel.setIsAnalyzeStarted(false);
				repoList.add(projectsModel);
			}

		} catch (Exception exception) {
			LOGGER.info(exception);
			throw new CustomException(exception.getMessage());
		}

		return repoList;
	}

	/*********************************************************************************************************************
	 * Create project JSON function.
	 * 
	 * @param accessToken
	 *********************************************************************************************************************/
	public UsersModel createUserObjectFromGithub(JSONObject resultJson, String accessToken) throws CustomException {
		UsersModel users = new UsersModel();

		try {
			users.setName(resultJson.getString(CodeGripConstants.LOGIN));
			users.setGithubLogin(resultJson.getString(CodeGripConstants.LOGIN));
			users.setGithubUserId(resultJson.getDouble("id"));
			if (resultJson.isNull(CodeGripConstants.EMAIL)) {
				users.setEmail(getEmailFromGithub(accessToken));
			} else {
				users.setEmail(resultJson.getString("email"));
			}
			users.setProvider("github");
			users.setProfilePictureUrl(resultJson.getString("avatar_url"));
			/**
			 * if(resultJson.getString("bio")!=null)
			 * users.setAboutMe(resultJson.getString("bio"));
			 */
			/**
			 * if(resultJson.getJSONObject("email")!=null)
			 * users.setEmail(resultJson.getString("email"));
			 */
		} catch (Exception exception) {
			LOGGER.info(exception);
			throw new CustomException(exception.getMessage());
		}

		return users;
	}

	/************************************************************************************************
	 * Get email from github.
	 * 
	 * @throws JSONException
	 ***********************************************************************************************/
	private String getEmailFromGithub(String accessToken) throws CustomException, JSONException {
		String email = "";
		String response = callRestAPI(environment.getProperty(CodeGripConstants.GTIHUB_GET_EMAIL) + accessToken);
		JSONArray jsonArray = new JSONArray(response);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			if (jsonObject.getBoolean("primary")) {
				email = jsonObject.getString("email");
				break;
			}
		}
		return email;
	}

	/************************************************************************************************
	 * Call github rest API function.
	 ***********************************************************************************************/
	public String callRestAPI(String url) throws CustomException {
		try {
			RestTemplate restTemplate = new RestTemplate();
			return restTemplate.getForObject(url, String.class);
		} catch (Exception exception) {
			LOGGER.info(exception);
			throw new CustomException(exception.getMessage());
		}

	}

	/*********************************************************************************************************************
	 * for getting github details from by passing code.
	 *********************************************************************************************************************/
	public UsersModel getGithubDetails(String accessToken) throws JSONException, CustomException {
		LOGGER.info("------------ get github details --------------");
		JSONObject getUserData = getUserDataFromGithub(accessToken);
		return createUserObjectFromGithub(getUserData, accessToken);
	}

	/*********************************************************************************************************************
	 * Get commits from github.
	 *********************************************************************************************************************/
	public List<CommitModel> getCommitsFromGithub(String projectName, UsersAccountDetails usersAccountDetails)
			throws ParseException {
		JSONArray getCommitData = new JSONArray();
		try {
			getCommitData = getCommitDataFromGithubAPI(usersAccountDetails, projectName);
		} catch (JSONException | CustomException e) {
			LOGGER.error(e.getMessage());
		}
		return createCommitJSONOfGithub(getCommitData);
	}

	/*********************************************************************************************************************
	 * Create commit JSON from github.
	 *********************************************************************************************************************/
	private List<CommitModel> createCommitJSONOfGithub(JSONArray getCommitData) throws ParseException {
		List<CommitModel> commitModels = new ArrayList<>();

		int length = (getCommitData.length() >= 4 ? 4 : getCommitData.length());
		for (int i = 0; i < length; i++) {
			CommitModel commitModel = new CommitModel();
			try {
				JSONObject jsonObject = getCommitData.getJSONObject(i);
				commitModel.setCommitHash(jsonObject.getString("node_id"));
				commitModel.setLink(jsonObject.getString("html_url"));

				// Profile image.
				JSONObject author = jsonObject.getJSONObject(CodeGripConstants.AUTHOR);
				commitModel.setUserProfileIcon(author.getString("avatar_url"));

				commitModel.setProvider("github");
				JSONObject commitData = jsonObject.getJSONObject("commit");
				commitModel.setMessage(commitData.getString("message"));
				JSONObject authorData = commitData.getJSONObject(CodeGripConstants.AUTHOR);
				commitModel.setAuthor(authorData.getString("name"));

				commitModel.setDate(commonUtil.progressDateFormat(authorData.getString("date")));
				commitModels.add(commitModel);
			} catch (JSONException e) {
				LOGGER.error(e.getMessage());
			}
		}
		return commitModels;
	}

	/*********************************************************************************************************************
	 * Get commit details from github.
	 *********************************************************************************************************************/
	public JSONArray getCommitDataFromGithubAPI(UsersAccountDetails usersAccountDetails, String projectName)
			throws JSONException, CustomException {
		String url = environment.getProperty(CodeGripConstants.GITHUB_REPO_COMMIT_URL);
		url = url.replace("<USER_NAME>", usersAccountDetails.getAccountUsername());
		url = url.replace("<REPO_NAME>", projectName);
		return new org.json.JSONArray(bitbucketAPIUtil.callRestAPI(url + "" + usersAccountDetails.getAccessToken()));
	}

	/******************************************************************************************************
	 * Add webhook to Github Method.
	 *******************************************************************************************************/
	public JSONObject addWebhookToGithub(ProjectsModel projectsModel, UsersAccountDetails usersAccountDetails,
			Boolean webhookAdded) throws JSONException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject configData = new JSONObject();
		configData.put("url", environment.getProperty(CodeGripConstants.GITHUB_COMMIT_WEBHOOK_URL));
		configData.put("content_type", "json");

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(CodeGripConstants.NAME, "web");
		jsonObject.put(CodeGripConstants.CONFIG, configData);
		jsonObject.put(CodeGripConstants.ACTIVE, true);
		jsonObject.put(CodeGripConstants.EVENTS, new String[] { "push" });

		if (!webhookAdded) {
			// Note the body object as first parameter!
			HttpEntity<?> httpEntity = new HttpEntity<>(jsonObject.toString(), headers);
			RestTemplate restTemplate = new RestTemplate();
			String url = "";
			url = environment.getProperty(CodeGripConstants.GITHUB_ADD_WEBHOOK) + usersAccountDetails.getAccessToken();
			url = url.replace("<USER_NAME>", usersAccountDetails.getAccountUsername()).replace("<REPO_NAME>",
					projectsModel.getName());
			try {
				restTemplate.postForObject(url, httpEntity, String.class);
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
		jsonObject.put("access_token", usersAccountDetails.getAccessToken());
		return jsonObject;
	}

	/**************************************************************************************************
	 * Create commit json from github data.
	 * 
	 * @throws JSONException
	 **************************************************************************************************/
	public CommonCommitModel createCommitJSONofGithubWebhook(JSONObject jsonObject) throws JSONException {
		CommonCommitModel githubCommitModel = new CommonCommitModel();

		JSONObject commits = new JSONObject();
		if (jsonObject.has("head_commit")) {
			commits = jsonObject.getJSONObject("head_commit");
		}
		String fullBranchName = jsonObject.getString("ref");

		// set branch cloning key.
		githubCommitModel.setBranchCloningKey(fullBranchName);

		String[] branchNameArr = fullBranchName.split("/");
		githubCommitModel.setBranchName(branchNameArr[2]);

		// set commits data.
		githubCommitModel.setCommitId(commits.getString("id"));
		githubCommitModel.setMessage(commits.getString("message"));

		// set author details.
		JSONObject author = commits.getJSONObject(CodeGripConstants.AUTHOR);
		githubCommitModel.setPusherName(author.getString("name"));

		// set repository details.
		JSONObject reposiotry = jsonObject.getJSONObject("repository");
		githubCommitModel.setUid(reposiotry.getString("node_id"));
		githubCommitModel.setProjectName(reposiotry.getString("name"));
		githubCommitModel.setGitUrl(reposiotry.getString("git_url"));

		// Get owner login name.
		JSONObject owner = reposiotry.getJSONObject("owner");
		githubCommitModel.setOwnerLoginId(owner.getString(CodeGripConstants.LOGIN));

		return githubCommitModel;
	}
}
