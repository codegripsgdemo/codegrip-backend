package com.mb.codegrip.service;

import static com.mb.codegrip.constants.CodeGripConstants.PROVIDER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dao.ProjectDAO;
import com.mb.codegrip.dao.QualityDAO;
import com.mb.codegrip.dao.SourceControlDAO;
import com.mb.codegrip.dao.UserDAO;
import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.ProjectBranch;
import com.mb.codegrip.dto.Projects;
import com.mb.codegrip.dto.Quality;
import com.mb.codegrip.dto.QualityCondition;
import com.mb.codegrip.dto.ShareDashboard;
import com.mb.codegrip.dto.UserProjects;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.dto.UsersAccountDetails;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.mail.MailerHelper;
import com.mb.codegrip.model.CommitModel;
import com.mb.codegrip.model.CompanySubscriptionModel;
import com.mb.codegrip.model.FilterModel;
import com.mb.codegrip.model.ProjectBranchModel;
import com.mb.codegrip.model.ProjectsModel;
import com.mb.codegrip.model.QualityConditionModel;
import com.mb.codegrip.model.QualityModel;
import com.mb.codegrip.model.ShareDashboardModel;
import com.mb.codegrip.model.ShareDashbordRequestModel;
import com.mb.codegrip.model.UserProjectsModel;
import com.mb.codegrip.repository.CompanyRepository;
import com.mb.codegrip.repository.ProjectsRepository;
import com.mb.codegrip.repository.QualityRepository;
import com.mb.codegrip.repository.ShareDashbordRepository;
import com.mb.codegrip.repository.UserProjectsRepository;
import com.mb.codegrip.repository.UserRepository;
import com.mb.codegrip.repository.UsersAccountDetailsRepository;
import com.mb.codegrip.utils.BitbucketAPIUtil;
import com.mb.codegrip.utils.CGScannerUtil;
import com.mb.codegrip.utils.CommonUtil;
import com.mb.codegrip.utils.CustomDozerHelper;
import com.mb.codegrip.utils.GitLabAPIUtil;
import com.mb.codegrip.utils.GithubAPIUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:exception.properties"), @PropertySource("classpath:notifications.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
@Service
public class ProjectServiceImpl implements ProjectService {

	private static final Logger LOGGER = Logger.getLogger(ProjectServiceImpl.class);

	@Autowired
	private UserDAO userDAO;

	@Autowired
	private SourceControlDAO sourceControlDAO;

	@Autowired
	private ProjectDAO projectDAO;

	@Autowired
	private QualityRepository qualityRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private SourceControlService sourceControlService;

	@Autowired
	private QualityDAO qualityDAO;

	@Autowired
	private UserService userService;

	@Autowired
	private ProjectsRepository projectsRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UsersAccountDetailsRepository usersAccountDetailsRepository;

	@Autowired
	private ShareDashbordRepository shareDashbordRepository;

	@Autowired
	private UserProjectsRepository userProjectsRepository;

	@Autowired
	private Environment environment;

	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	@Autowired
	private MailerHelper mailerHelper;

	Mapper mapper = new DozerBeanMapper();

	CommonUtil commonUtil = new CommonUtil();
	BitbucketAPIUtil bitbucketAPIUtil = new BitbucketAPIUtil();
	GithubAPIUtil githubAPIUtil = new GithubAPIUtil();
	GitLabAPIUtil gitlabAPIUtil = new GitLabAPIUtil();
	CGScannerUtil cgScannerUtil = new CGScannerUtil();

	/*************************************************************************************************
	 * call get API Get Quality get reports .
	 **************************************************************************************************/
	@Override
	public JSONObject getQualityGatesReport(FilterModel filterModel, String projectKey, String pageNo)
			throws IOException {
		String url = environment.getProperty(CodeGripConstants.GET_FILE_ISSUE_URL);
		url = url.replace("<PROJECT_KEY>", projectKey).replace("<PAGE_NO>", pageNo).replace(CodeGripConstants.PAGE_SIZE,
				CodeGripConstants.PAGE_SIZE_VALUE.toString());
		String types = (filterModel.getBugs() ? "BUG" : "") + (filterModel.getCodesmell() ? ",CODE_SMELL" : "")
				+ (filterModel.getVulnerability() ? ",VULNERABILITY" : "");
		String severities = (filterModel.getBlocker() ? "BLOCKER" : "") + (filterModel.getInfo() ? ",INFO" : "")
				+ (filterModel.getCritical() ? ",CRITICAL" : "") + (filterModel.getMajor() ? ",MAJOR" : "")
				+ (filterModel.getMinor() ? ",MINOR" : "");
		LOGGER.info("Types: " + types);
		LOGGER.info("Severities: " + severities);

		if (!"".equals(types))
			url = url.replace("<TYPES>", types.charAt(0) == ',' ? types.substring(1) : types);
		else
			url = url.replace("<TYPES>", "");
		if (!"".equals(severities))
			url = url.replace("<SEVERITIES>", severities.charAt(0) == ',' ? severities.substring(1) : severities);
		else
			url = url.replace("<SEVERITIES>", "");

		url = url + (filterModel.getEndDate() != null ? ("&createdBefore=" + filterModel.getEndDate().substring(0, 10))
				: "");
		url = url
				+ (filterModel.getStartDate() != null ? ("&createdAfter=" + filterModel.getStartDate().substring(0, 10))
						: "");
		LOGGER.info("Final filter url: " + url);
		return JSONObject.fromObject(callGetAPI(url).toString());
	}

	@Override
	public JSONObject getQualityGatesRule(String rule) throws IOException {
		String url = environment.getProperty(CodeGripConstants.ISSUE_SOLUTION_URL);
		url = url.replace(CodeGripConstants.RULE, rule);
		return JSONObject.fromObject(callGetAPI(url).toString());

	}

	/*************************************************************************************************
	 * call get API method.
	 **************************************************************************************************/
	@Override
	public StringBuilder callGetAPI(String url) throws IOException {
		String userLoginKey = environment.getProperty(CodeGripConstants.SONAR_LOGIN_KEY);
		String encodedBytes = Base64.getEncoder().encodeToString((userLoginKey + ":").getBytes());
		LOGGER.info("URL: " + url);
		HttpGet request = new HttpGet(url);
		HttpClient client = HttpClientBuilder.create().build();
		request.addHeader(CodeGripConstants.AUTHORIZATION, "Basic " + encodedBytes);
		HttpResponse response = client.execute(request);
		StringBuilder result = new StringBuilder();
		try (BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			LOGGER.info(result);
		} catch (Exception e) {
			LOGGER.error(e);
		}
		/** LOGGER.info("Data: " + result); */
		return result;
	}

