package com.mb.codegrip.utils;

import static com.mb.codegrip.constants.CodeGripConstants.ACCESS_TOKEN;
import static com.mb.codegrip.constants.CodeGripConstants.ACCESS_TOKEN_KEY;
import static com.mb.codegrip.constants.CodeGripConstants.BITBUCKET_PROVIDER;
import static com.mb.codegrip.constants.CodeGripConstants.GITHUB_PROVIDER;
import static com.mb.codegrip.constants.CodeGripConstants.GITLAB_PROVIDER;
import static com.mb.codegrip.constants.CodeGripConstants.LINKS;
import static com.mb.codegrip.constants.CodeGripConstants.USER_NAME_KEY;
import static com.mb.codegrip.constants.CodeGripConstants.VALUES;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dto.Projects;
import com.mb.codegrip.dto.UsersAccountDetails;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.mail.MailerHelper;
import com.mb.codegrip.model.CommitModel;
import com.mb.codegrip.model.CommonCommitModel;
import com.mb.codegrip.model.ProjectsModel;
import com.mb.codegrip.model.UsersModel;

@Configuration
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:exception.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class BitbucketAPIUtil implements EnvironmentAware {
	private static final Logger LOGGER = Logger.getLogger(BitbucketAPIUtil.class);

	private static Environment environment;

	private static CommonUtil commonUtil = new CommonUtil();

	@Autowired
	private MailerHelper mailerHelper;

	public String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	/*********************************************************************************************************************
	 * Get new access token API.
	 * 
	 * @throws JSONException
	 *********************************************************************************************************************/
	public JSONObject getNewAccesstokenBitbucket(String value, String type) throws JSONException {
		String encodedBytes = Base64.getEncoder()
				.encodeToString((environment.getProperty(CodeGripConstants.BITBUCKET_KEY) + ":"
						+ environment.getProperty(CodeGripConstants.BITBUCKET_SECRET)).getBytes());
		LOGGER.info(encodedBytes);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set(CodeGripConstants.AUTHORIZATION, CodeGripConstants.BASIC + " " + encodedBytes);
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

		if (type.equals(CodeGripConstants.REFRESH_TOKEN)) {
			body.add(CodeGripConstants.GRANT_TYPE, CodeGripConstants.REFRESH_TOKEN);
			body.add(CodeGripConstants.REFRESH_TOKEN, value);
		} else {
			body.add(CodeGripConstants.GRANT_TYPE, CodeGripConstants.AUTORIZATION_CODE);
			body.add(CodeGripConstants.CODE, value);
		}
		// Note the body object as first parameter!
		HttpEntity<?> httpEntity = new HttpEntity<>(body, headers);
		RestTemplate restTemplate = new RestTemplate();
		String url = "";
		url = environment.getProperty(CodeGripConstants.ACCESS_TOKEN_URL);
		LOGGER.info("new access token url: " + url);
		String val = restTemplate.postForObject(url, httpEntity, String.class);
		LOGGER.info("New access token result :" + val);
		return new JSONObject(val);
	}

	/*********************************************************************************************************************
	 * Get new access token API.
	 * 
	 * @throws JSONException
	 *********************************************************************************************************************/
	public JSONObject getUserNameFromBitbucket(String accessToken) throws CustomException, JSONException {
		String userNameUrl = environment.getProperty(CodeGripConstants.BITBUCKET_GET_USERNAME_URL);
		return new org.json.JSONObject(callRestAPI(userNameUrl + "" + accessToken));
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

	/*********************************************************************************************************************
	 * Call rest API function for github repo.
	 *********************************************************************************************************************/
	public ResponseEntity<String> callRestAPIGithubRepo(String url) throws CustomException {
		try {
			RestTemplate restTemplate = new RestTemplate();
			return restTemplate.getForEntity(url, String.class);
		} catch (Exception exception) {
			LOGGER.info(exception);
			throw new CustomException(exception.getMessage());
		}

	}

	/***********************************************************************************************************************
	 * get repolist from bitbucket.
	 ************************************************************************************************************************/
	public List<ProjectsModel> getRepositorylistFromBitbucket(String bucketAdminUserName, String accessToken)
			throws CustomException, JSONException {

		List<ProjectsModel> projectsModels = new ArrayList<>();
		String url = environment.getProperty(CodeGripConstants.BITBUCKET_GET_REOSITORIES_URL);
		url = url.replace(USER_NAME_KEY, bucketAdminUserName);
		projectsModels.addAll(getReposFromBitbucket(url, accessToken, new ArrayList<ProjectsModel>()));
		return projectsModels;
	}

	/*********************************************************************************************************************
	 * Get all repo from bitbucket.
	 *********************************************************************************************************************/
	public List<ProjectsModel> getReposFromBitbucket(String url, String accessToken, List<ProjectsModel> projectsModels)
			throws CustomException, JSONException {
		LOGGER.info("Repo url: " + url);
		url = url + "&access_token=" + accessToken;
		String result = callRestAPI(url);
		JSONObject resultJson = new org.json.JSONObject(result);
		if (resultJson.has("next")) {
			projectsModels.addAll(creatRepoJSON(resultJson));
			getReposFromBitbucket(resultJson.getString("next"), accessToken, projectsModels);

		} else {
			projectsModels.addAll(creatRepoJSON(resultJson));
		}
		return projectsModels;
	}

	/*********************************************************************************************************************
	 * Create repository JSON function.
	 *********************************************************************************************************************/
	private List<ProjectsModel> creatRepoJSON(JSONObject resultJson) throws CustomException {

		List<ProjectsModel> repoList = new ArrayList<>();
		try {
			JSONArray values = resultJson.getJSONArray(VALUES);
			for (int i = 0; i < values.length(); i++) {
				JSONObject repoJson = values.getJSONObject(i);
				ProjectsModel projectsModel = new ProjectsModel();
				projectsModel.setName(repoJson.getString("name"));
				projectsModel.setUid(repoJson.getString("uuid"));
				projectsModel.setCreatedDate(commonUtil.getCurrentTimeStampInString());
				projectsModel.setProjectSlugName(repoJson.getString("slug"));
				// get owner details
				JSONObject owner = repoJson.getJSONObject("owner");
				projectsModel.setUserName(owner.getString(CodeGripConstants.USERNAME));
				// get Project git url
				JSONObject links = repoJson.getJSONObject(LINKS);
				JSONArray gitUrl = links.getJSONArray("clone");
				JSONObject httpsUrl = (JSONObject) gitUrl.get(1);
				projectsModel.setGitCloneUrl(httpsUrl.getString("href"));
				repoList.add(projectsModel);
			}

		} catch (Exception exception) {
			LOGGER.info(exception);
			throw new CustomException(exception.getMessage());
		}

		return repoList;
	}

	/*********************************************************************************************
	 * Get latest commit API method.
	 *********************************************************************************************/
	public CommitModel getLatestCommitAPI(String projectName, Projects projects,
			UsersAccountDetails usersAccountDetails) throws JSONException {
		List<CommitModel> commitId = new ArrayList<>();
		if (BITBUCKET_PROVIDER.equalsIgnoreCase(projects.getProvider())) {
			try {
				commitId = getCommitsFromBitbucket(projectName, usersAccountDetails);
			} catch (CustomException e) {
				LOGGER.error(e.getMessage());
			}
		} else if (GITHUB_PROVIDER.equalsIgnoreCase(projects.getProvider())) {

		} else if (GITLAB_PROVIDER.equalsIgnoreCase(projects.getProvider())) {

		}
		return commitId.get(0);
	}

	/*********************************************************************************************
	 * Get commit details from bitbucket.
	 *********************************************************************************************/
	public List<CommitModel> getCommitsFromBitbucket(String projectName, UsersAccountDetails usersAccountDetails)
			throws CustomException, JSONException {
		JSONObject newAccessToken = getNewAccesstokenBitbucket(usersAccountDetails.getRefreshToken(),
				CodeGripConstants.REFRESH_TOKEN);
		String url = environment.getProperty(CodeGripConstants.GET_COMMIT_URL);
		url = url.replace(USER_NAME_KEY, usersAccountDetails.getAccountUsername());
		url = url.replace(ACCESS_TOKEN_KEY, newAccessToken.getString(ACCESS_TOKEN));
		url = url.replace("<PROJECT_KEY>", projectName);
		LOGGER.info("Repo url: " + url);
		String result = callRestAPI(url);
		JSONObject resultJson = new org.json.JSONObject(result);
		return createCommitJSONFromBitbucket(resultJson);

	}

	/*********************************************************************************************
	 * Create commit json of bitbucket.
	 *********************************************************************************************/
	private List<CommitModel> createCommitJSONFromBitbucket(JSONObject resultJson) throws CustomException {
		List<CommitModel> commitList = new ArrayList<>();
		try {
			JSONArray values = resultJson.getJSONArray(VALUES);
			int length = (values.length() >= 4 ? 4 : values.length());
			for (int i = 0; i < length; i++) {
				JSONObject repoJson = values.getJSONObject(i);
				CommitModel commitModel = new CommitModel();
				commitModel.setDate(commonUtil.progressDateFormat(repoJson.getString("date")));
				LOGGER.info("Progress date format: " + commitModel.getDate());
				commitModel.setMessage(repoJson.getString("message"));
				commitModel.setCommitHash(repoJson.getString("hash"));
				// get owner details
				JSONObject owner = repoJson.getJSONObject("author");
				commitModel.setAuthor(owner.getString("raw"));

				// set profile image of user.
				if (owner.has("user")) {
					JSONObject user = owner.getJSONObject("user");
					JSONObject profileImage = user.getJSONObject("links");
					JSONObject avatar = profileImage.getJSONObject("avatar");
					commitModel.setUserProfileIcon(avatar.getString("href"));
				}

				// get Project git url
				JSONObject links = repoJson.getJSONObject(LINKS);
				JSONObject htmlLink = links.getJSONObject("html");
				commitModel.setLink(htmlLink.getString("href"));

				commitModel.setProvider(CodeGripConstants.BITBUCKET_PROVIDER);
				commitList.add(commitModel);
			}

		} catch (Exception exception) {
			LOGGER.info(exception);
			throw new CustomException(exception.getMessage());
		}

		return commitList;
	}

	/*********************************************************************************************
	 * Get user details from bitbucket.
	 *********************************************************************************************/
	public UsersModel getUserDetailsFromBitbucket(String accessToken) throws JSONException, CustomException {
		return createUserObject(getUserNameFromBitbucket(accessToken), accessToken);
	}

	/*********************************************************************************************
	 * Create user object of bitbucket user data.
	 *********************************************************************************************/
	private UsersModel createUserObject(JSONObject userDetailsFromBitbucket, String accessToken)
			throws JSONException, CustomException {
		UsersModel users = new UsersModel();
		if (!userDetailsFromBitbucket.has("email"))
			users.setEmail(getEmailFromBitbucket(accessToken));
		JSONObject links = userDetailsFromBitbucket.getJSONObject(LINKS);
		JSONObject avatar = links.getJSONObject("avatar");
		users.setProfilePictureUrl(avatar.getString("href"));
		users.setName(userDetailsFromBitbucket.getString("display_name"));
		users.setBitbucketAccountId(userDetailsFromBitbucket.getString("account_id"));
		users.setGithubLogin(userDetailsFromBitbucket.getString("username"));
		users.setProvider(CodeGripConstants.BITBUCKET_PROVIDER);
		return users;
	}

	/*********************************************************************************************
	 * Get email Id from bitbucket.
	 * 
	 * @throws JSONException
	 *********************************************************************************************/
	private String getEmailFromBitbucket(String accessToken) throws CustomException, JSONException {
		String email = "";
		String response = callRestAPI(environment.getProperty(CodeGripConstants.BITBUCKET_GET_EMAIL) + accessToken);
		JSONObject jsonObject = new JSONObject(response);
		JSONArray jsonArray = jsonObject.getJSONArray(VALUES);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject userEmail = jsonArray.getJSONObject(i);
			if (userEmail.getBoolean("is_primary"))
				email = userEmail.getString("email");
		}
		return email;
	}

	/*********************************************************************************************
	 * Add webhook to bitbucket.
	 *********************************************************************************************/
	public JSONObject addWebhookToBitbucket(ProjectsModel projectsModel, UsersAccountDetails usersAccountDetails,
			Boolean webhookAdded) {
		JSONObject newAccessToken = new JSONObject();
		try {
			newAccessToken = getNewAccesstokenBitbucket(usersAccountDetails.getRefreshToken(),
					CodeGripConstants.REFRESH_TOKEN);

			if (!webhookAdded) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				JSONObject jsonObject = new JSONObject();
				jsonObject.put(CodeGripConstants.DESCRIPTION, "CG push " + projectsModel.getName());
				jsonObject.put(CodeGripConstants.URL,
						environment.getProperty(CodeGripConstants.BITBUCKET_COMMIT_WEBHOOK));
				jsonObject.put(CodeGripConstants.EVENTS, new String[] { "repo:push" });
				jsonObject.put(CodeGripConstants.ACTIVE, true);
				HttpEntity<?> httpEntity = new HttpEntity<>(jsonObject.toString(), headers);
				RestTemplate restTemplate = new RestTemplate();
				String url = "";

				url = environment.getProperty(CodeGripConstants.BITBUCKET_ADD_WEBHOOK);
				url = url.replace(ACCESS_TOKEN_KEY, newAccessToken.getString(CodeGripConstants.ACCESS_TOKEN))
						.replace(USER_NAME_KEY, usersAccountDetails.getAccountUsername())
						.replace("<REPO_SLUG>", projectsModel.getName());
				LOGGER.info("Body: " + httpEntity.getBody());
				LOGGER.info("Code url: " + url);
				String val = restTemplate.postForObject(url, httpEntity, String.class);
				LOGGER.info("Webook Data :" + val);
			}
		} catch (JSONException e) {
			LOGGER.error(e.getMessage());
		}
		return newAccessToken;
	}

	/*********************************************************************************************
	 * Add ssh over bitbucket.
	 * 
	 * @throws IOException
	 * @throws JSONException
	 * @throws Exception
	 *********************************************************************************************/
	public JSONObject addSSHOverBitbucket(ProjectsModel projectsModel, UsersAccountDetails usersAccountDetails,
			Boolean isSSHAdded) throws IOException, JSONException {
		JSONObject newAccessToken = new JSONObject();

		newAccessToken = getNewAccesstokenBitbucket(usersAccountDetails.getRefreshToken(),
				CodeGripConstants.REFRESH_TOKEN);
		if (!isSSHAdded) {
			// load ssh file details here

			String os = System.getProperty("os.name");
			String[] osSplitVal = os.split(" ");
			String sshFile = "";
			if (!osSplitVal[0].equalsIgnoreCase("windows")) {
				sshFile = File.separator + "home" + File.separator + "ec2-user" + File.separator
						+ CodeGripConstants.SSH_FILE_NAME;
			} else {

				ClassLoader classLoader = getClass().getClassLoader();
				sshFile = classLoader.getResource("SSHKeys").getFile();
				sshFile = sshFile + File.separator + (environment.getProperty(CodeGripConstants.SSH_PATH))
						+ File.separator + CodeGripConstants.SSH_FILE_NAME;
				sshFile = sshFile.replaceAll("%20", " ");
			}
			/** File scanner = new File(sshFile); */

			String scannerPath = System.getProperty("user.home") + File.separator + ".ssh" + File.separator
					+ "id_rsa.pub";
			LOGGER.info("SSH path before: " + scannerPath);
			/**
			 * scannerPath = scannerPath.replace(File.separator + "root", "~");
			 */

			File scanner = new File(scannerPath);

			LOGGER.info("SSH path after: " + scanner);
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
				jsonObject.put(CodeGripConstants.SSH_KEY_NAME, sshKey);
				jsonObject.put(CodeGripConstants.SSH_LABEL, environment.getProperty(CodeGripConstants.CG_SSH_TITLE));
				HttpEntity<?> httpEntity = new HttpEntity<>(jsonObject.toString(), headers);
				RestTemplate restTemplate = new RestTemplate();
				String url = "";
				url = environment.getProperty(CodeGripConstants.ADD_SSH_OVER_BITBUCLET);
				url = url.replace(ACCESS_TOKEN_KEY, newAccessToken.getString(CodeGripConstants.ACCESS_TOKEN))
						.replace(USER_NAME_KEY, usersAccountDetails.getAccountUsername())
						.replace("<REPO_SLUG>", projectsModel.getName());
				LOGGER.info("Body: " + httpEntity.getBody());
				LOGGER.info("Code url: " + url);
				String val = restTemplate.postForObject(url, httpEntity, String.class);
				LOGGER.info("Webook Data :" + val);
			} catch (HttpClientErrorException e) {
				LOGGER.info(e.getMessage());
				/**try {
					if (e.getRawStatusCode() == 401) {
						mailerHelper.sendExceptionEmail(
								environment.getProperty(CodeGripConstants.SSH_UPLOAD_ISSUE_SUBJECT),
								branchId, e.getResponseBodyAsString());
					}

					if (e.getRawStatusCode() == 400) {
						mailerHelper.sendExceptionEmail(
								environment.getProperty(CodeGripConstants.SSH_UPLOAD_ISSUE_SUBJECT),
								branchId, e.getResponseBodyAsString());
					}
				} catch (Exception e2) {
					LOGGER.error(e2.getMessage());
					mailerHelper.sendExceptionEmail(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG),
							projectsModel.getProjectBranches().get(0).getId(), e2.getMessage());
				}*/
			} finally {
				br.close();
			}
		}
		return newAccessToken;

	}

	/*********************************************************************************************
	 * Get SSH key of local server.
	 *********************************************************************************************/
	public String getSshKey() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		File scanner = new File(classLoader.getResource("SSHKeys" + File.separator
				+ environment.getProperty(CodeGripConstants.SSH_PATH) + CodeGripConstants.SSH_FILE_NAME).getFile());

		String content = new String(Files.readAllBytes(Paths.get(scanner.getPath())));
		LOGGER.info(content);
		return content;
	}

	/*********************************************************************************************
	 * Create commit json of bitbucket from webhook.
	 * 
	 * @throws CustomException
	 *********************************************************************************************/
	public CommonCommitModel createCommitJSONofBitbucketWebhook(JSONObject jsonObject) throws CustomException {
		CommonCommitModel commonCommitModel = new CommonCommitModel();
		try {
			JSONObject push = jsonObject.getJSONObject("push");
			JSONArray changes = push.getJSONArray("changes");
			JSONObject changesIndex = changes.getJSONObject(0);
			JSONObject newCommit = changesIndex.getJSONObject("new");

			// set branch data here.
			commonCommitModel.setBranchName(newCommit.getString("name"));
			commonCommitModel.setBranchCloningKey(newCommit.getString("name"));

			JSONObject target = newCommit.getJSONObject("target");
			commonCommitModel.setCommitId(target.getString("hash"));
			commonCommitModel.setTimestamp(target.getString("date"));
			commonCommitModel.setMessage(target.getString("message"));

			// save user details here.
			JSONObject actor = jsonObject.getJSONObject("actor");
			commonCommitModel.setPusherName(actor.getString("display_name"));

			// save repository details.
			JSONObject repository = jsonObject.getJSONObject("repository");
			
			String[] projectName = repository.getString("full_name").split("/");
			commonCommitModel.setProjectName(projectName[1]);
			commonCommitModel.setUid(repository.getString("uuid"));

			// save owner details here.
			JSONObject owner = repository.getJSONObject("owner");
			commonCommitModel.setOwnerLoginId(owner.getString("username"));

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return commonCommitModel;
	}
	
	
	/*********************************************************************************************
	 *Get webhook list for Delete webhook from bitbucket.
	 *********************************************************************************************/
	public JSONObject listWebhookFromBitbucket(Projects project, UsersAccountDetails usersAccountDetails) {
		JSONObject newAccessToken = new JSONObject();
		try {
			newAccessToken = getNewAccesstokenBitbucket(usersAccountDetails.getRefreshToken(),
					CodeGripConstants.REFRESH_TOKEN);

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
			
				HttpEntity<?> httpEntity = new HttpEntity<>(headers);
				RestTemplate restTemplate = new RestTemplate();
				String url = "";
				url = environment.getProperty(CodeGripConstants.BITBUCKET_ADD_WEBHOOK);
				url = url.replace(ACCESS_TOKEN_KEY, newAccessToken.getString(CodeGripConstants.ACCESS_TOKEN))
						.replace(USER_NAME_KEY, usersAccountDetails.getAccountUsername())
						.replace("<REPO_SLUG>", project.getName());
				LOGGER.info("Body: " + httpEntity.getBody());
				LOGGER.info("Code url: " + url);
				String val = restTemplate.getForObject(url, String.class);
				JSONObject jsonObj = new JSONObject(val);
				JSONArray valueList = jsonObj.getJSONArray("values");
				LOGGER.info("Webook Data :" + valueList);
				for(int i =0;i<valueList.length();i++) {
					JSONObject valueObject = valueList.getJSONObject(i);
					if(environment.getProperty(CodeGripConstants.BITBUCKET_COMMIT_WEBHOOK).equals(valueObject.get("url"))) {
						String uid = (String) valueObject.get("uuid");
					    String uId =uid.replace("{", "");
						String uId1 = uId.replace("}", "");
						deleteWebhook(project.getName(),uId1,usersAccountDetails);
					}
					
				}
				getSshDeploy(project, usersAccountDetails);
				
		} catch (JSONException e) {
			LOGGER.error(e.getMessage());
		}
		return newAccessToken;
	}
	
	/*********************************************************************************************
	 * Delete webhook from bitbucket.
	 * @throws JSONException 
	 *********************************************************************************************/
	public void deleteWebhook(String projectName, String uid,UsersAccountDetails usersAccountDetails) throws JSONException {
		   JSONObject newAccessToken = getNewAccesstokenBitbucket(usersAccountDetails.getRefreshToken(),
				CodeGripConstants.REFRESH_TOKEN);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
		
			RestTemplate restTemplate = new RestTemplate();
			String url = "";
			url = environment.getProperty(CodeGripConstants.BITBUCKET_DELETE_WEBHOOK);
			url = url.replace(ACCESS_TOKEN_KEY, newAccessToken.getString(CodeGripConstants.ACCESS_TOKEN))
					.replace(USER_NAME_KEY, usersAccountDetails.getAccountUsername())
					.replace("<REPO_SLUG>", projectName)
					.replace("<UID>",uid);
			 restTemplate.delete(url);
			
	}
	
	/*********************************************************************************************
	 * get ssh from bitbucket for delete.
	 *********************************************************************************************/
	public JSONObject getSshDeploy(Projects project, UsersAccountDetails usersAccountDetails) {
		JSONObject newAccessToken = new JSONObject();
		try {
			newAccessToken = getNewAccesstokenBitbucket(usersAccountDetails.getRefreshToken(),
					CodeGripConstants.REFRESH_TOKEN);

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
			
				HttpEntity<?> httpEntity = new HttpEntity<>(headers);
				RestTemplate restTemplate = new RestTemplate();
				String url = "";
				url = environment.getProperty(CodeGripConstants.ADD_SSH_OVER_BITBUCLET);
				url = url.replace(ACCESS_TOKEN_KEY, newAccessToken.getString(CodeGripConstants.ACCESS_TOKEN))
						.replace(USER_NAME_KEY, usersAccountDetails.getAccountUsername())
						.replace("<REPO_SLUG>", project.getName());
				LOGGER.info("Body: " + httpEntity.getBody());
				LOGGER.info("Code url: " + url);
				String val = restTemplate.getForObject(url, String.class);
				JSONObject jsonObj = new JSONObject(val);
				JSONArray valueList = jsonObj.getJSONArray("values");
				for(int i=0;i<valueList.length();i++) {
					JSONObject valueObject = valueList.getJSONObject(i);
					if(environment.getProperty(CodeGripConstants.CG_SSH_TITLE).equals(valueObject.get("label"))) {
						Integer sshId = (Integer) valueObject.get("id");
						deleteSshFromBitbucket(project.getName(),sshId,usersAccountDetails);
					}
					
				}
				LOGGER.info("ssh Data :" + valueList);
		
				
		} catch (JSONException e) {
			LOGGER.error(e.getMessage());
		}
		return newAccessToken;
	}
	
	/*********************************************************************************************
	 * delete ssh key details from bitbucket.
	 *********************************************************************************************/
	public void deleteSshFromBitbucket(String projectName,Integer id, UsersAccountDetails usersAccountDetails){
		try {
			 JSONObject newAccessToken = getNewAccesstokenBitbucket(usersAccountDetails.getRefreshToken(),
						CodeGripConstants.REFRESH_TOKEN);
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_JSON);
					RestTemplate restTemplate = new RestTemplate();
					String url="";
					url = environment.getProperty(CodeGripConstants.BITBUCKET_DELETE_SSHKEY_URL);
					url = url.replace(USER_NAME_KEY, usersAccountDetails.getAccountUsername())
						.replace("<REPO_SLUG>", projectName);
					String deleteSshUrl = url+id+"/?access_token="+newAccessToken.getString(CodeGripConstants.ACCESS_TOKEN);
					 restTemplate.delete(deleteSshUrl);
		}catch(JSONException e) {
			LOGGER.error(e.getMessage());
		}
	}
}