	/*************************************************************************************************
	 * Get file code method.
	 **************************************************************************************************/
	@Override
	public JSONObject getFileCode(String projectKey, String startLine, String endLine) throws IOException {

		String url = "";
		url = environment.getProperty(CodeGripConstants.GET_FILE_LINES_URL);

		url = url.replace(CodeGripConstants.PROJECT_KEY, projectKey);

		if (startLine != null && endLine != null)
			url = url + "&from=" + startLine + "&to=" + endLine;
		if (url.contains(" "))
			url = url.replaceAll(" ", "%20");

		LOGGER.info("final URL:" + url);
		return JSONObject.fromObject(callGetAPI(url).toString());
	}

	/*************************************************************************************************
	 * Get issues method.
	 **************************************************************************************************/
	@Override
	public JSONArray getIssues(String projectKey) throws IOException {

		String url = "";
		LOGGER.info("<-----Get Isseses--->");
		url = environment.getProperty(CodeGripConstants.GET_FILE_ISSUE_URL_FULL);
		url = url.replace(CodeGripConstants.PROJECT_KEY, projectKey);
		/**
		 * if (startLine != null && endLine != null) url = url + "&startLine=" +
		 * startLine + "&endLine=" + endLine; if (url.contains(" ")) url =
		 * url.replaceAll(" ", "%20");
		 */
		JSONObject jsonObject = JSONObject.fromObject(callGetAPI(url).toString());
		return JSONArray.fromObject(jsonObject.get("issues"));
	}

	/*************************************************************************************************
	 * Get issue list API.
	 **************************************************************************************************/
	@Override
	public JSONObject getIssueList(String projectName, int pageNo, String owaspTop) throws IOException {
		String url = "";
		url = environment.getProperty(CodeGripConstants.SONAR_GET_ISSUE_LIST_URL);
		url = url.replace(CodeGripConstants.PROJECT_NAME, projectName).replace(CodeGripConstants.PAGE_SIZE,
				CodeGripConstants.PAGE_SIZE_VALUE.toString());
		url = url + pageNo;
		if (owaspTop != null) {
			url = url + "&" + owaspTop + "&types=VULNERABILITY";
			LOGGER.info("FINAL URL With OWASP--------------------->" + url);
		}
		return JSONObject.fromObject(callGetAPI(url).toString());
	}

	/*************************************************************************************************
	 * Get email and companyId method.
	 **************************************************************************************************/
	@Override
	public Users getEmailAndCompanyId(String email) {
		return userDAO.getByEmailAndCompanyId(email);
	}

	/*************************************************************************************************
	 * Save codegrip Quality report.
	 * 
	 * @throws ParseException
	 **************************************************************************************************/
	@SuppressWarnings("rawtypes")
	@Override
	public void saveCodeGripQuality(Object object, HttpServletRequest request) throws CustomException, ParseException {
		try {

			JSONObject webhookObject = new JSONObject();
			if (object != null) {
				if (object instanceof Map) {
					webhookObject.putAll((Map) object);
					LOGGER.info(webhookObject.toString());
				}
				JSONObject projectObj = webhookObject.getJSONObject("project");
				JSONObject properties = webhookObject.getJSONObject("properties");
				/** JSONObject branchObj = webhookObject.getJSONObject("branch"); */
				String key = projectObj.getString("key");

				String[] branchNameArr = key.split(":");
				String[] bName = branchNameArr[1].split(CodeGripConstants.SEPERATOR);

				String projectName = projectObj.getString("name");

				LOGGER.info("Final project name from sonar webhook: " + projectName);
				String[] finalProjectName = projectName.split(CodeGripConstants.SEPERATOR);
				String url = projectObj.getString("url");

				/**
				 * if (branchObj.getString("name") != null) branchName =
				 * branchObj.getString("name"); else{ branchName = CodeGripConstants.MASTER;
				 */
				String[] branchName = key.split(CodeGripConstants.SEPERATOR);
				if (branchName.length >= 3) {
					key = branchName[0] + CodeGripConstants.SEPERATOR + branchName[1] + CodeGripConstants.SEPERATOR
							+ branchName[2];
				}

				LOGGER.info("Final branch name from sonar webhook: " + key);
				ProjectBranch branch = projectDAO.getBranchDetails(key, bName[0]);

				Quality quality = createQualityJSON(webhookObject, key);
				quality.setProjectKey(key);
				quality.setIsDeleted(false);
				quality.setProjectName(finalProjectName[0]);
				quality.setQualityDetailsUrl(url);
				quality.setAnalyzeAt(new Timestamp(System.currentTimeMillis()));

				// Get commit id from project key and set to quality
				/**
				 * String[] commitId = key.split(CodeGripConstants.SEPERATOR); if
				 * (commitId.length > 1) quality.setCommitId(commitId[1]);
				 */

				List<QualityCondition> qualityConditionDTOs = new ArrayList<>();
				for (QualityCondition qualityConditionDTO : quality.getQualityConditions()) {
					if (qualityConditionDTO != null) {
						qualityConditionDTO.setQuality(quality);
						qualityConditionDTOs.add(qualityConditionDTO);
					}
				}

				quality.getQualityConditions().remove(null);
				if (branch != null) {
					Quality dbQuality = new Quality();
					try {
						if (!properties.getString(CodeGripConstants.SONAR_BUILD_NUMBER_KEY).equals("0")) {
							LOGGER.info(
									"Quality id: " + properties.getString(CodeGripConstants.SONAR_BUILD_NUMBER_KEY));
							dbQuality = qualityRepository.findById(
									Integer.parseInt(properties.getString(CodeGripConstants.SONAR_BUILD_NUMBER_KEY)));
						}
					} catch (Exception e) {
						LOGGER.info(e.getMessage());
					}
					quality.setProjectBranchId(branch.getId());
					if (dbQuality.getId() != null) {
						quality.setId(dbQuality.getId());
						quality.setCommitId(dbQuality.getCommitId());
					}
					qualityRepository.save(quality);
				}
				/**
				 * List<Users> userDTOs = userDAO.getAdminList(key); Projects projectsModel =
				 * userDAO.getRepositoryDetails(key); List<UsersModel> userModels =
				 * CustomDozerHelper.map(mapper, userDTOs, UsersModel.class);
				 * 
				 * if (projectsModel!=null && projectsModel.getIsSlackNotified()) {
				 * LOGGER.info("In send webhook slack notification.");
				 * sendWebhookSlackNotification(projectsModel,
				 * environment.getProperty(CodeGripConstants.PROJECT_DASHBOARD_URL),
				 * qualityConditionDTOs); }
				 * 
				 * if (projectsModel!=null && projectsModel.getIsEmailNotified()) {
				 * sendWebhookMail(userModels,
				 * environment.getProperty(CodeGripConstants.PROJECT_DASHBOARD_URL),
				 * qualityConditionDTOs, projectsModel); LOGGER.info("In send webhook email
				 * notification."); }
				 */
			} else {
				LOGGER.error("webhook object is null");
			}
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/*************************************************************************************************
	 * Create quality JSON method.
	 **************************************************************************************************/
	private Quality createQualityJSON(JSONObject webhookObject, String key) throws CustomException {

		try {

			com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
			objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

			Quality quality = objectMapper.readValue(webhookObject.toString(), Quality.class);

			/**
			 * JSONObject qualityGate = webhookObject.getJSONObject("qualityGate");
			 * JSONArray conditionsArray = qualityGate.getJSONArray("conditions");
			 */

			JSONObject qualityConditionDTO = createQualityConditions(key);
			JSONObject component = qualityConditionDTO.getJSONObject("component");
			JSONArray measures = component.getJSONArray("measures");

			Map<String, Object> conditionMap = new HashMap<>();
			List<QualityCondition> conditionsList = new ArrayList<>();
			List<String> requiredDataList = Arrays
					.asList(environment.getProperty(CodeGripConstants.MEASURE_LIST).split(","));
			LOGGER.info("measures.size : " + measures.size());
			LOGGER.info("measure list: " + measures);
			for (int i = 0; i < measures.size(); i++) {
				LOGGER.info("measure: " + measures.getJSONObject(i));
				JSONObject jsonobject = measures.getJSONObject(i);

				if (!jsonobject.get("metric").equals("quality_gate_details")) {
					QualityCondition conditionDTO = objectMapper.readValue(jsonobject.toString(),
							QualityCondition.class);
					if (requiredDataList.contains(conditionDTO.getMetric())) {
						conditionMap.put(conditionDTO.getMetric(), conditionDTO);
					}
				}

				QualityCondition qualityCondition = createNewQualityObject(jsonobject);
				if (qualityCondition.getMetric() != null)
					conditionsList.add(qualityCondition);

			}
			List<String> qualityRates = Arrays
					.asList(environment.getProperty(CodeGripConstants.QUALITY_RATES).split(","));
			QualityCondition bugs = (QualityCondition) conditionMap.get("bugs");
			if (bugs.getMetric().equals("bugs")) {
				QualityCondition reliabilityRating = (QualityCondition) conditionMap.get("reliability_rating");
				LOGGER.info("Rating: "
						+ qualityRates.get((int) Math.round((Double.parseDouble(reliabilityRating.getValue()))) - 1));
				bugs.setRating(
						qualityRates.get((int) Math.round((Double.parseDouble(reliabilityRating.getValue()))) - 1));
			}
			QualityCondition vulnerabilities = (QualityCondition) conditionMap.get(CodeGripConstants.VULNERABILITIES);
			if (vulnerabilities.getMetric().equals(CodeGripConstants.VULNERABILITIES)) {
				QualityCondition securityRating = (QualityCondition) conditionMap.get("security_rating");
				vulnerabilities.setRating(
						qualityRates.get((int) Math.round((Double.parseDouble(securityRating.getValue()))) - 1));
			}
			QualityCondition codeSmells = (QualityCondition) conditionMap.get(CodeGripConstants.CODE_SMELLS);
			if (codeSmells.getMetric().equals("code_smells")) {
				QualityCondition reliabilityRating = (QualityCondition) conditionMap.get("reliability_rating");
				codeSmells.setRating(
						qualityRates.get((int) Math.round((Double.parseDouble(reliabilityRating.getValue()))) - 1));
			}

			conditionsList.add(codeSmells);
			conditionsList.add(bugs);
			conditionsList.add(vulnerabilities);
			conditionsList.add((QualityCondition) conditionMap.get("coverage"));
			conditionsList.add((QualityCondition) conditionMap.get("alert_status"));
			conditionsList.add((QualityCondition) conditionMap.get("ncloc_language_distribution"));
			conditionsList.add((QualityCondition) conditionMap.get("duplicated_lines_density"));

			quality.setQualityConditions(conditionsList);
			return quality;
		} catch (Exception exception) {
			LOGGER.error(exception.getMessage(), exception);
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/*************************************************************************************************
	 * Create quality condition object.
	 **************************************************************************************************/
	private QualityCondition createNewQualityObject(JSONObject jsonobject) {
		QualityCondition qualityCondition = new QualityCondition();

		if (jsonobject.has("periods")) {
			JSONArray jsonArray = jsonobject.getJSONArray("periods");
			JSONObject value = jsonArray.getJSONObject(0);

			if (jsonobject.has("new_coverage")) {
				qualityCondition.setMetric(jsonobject.getString("new_coverage"));
				qualityCondition.setValue(value.getString("value"));
			}
			if (jsonobject.has("new_technical_debt")) {
				qualityCondition.setMetric(jsonobject.getString("new_technical_debt"));
				qualityCondition.setValue(value.getString("value"));
			}
			if (jsonobject.has("new_security_rating")) {
				qualityCondition.setMetric(jsonobject.getString("new_security_rating"));
				qualityCondition.setValue(value.getString("value"));
			}
			if (jsonobject.has("new_duplicated_lines_density")) {
				qualityCondition.setMetric(jsonobject.getString("new_duplicated_lines_density"));
				qualityCondition.setValue(value.getString("value"));
			}
			if (jsonobject.has("new_maintainability_rating")) {
				qualityCondition.setMetric(jsonobject.getString("new_maintainability_rating"));
				qualityCondition.setValue(value.getString("value"));
			}
			if (jsonobject.has("new_reliability_rating")) {
				qualityCondition.setMetric(jsonobject.getString("new_reliability_rating"));
				qualityCondition.setValue(value.getString("value"));
			}
			if (jsonobject.has("new_vulnerabilities")) {
				qualityCondition.setMetric(jsonobject.getString("new_vulnerabilities"));
				qualityCondition.setValue(value.getString("value"));
			}
			if (jsonobject.has("new_lines_to_cover")) {
				qualityCondition.setMetric(jsonobject.getString("new_lines_to_cover"));
				qualityCondition.setValue(value.getString("value"));
			}
			if (jsonobject.has("new_bugs")) {
				qualityCondition.setMetric(jsonobject.getString("new_bugs"));
				qualityCondition.setValue(value.getString("value"));
			}
		}
		return qualityCondition;
	}

	/*************************************************************************************************
	 * Get quality gates from sonar
	 **************************************************************************************************/
	@Override
	public JSONObject createQualityConditions(String key) throws CustomException {
		String url = environment.getProperty(CodeGripConstants.SONAR_QUALITY_GATES);
		String userLoginKey = environment.getProperty(CodeGripConstants.SONAR_LOGIN_KEY);
		String encodedBytes = Base64.getEncoder().encodeToString((userLoginKey + ":").getBytes());
		url = url.replace("<KEY>", key);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Basic " + encodedBytes);
		LOGGER.info("URL: " + url);
		LOGGER.info("token: " + encodedBytes);
		String response = "";
		try {
			response = commonUtil.callRestPostApi(headers, url);
		} catch (Exception e) {
			LOGGER.error("Project not found on sonar.");
		}
		if (response != "")
			return JSONObject.fromObject(response);
		else
			return null;
	}

	/********************************************************************************************************
	 * Start project analysing service layer.
	 * 
	 * @throws JSONException
	 * 
	 * @throws Exception
	 * 
	 * @throws IOException
	 *********************************************************************************************************/
	@Override
	public ProjectsModel startAnalyzeProject(ProjectsModel projectsModel, HttpServletRequest request)
			throws CustomException, JSONException {

		// check if user have startup plan and reached the limit.
		CompanySubscriptionModel companySubscriptionModel = sourceControlService
				.getSubscriptionAndProjectDetails(projectsModel.getCompanyId());
		if (companySubscriptionModel.getRemainingProjects() != null
				&& companySubscriptionModel.getRemainingProjects() < 1) {
			LOGGER.error(environment.getProperty(CodeGripConstants.PROJECT_LIMIT_REACHED));
			throw new CustomException(environment.getProperty(CodeGripConstants.PROJECT_LIMIT_REACHED));
		}

		org.json.JSONObject accessTokens = new org.json.JSONObject();
		boolean flag = false;
		/**
		 * if (principal != null) { users =
		 * userDAO.getByEmailAndCompanyId(principal.getName());
		 * projects.setCompanyId(users.getOwnerCompanyId());
		 * projects.setAnalyzeStartTime(commonUtil.getCurrentTimeStampInString());
		 * projects.setIsAnalyzeStarted(true);
		 * projects.setCreatedDate(commonUtil.getCurrentTimeStampInString());
		 * projects.setUserId(users.getId()); LOGGER.info("Username: " +
		 * users.getEmail()); users.setIsFreeTrialStarted(true);
		 */
		if ("giturl".equals(projectsModel.getProvider())) {
			// create project data from URL.
			projectsModel = createProjectDataFromGitURL(projectsModel);
			accessTokens.put(PROVIDER, "giturl");
		}

		// Project name to convert to slug name.
		if("bitbucket".equalsIgnoreCase(projectsModel.getProvider()) || "github".equalsIgnoreCase(projectsModel.getProvider()))
			projectsModel.setName(createProjectNameFromURL(projectsModel.getGitCloneUrl()));
		
		/**String projectName = projectsModel.getName();
		projectName = projectName.toLowerCase().replace(" ", "-");
		projectsModel.setName(projectName);*/

		Projects projects = mapper.map(projectsModel, Projects.class);

		Projects alreadyExistedProject = projectsRepository.findByNameAndCompanyId(projects.getName(),
				projects.getCompanyId());

		UsersAccountDetails usersAccountDetails = usersAccountDetailsRepository
				.findByUserIdAndSourceControlNameAndAccountUsernameAndCompanyId(projectsModel.getUserId(),
						projectsModel.getProvider(), projectsModel.getUserName(), projectsModel.getCompanyId());
		try {
			if (CodeGripConstants.BITBUCKET_PROVIDER.equals(projectsModel.getProvider())) {
				bitbucketAPIUtil.addWebhookToBitbucket(projectsModel, usersAccountDetails,
						alreadyExistedProject != null ? alreadyExistedProject.getIsWebhookAdded() : flag);
				accessTokens = bitbucketAPIUtil.addSSHOverBitbucket(projectsModel, usersAccountDetails,
						alreadyExistedProject != null ? alreadyExistedProject.getIsSSHAdded() : flag);
				accessTokens.put(PROVIDER, CodeGripConstants.BITBUCKET_PROVIDER);
				projects.setIsSSHAdded(true);
			} else if (CodeGripConstants.GITHUB_PROVIDER.equals(projectsModel.getProvider())) {
				try {
					accessTokens = githubAPIUtil.addWebhookToGithub(projectsModel, usersAccountDetails,
							alreadyExistedProject != null ? alreadyExistedProject.getIsWebhookAdded() : flag);
				} catch (Exception e) {
					LOGGER.info(e.getMessage());
				}
				accessTokens.put(PROVIDER, CodeGripConstants.GITHUB_PROVIDER);
			} else if (CodeGripConstants.GITLAB_PROVIDER.equals(projectsModel.getProvider())) {
				gitlabAPIUtil.addWebhookToGitlab(projectsModel, usersAccountDetails,
						alreadyExistedProject != null ? alreadyExistedProject.getIsWebhookAdded() : flag);
				accessTokens = gitlabAPIUtil.addSSHOverGitlab(projectsModel, usersAccountDetails,
						alreadyExistedProject != null ? alreadyExistedProject.getIsSSHAdded() : flag);
				accessTokens.put(CodeGripConstants.ACCESS_TOKEN, usersAccountDetails.getAccessToken());
				accessTokens.put(PROVIDER, CodeGripConstants.GITLAB_PROVIDER);
				projects.setIsSSHAdded(true);
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		if (alreadyExistedProject != null) {
			projects.setId(alreadyExistedProject.getId());
			projects.setIsWebhookAdded(alreadyExistedProject.getIsWebhookAdded());
		} else {
			projects.setIsWebhookAdded(true);
			projects.setIsSSHAdded(true);
		}

		String projectKey = projectsModel.getName() + ":master" + CodeGripConstants.SEPERATOR + timestamp.getTime();
		if (projects.getProjectBranches() == null || projects.getProjectBranches().isEmpty()) {
			ProjectBranch projectBranch = new ProjectBranch();
			List<ProjectBranch> projectBranches = new ArrayList<>();
			projects.setProjectBranches(projectBranches);
			if (alreadyExistedProject != null) {
				for (ProjectBranch projectBranch1 : alreadyExistedProject.getProjectBranches()) {
					if (projectBranch1.getBranchKey().equals(CodeGripConstants.MASTER))
						projectBranch.setId(projectBranch1.getId());
				}
			}
			projectBranch.setBranchKey("master");
			projectBranch.setName(projectKey);
			projectBranch.setProjects(projects);
			projectBranch.setIsDeleted(false);
			projects.addProjectBranch(projectBranch);
		}
		projects.setGitCloneUrl(projectsModel.getGitCloneUrl());
		projects.setIsAnalyzeStarted(true);
		projects.setAnalyzeStartTime(new Timestamp(System.currentTimeMillis()).toString());
		projects.setCreatedDate(new Timestamp(System.currentTimeMillis()).toString());
		projects.setIsDeleted(false);
		Projects savedProject = projectDAO.saveProjectsRecord(projects);

		startProjectScanning(projectsModel.getGitCloneUrl(), projectsModel.getName(), accessTokens, projectKey,
				"master", 0, mapper.map(savedProject, ProjectsModel.class));
		return mapper.map(projects, ProjectsModel.class);

	}

	/*********************************************************************************************
	 * Create project name from gitURL.
	 *********************************************************************************************/
	private String createProjectNameFromURL(String gitCloneUrl) {
		String[] projectUrlSplit = gitCloneUrl.split("/");
		String projectName = projectUrlSplit[projectUrlSplit.length - 1];
		return projectName.substring(0, projectName.length() - 4);
	}

	/*********************************************************************************************
	 * Create project from gitURL.
	 *********************************************************************************************/
	private ProjectsModel createProjectDataFromGitURL(ProjectsModel projectsModel) {
		// https://github.com/deepak-kumbhar/springboot-oauth2-spring-security-mysql.git
		ProjectsModel newProjectsModel = new ProjectsModel();
		String[] projectUrlSplit = projectsModel.getGitCloneUrl().split("/");
		String projectName = projectUrlSplit[projectUrlSplit.length - 1];
		newProjectsModel.setName(projectName.substring(0, projectName.length() - 4));
		newProjectsModel.setUserName(projectUrlSplit[3]);
		newProjectsModel.setGitCloneUrl(projectsModel.getGitCloneUrl());
		newProjectsModel.setCompanyId(projectsModel.getCompanyId());
		newProjectsModel.setUserId(projectsModel.getUserId());
		newProjectsModel.setProvider("giturl");
		return newProjectsModel;
	}

	/******************************************************************************************************
	 * Start project scanning.
	 * 
	 * @param qualityId
	 * @param projectsModel
	 ******************************************************************************************************/
	public void startProjectScanning(String gitUrl, String projectName, org.json.JSONObject accessTokens,
			String projectKey, String branchName, Integer qualityId, ProjectsModel projectsModel) {
		ExecutorService scannerExecutor = Executors.newSingleThreadExecutor();
		scannerExecutor.execute(() -> {
			try {
				String warningErrorLog = cgScannerUtil.startCGScan(gitUrl, projectName, accessTokens, projectKey,
						branchName, qualityId, projectsModel);
				scannerExecutor.shutdown();

				// send error warning email.

				if (warningErrorLog != "") {
					if (warningErrorLog.contains("WARN")) {
						updateStatusAndSendMailToAdmin(projectsModel, warningErrorLog, "Warning while Analyzing");
					} else if (warningErrorLog.contains("ERROR") && warningErrorLog.contains("Caused by")) {
						String newStr = warningErrorLog.substring(warningErrorLog.indexOf("Caused by:"),
								warningErrorLog.indexOf('\n', warningErrorLog.indexOf("Caused by:")));
						updateStatusAndSendMailToAdmin(projectsModel, newStr, "Analyzing Failed");
					} else if (warningErrorLog.contains("EXECUTION FAILURE")) {
						updateStatusAndSendMailToAdmin(projectsModel, "EXECUTION FAILURE", "Analyzing Failed");
					} else {
						String emailString = environment.getProperty(CodeGripConstants.SEND_ERROR_WARNING_MAIL);
						String[] emails = emailString.split(",");
						for (String email : emails) {
							sendProjectAnalysisMail(email, projectName, warningErrorLog);
						}
					}
				}
			} catch (IOException | JSONException | CustomException e) {
				LOGGER.info(e.getMessage());
			}
		});

	}

	/**********************************************************************************************************************
	 * Send email public method.
	 **********************************************************************************************************************/
	@Override
	public void sendProjectAnalysisMail(String email, String projectName, String errorMessage) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor.execute(() -> mailerHelper.sendProjectScannedMail(email,
				environment.getProperty(CodeGripConstants.ANALYSIS_SUBJECT), projectName + " " + errorMessage));
		emailExecutor.shutdown();
	}

	/*********************************************************************************************************************
	 * Create dynamic token function.
	 *********************************************************************************************************************/
	/**
	 * private Projects createDynamicToken(Projects projects) throws CustomException
	 * {
	 * 
	 * try { String userLoginKey =
	 * environment.getProperty(CodeGripConstants.BITBUCKET_KEY); String encodedBytes
	 * = Base64.getEncoder().encodeToString((userLoginKey + ":").getBytes());
	 * 
	 * HttpHeaders headers = new HttpHeaders();
	 * headers.setContentType(MediaType.APPLICATION_JSON);
	 * headers.set("Authorization", "Basic " + encodedBytes);
	 * 
	 * String url =
	 * environment.getProperty(CodeGripConstants.SONAR_DYNAMIC_TOKEN_CREATION_URL);
	 * url = url.concat(projects.getName() + "+" + projects.getUserName());
	 * LOGGER.info("url :" + url); String result =
	 * commonUtil.callRestPostApi(headers, url); LOGGER.info("result :" + result);
	 * org.json.JSONObject resultJson = new org.json.JSONObject(result); String
	 * dynamicToken = resultJson.getString("token");
	 * projects.setSonarDyanmicToken(dynamicToken); return projects;
	 * 
	 * } catch (Exception exception) { LOGGER.info(exception); throw new
	 * CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
	 * }
	 * 
	 * }
	 */
	/*********************************************************************************************************************
	 * Create method to get code List return Json Object
	 *********************************************************************************************************************/
	@Override
	public JSONObject getCodeList(String projectDir) throws IOException {

		String url = environment.getProperty(CodeGripConstants.GET_CODE_LIST_URL);
		projectDir = projectDir.replace(" ", "%20");
		url = url.replace(CodeGripConstants.PROJECT_DIR, projectDir).replace(CodeGripConstants.PAGE_SIZE,
				CodeGripConstants.PAGE_SIZE_VALUE.toString());

		LOGGER.info("URL to get code List----------------- " + url);

		return JSONObject.fromObject(callGetAPI(url).toString());
	}

	/*************************************************************************************************
	 * Get progress of projects.
	 **************************************************************************************************/
	@Override
	public Map<String, Object> getProgressOfProject(String projectKey, HttpServletRequest request, Integer userId,
			Integer companyId, String role) throws CustomException, JSONException {
		Map<String, Object> progressResult = new HashMap<>();
		String[] projectBranchKey = projectKey.split(":");

		// get user details using access_token.
		Projects projects = projectsRepository.findByNameAndCompanyId(projectBranchKey[0], companyId);
		ProjectsModel projectsModel = mapper.map(projects, ProjectsModel.class);
		LOGGER.info("UserID :" + projects.getUserId());
		LOGGER.info("Provider :" + projects.getProvider());
		UsersAccountDetails usersAccountDetails = usersAccountDetailsRepository
				.findByUserIdAndSourceControlNameAndAccountUsernameAndCompanyId(projects.getUserId(),
						projects.getProvider(), projects.getUserName(), companyId);
		List<CommitModel> recentCommit = new ArrayList<>();
		try {
			if ("bitbucket".equals(projects.getProvider()))
				recentCommit = bitbucketAPIUtil.getCommitsFromBitbucket(projectBranchKey[0], usersAccountDetails);
			else if ("github".equals(projects.getProvider())) {
				recentCommit = githubAPIUtil.getCommitsFromGithub(projectBranchKey[0], usersAccountDetails);
			} else if ("gitlab".equals(projects.getProvider())) {
				recentCommit = gitlabAPIUtil.getCommitsFromGitlab(projects.getUid(), usersAccountDetails);
			}
		} catch (ParseException e) {
			LOGGER.error(e.getMessage());
		}
		progressResult.put("recentCommits", recentCommit);
		LOGGER.info("recent commit success.");
		LOGGER.info(projectBranchKey[1]);
		LOGGER.info(projectsModel.getProjectBranches().size());

		List<Integer> ids = new ArrayList<>();
		for (ProjectBranchModel projectBranchModel : projectsModel.getProjectBranches()) {
			ids.add(projectBranchModel.getId());
		}
		List<Quality> qualities = qualityRepository.findByProjectBranchIdIn(ids);
		if (!qualities.isEmpty()) {
			List<CommitModel> commitModels = filterProgressData(
					CustomDozerHelper.map(mapper, qualities, QualityModel.class), projectBranchKey[1],
					projectsModel.getProjectBranches());
			LOGGER.info("commit" + commitModels.size());
			progressResult.put(CodeGripConstants.COMMIT_DETAILS, sortProgressDataFinalMethod(commitModels));

			if (!recentCommit.isEmpty())
				qualityDAO.updateCommitId(qualities.get(0), recentCommit.get(0));
		} else {
			progressResult.put(CodeGripConstants.COMMIT_DETAILS, null);
		}

		return progressResult;
	}

	/*************************************************************************************************
	 * Filter progress data. Only Quality report.
	 * 
	 * @param projectBranchModel
	 **************************************************************************************************/
	private List<CommitModel> filterProgressData(List<QualityModel> list, String projectBranchKey,
			List<ProjectBranchModel> projectBranchModels) {
		List<CommitModel> commitModels = new ArrayList<>();
		list.sort(Comparator.comparing(QualityModel::getAnalyzeAt).reversed());

		for (ProjectBranchModel projectBranchModel : projectBranchModels) {
			LOGGER.info("B-Id: " + projectBranchModel.getId());
			LOGGER.info("Quality: " + projectBranchModel.getId());
			String[] key = projectBranchModel.getName().split(":");
			if (key.length == 2) {
				if (key[1].equals(projectBranchKey))
					commitModels = filterProgressFunction(list);
			} else if (key[0].equals(projectBranchKey)) {
				commitModels = filterProgressFunction(list);
			}

		}
		return commitModels;
	}

	/*************************************************************************************************
	 * Sorting final filter data for progress report.
	 **************************************************************************************************/
	private List<CommitModel> sortProgressDataFinalMethod(List<CommitModel> filterProgressFunction) {
		filterProgressFunction.sort(Comparator.comparing(CommitModel::getCommitDate).reversed());
		List<CommitModel> finalCommitModels = new ArrayList<>();
		CommitModel commitModel = new CommitModel();
		if (filterProgressFunction.size() > 1) {
			int fixedIssues = Integer.parseInt(filterProgressFunction.get(0).getFixedIssues())
					- Integer.parseInt(filterProgressFunction.get(1).getFixedIssues());
			commitModel.setFixedIssues(fixedIssues >= 0
					? Integer.toString(fixedIssues) + " out of " + filterProgressFunction.get(1).getFixedIssues()
					: " 0 out of " + filterProgressFunction.get(1).getFixedIssues());
			commitModel.setNewIssues(filterProgressFunction.get(0).getNewIssues());
			DecimalFormat df = new DecimalFormat("###.##");
			Double firstVal = Double.parseDouble(filterProgressFunction.get(0).getDuplication());
			Double secVal = Double.parseDouble(filterProgressFunction.get(1).getDuplication());
			Double duplication = firstVal - secVal;
			commitModel.setDuplication(duplication >= 0.0
					? df.format(duplication) + "% out of " + filterProgressFunction.get(1).getDuplication() + "%"
					: "0% out of " + filterProgressFunction.get(1).getDuplication() + "%");
			commitModel.setSecurity(filterProgressFunction.get(0).getSecurity());
			finalCommitModels.add(commitModel);
		} else if (filterProgressFunction.size() == 1) {
			commitModel.setFixedIssues(" 0 out of " + filterProgressFunction.get(0).getFixedIssues());
			commitModel.setNewIssues(filterProgressFunction.get(0).getNewIssues());
			commitModel.setDuplication("0% out of " + filterProgressFunction.get(0).getDuplication() + "%");
			commitModel.setSecurity(filterProgressFunction.get(0).getSecurity());
			finalCommitModels.add(commitModel);
		}

		return finalCommitModels;

	}

	/*************************************************************************************************
	 * filter progress function for calculating progress board data.
	 **************************************************************************************************/
	private List<CommitModel> filterProgressFunction(List<QualityModel> projectBranchModel) {
		List<CommitModel> commitModels = new ArrayList<>();
		int totalIssues = 0;
		String duplication = "";
		int security = 0;
		for (QualityModel qualityModel : projectBranchModel) {
			CommitModel commitModel1 = new CommitModel();
			for (QualityConditionModel qualityConditionModel : qualityModel.getQualityConditions()) {

				totalIssues = (qualityConditionModel.getMetric().equals("code_smells")
						|| qualityConditionModel.getMetric().equals("bugs")
								? (Integer.parseInt(qualityConditionModel.getValue()) + totalIssues)
								: totalIssues);
				duplication = (qualityConditionModel.getMetric().equals("duplicated_lines_density")
						? qualityConditionModel.getValue()
						: duplication);
				security = (qualityConditionModel.getMetric().equals("vulnerabilities")
						? Integer.parseInt(qualityConditionModel.getValue())
						: security);
			}
			commitModel1.setFixedIssues(Integer.toString(totalIssues));
			commitModel1.setNewIssues(Integer.toString(totalIssues));
			commitModel1.setDuplication(duplication);
			commitModel1.setSecurity(Integer.toString(security));
			commitModel1.setCommitDate(qualityModel.getAnalyzeAt());
			LOGGER.info("totalIssue: " + totalIssues);
			LOGGER.info("security: " + security);
			totalIssues = 0;
			security = 0;
			commitModels.add(commitModel1);
		}
		return commitModels;
	}

	/*************************************************************************************************
	 * Get duplicated code.
	 **************************************************************************************************/
	@Override
	public JSONObject getDuplicateCode(String projectKey) throws IOException {

		String url = "";
		url = environment.getProperty(CodeGripConstants.GET_DUPLICATION_URL);

		url = url.replace(CodeGripConstants.PROJECT_KEY, projectKey);

		LOGGER.info("URL to get Dublicate code----------------- " + url);

		return JSONObject.fromObject(callGetAPI(url).toString());
	}

	/*************************************************************************************************
	 * Get commit details from database.
	 **************************************************************************************************/
	@Override
	public Map<String, Object> getCommitDetails(String commitHashTop, String commitHashBottom,
			HttpServletRequest request) {
		Map<String, Object> finalResult = new HashMap<>();
		List<String> commitHash = new ArrayList<>();

		commitHash.add(commitHashTop);
		commitHash.add(commitHashBottom);
		List<QualityModel> projects = CustomDozerHelper.map(mapper, qualityRepository.findByCommitIdIn(commitHash),
				QualityModel.class);
		if (!projects.isEmpty())
			finalResult.put(CodeGripConstants.COMMIT_DETAILS,
					sortProgressDataFinalMethod(filterProgressFunction(projects)));
		else
			finalResult.put(CodeGripConstants.COMMIT_DETAILS,
					environment.getProperty(CodeGripConstants.NO_COMMIT_DETAILS_FOUND));
		return finalResult;
	}

	/***********************************************************************************************
	 * Find by email in company id
	 * 
	 * @throws CustomException
	 ************************************************************************************************/
	@Override
	public List<Object[]> getByEmailId(String emailId, HttpServletRequest request, Integer companyId, Integer userId)
			throws CustomException {
		Company company = companyRepository.findById(companyId);
		return userService.getEmailAndId(emailId, company, userId);
	}

	/***********************************************************************************************
	 * Send project.
	 * 
	 * @throws CustomException
	 **************************************************************************************************/
	@Override
	public String sendProjectService(ShareDashboardModel shareDashboardModel, HttpServletRequest request)
			throws CustomException {

		Optional<Users> users = userRepository.findById(shareDashboardModel.getSenderId());
		String name = "";
		if (users.isPresent()) {
			name = users.get().getName() != null ? users.get().getName() : users.get().getEmail();
		}

		Date today = new Date();
		Date newDate = DateUtils.addHours(today, 24);
		Timestamp currentDate = new Timestamp(today.getTime());
		Timestamp expiresIn = new Timestamp(newDate.getTime());
		LOGGER.info("expires in :" + expiresIn.getTime());

		for (ShareDashbordRequestModel shareDashboardModels : shareDashboardModel.getShareDashboardRequestModel()) {
			ShareDashboard entity = new ShareDashboard();
			entity.setSenderId(shareDashboardModel.getSenderId());
			entity.setReceiverId(shareDashboardModels.getReceiverId());
			entity.setReceiverMailId(shareDashboardModels.getReceiverMailId());
			entity.setProjectId(shareDashboardModel.getProjectId());
			entity.setSharedDate(currentDate);
			entity.setIsDeleted(false);
			entity.setExpiresIn(expiresIn);
			entity.setMessage(shareDashboardModel.getMessage());

			String token = generateToken(shareDashboardModels.getReceiverMailId(),
					shareDashboardModel.getProjectId().toString());
			entity.setSharedToken(token);

			shareDashboardModels.setProjectName(projectsRepository.findNameById(shareDashboardModel.getProjectId()));

			LOGGER.info("project Name" + shareDashboardModels.getProjectName());
			LOGGER.info("sender Id->" + entity.getSenderId());
			LOGGER.info("Receiver Mail id-->" + shareDashboardModels.getReceiverMailId());
			LOGGER.info("Share Dashbord model-->" + shareDashboardModel.getDashboardImage());
			String dashboardUrl = sendTokenToMail(shareDashboardModels.getReceiverMailId(),
					shareDashboardModels.getProjectName(), token, shareDashboardModel.getDashboardImage(),
					shareDashboardModel.getMessage(), name);

			if (shareDashboardModels.getReceiverId() != null) {
				String message = name + " " + environment.getProperty(CodeGripConstants.ADMIN_SHARED_DASHBOARD);
				message = message.replace("<PROJECT_NAME>", shareDashboardModels.getProjectName());
				userService.saveNotification(message, CodeGripConstants.UNREAD,
						environment.getProperty(CodeGripConstants.ASSIGNED_TO_NEW_PROJECT),
						environment.getProperty(CodeGripConstants.ASSIGNED_TO_NEW_PROJECT),
						environment.getProperty(CodeGripConstants.ASSIGN_PROJECT_DESTINATION_PAGE),
						shareDashboardModel.getCompanyId(), shareDashboardModel.getReceiverId(),
						shareDashboardModel.getProjectId(), CodeGripConstants.IMAGE_SHARE_DASHBOARD, dashboardUrl,
						true);
			}

			shareDashbordRepository.save(entity);

		}
		return environment.getProperty(CodeGripConstants.TOKEN_SENT_ON_MAIL);
	}

	/********************************************************************
	 * Generate token and send to mail
	 * 
	 * @param projectId
	 *******************************************************************/
	private String generateToken(String receiverMailId, String projectId) {
		String encryptUrl = "";
		if (receiverMailId != null) {
			encryptUrl = new StringBuilder("{\"p\":").append(projectId).append(",").append("\"e\":")
					.append("\"" + receiverMailId + "\"").append("}").toString();
		} else {
			encryptUrl = new StringBuilder("{\"p\":").append(1).append(",").append("\"e\":").toString();
			encryptUrl = encryptUrl + null + "}";
		}
		LOGGER.info("INCRIPTED  URL:-------------->" + encryptUrl);
		byte[] byteArray = org.apache.commons.codec.binary.Base64.encodeBase64((encryptUrl.getBytes()));
		return new String(byteArray);
	}

	/*******************************************************************************************
	 * Send token to mail
	 * 
	 * @return
	 *******************************************************************************************/
	private String sendTokenToMail(String receiverEmailId, String projectName, String token, String dashbordImage,
			String message, String ownerName) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		String tokenUrl = environment.getProperty(CodeGripConstants.GENERATE_TOKEN_URL).concat("?").concat("key=")
				.concat(token);

		emailExecutor.execute(() -> {
			mailerHelper.sendTokenToMail(receiverEmailId, projectName,
					environment.getProperty(CodeGripConstants.SENDGRID_SUBJECT), tokenUrl, dashbordImage, message,
					ownerName);
			LOGGER.info("TOKEN DETAILS-->" + tokenUrl);
			LOGGER.info("MESSAGE:-->" + message);
		});
		emailExecutor.shutdown();
		return tokenUrl;

	}

	@Override
	public Map<String, Object> getSecurityReportDetails(String projectKey) throws IOException {

		String url = "";
		url = environment.getProperty(CodeGripConstants.SECURITY_REPORT_URL);
		url = url.replace(CodeGripConstants.PROJECT_NAME, projectKey);
		LOGGER.info("PROJECT REPORT URL -------------------:" + url);
		Map<String, Object> result = new HashMap<>();
		Map<String, Object> hotSpotData = new HashMap<>();
		try {
			JSONObject callRestAPI = JSONObject.fromObject(callGetAPI(url).toString());
			result.put("SecurityReports", callRestAPI);
			JSONArray array = callRestAPI.getJSONArray("categories");

			for (int i = 0; i < array.size(); i++) {
				if (array.getJSONObject(i).getInt("vulnerabilities") > 0) {
					hotSpotData.put(array.getJSONObject(i).getString("category"),
							getIssueList(projectKey, 1, CodeGripConstants.OWASPTOP));
				} else {
					hotSpotData.put(array.getJSONObject(i).getString("category"), null);
				}
			}

			result.put("securityHotSpotDetails", hotSpotData);
		} catch (Exception e) {
			LOGGER.error(e);
		}

		return result;
	}

	/*******************************************************************************************
	 * Get already shared email ids of users.
	 *******************************************************************************************/
	@Override
	public List<Object[]> getAlreadyShareEmailIds(int projectId, int senderId) {
		return shareDashbordRepository.findbyProjectIdAndSenderId(senderId, projectId);
	}

	/*******************************************************************************************
	 * Assign projects to users.
	 *******************************************************************************************/
	@Override
	public void assignProjectToUser(List<UserProjectsModel> userProjectsModels, Integer companyId, Integer projectId,
			Integer userId) throws CustomException {
		checkProjectBelongToAdmin(companyId, projectId, userId);
		Projects projects = projectsRepository.findById(projectId);
		Optional<Users> users = userRepository.findById(userId);

		String name = "";
		if (users.isPresent()) {
			name = users.get().getName() != null ? users.get().getName() : users.get().getEmail();
		}

		for (UserProjectsModel userProjectsModel : userProjectsModels) {
			userProjectsModel.setAssignedBy(userId);
			userProjectsModel.setProjectId(projectId);
			userProjectsModel.setIsDeleted(false);

			// Save notification of assign projects.
			userService.saveNotification(
					environment.getProperty(CodeGripConstants.ASSIGN_PROJECT_MESSAGE) + " " + projects.getName(),
					CodeGripConstants.UNREAD, environment.getProperty(CodeGripConstants.ASSIGN_PROJECT_TITLE),
					environment.getProperty(CodeGripConstants.ASSIGN_PROJECT_REASON),
					environment.getProperty(CodeGripConstants.ASSIGN_PROJECT_DESTINATION_PAGE), companyId,
					userProjectsModel.getUserId(), userProjectsModel.getProjectId(),
					CodeGripConstants.IMAGE_NEW_PROJECT_ADDED, null, true);

			// Send email to user.
			sendAssignProjectMail(userProjectsModel.getEmail(), name, projects.getName(),
					environment.getProperty(CodeGripConstants.ADDED_PROJECT_DASHBOARD_URL));

		}
		userProjectsRepository.save(CustomDozerHelper.map(mapper, userProjectsModels, UserProjects.class));

	}

	/*******************************************************************************************
	 * Send assign project to user email.
	 * 
	 * @param string
	 *******************************************************************************************/
	private void sendAssignProjectMail(String receiverEmailId, String name, String projectName, String url) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor.execute(() -> mailerHelper.sendAssignProjectMailToUser(receiverEmailId, name, projectName, url));
		emailExecutor.shutdown();

	}

	/*******************************************************************************************
	 * Check project belong to admin or not.
	 *******************************************************************************************/
	private void checkProjectBelongToAdmin(Integer companyId, Integer projectId, Integer userId)
			throws CustomException {
		if (projectsRepository.findByCompanyIdAndIdAndUserId(companyId, projectId, userId) == null) {
			throw new CustomException(environment.getProperty(CodeGripConstants.ADMIN_NOT_BELONGS_TO_PROJECT));
		}
	}

	/*******************************************************************************************
	 * User removed from project.
	 *******************************************************************************************/
	@Override
	public void removeProjectFromUser(List<UserProjectsModel> userProjectsModels) throws CustomException {
		try {
			for (UserProjectsModel userProjectsModel : userProjectsModels) {
				userProjectsModel.setId(userProjectsModel.getUserProjectId());
			}
			userProjectsRepository.save(CustomDozerHelper.map(mapper, userProjectsModels, UserProjects.class));
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

	}

	/*******************************************************************************************
	 * delete project.
	 *******************************************************************************************/
	@Override
	public void deleteProject(Integer companyId, Integer userId, Integer projectId) throws CustomException {
		try {
			checkProjectBelongToAdmin(companyId, projectId, userId);

			// Create null/dummy records of projects and its related data.
			Projects projects = createDeleteRandomProjectObject(projectId);
			List<Quality> qualities = new ArrayList<>();
			for (ProjectBranch projectBranch : projects.getProjectBranches()) {
				List<Integer> ids = new ArrayList<>();
				ids.add(projectBranch.getId());
				qualities.addAll(createDeleteRandomQualityObject(qualityRepository.findByProjectBranchIdIn(ids)));
			}

			// Delete ssh and wehbooks from particular source control.
			deleteSSHandWebhook(projects);

			qualityDAO.saveQualityRecord(qualities);
			projectDAO.saveProjectsRecord(createDeleteRandomProjectObject(projectId));
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	private void deleteSSHandWebhook(Projects projects) {
		UsersAccountDetails userAccount = usersAccountDetailsRepository
				.findByUserIdAndCompanyIdAndSourceControlNameAndAccountUsername(projects.getUserId(),
						projects.getCompanyId(), projects.getProvider(), projects.getUserName());
		// delete from bitbucket.
		if (projects.getProvider().equalsIgnoreCase("bitbucket")) {
			bitbucketAPIUtil.listWebhookFromBitbucket(projects, userAccount);
		}

	}

	/*******************************************************************************************
	 * delete project.
	 *******************************************************************************************/
	private Projects createDeleteRandomProjectObject(Integer projectId) {
		List<Projects> projects = projectsRepository.findByIdAndIsDeleted(projectId, false);
		for (Projects project : projects) {
			project.setGitCloneUrl(null);
			project.setIsDeleted(true);
			project.setName(CommonUtil.generateRandomString(8));
			project.setUserName(CommonUtil.generateRandomString(5));
			project.setUid(CommonUtil.generateRandomString(8));
			project.setProvider(CommonUtil.generateRandomString(6));
			for (ProjectBranch projectBranch : project.getProjectBranches()) {
				projectBranch.setName(CommonUtil.generateRandomString(8));
			}
		}
		return projects.get(0);
	}

	/*******************************************************************************************
	 * delete project.
	 *******************************************************************************************/
	private List<Quality> createDeleteRandomQualityObject(List<Quality> qualities) {
		for (Quality quality : qualities) {
			quality.setProjectKey(CommonUtil.generateRandomString(8));
			quality.setProjectName(CommonUtil.generateRandomString(8));
			quality.setQualityDetailsUrl(null);
			quality.setTaskId(null);
			quality.setCommitId(null);
			quality.setCommitterName(null);
		}
		return qualities;
	}

	/*******************************************************************************************
	 * Get list of projects for dashboard.
	 *******************************************************************************************/
	@Override
	public List<ProjectsModel> startedProjects(Integer companyId, Integer userId, String role) throws CustomException {
		List<Projects> projects = new ArrayList<>();
		if ("ROLE_USER".equalsIgnoreCase(role)) {
			projects = sourceControlDAO.getStartedProjectListOfUser(userId, "ALL", companyId, null);
		} else {
			projects = sourceControlDAO.getStartedProjectList(companyId, "ALL", null);
		}
		return CustomDozerHelper.map(mapper, projects, ProjectsModel.class);
	}

	/*********************************************************************************************
	 * Update status of project scanning in db.
	 *********************************************************************************************/
	public void updateStatusAndSendMailToAdmin(ProjectsModel projectsModel, String output, String status)
			throws CustomException {
		projectDAO.updateProjectErrorMessage(projectsModel.getId(), output, status);
		projectDAO.updateBranchErrorMessage(projectsModel.getProjectBranches().get(0).getId(), output, status);
		mailerHelper.sendExceptionEmail(status, projectsModel.getProjectBranches().get(0).getId(), output);

	}

}
