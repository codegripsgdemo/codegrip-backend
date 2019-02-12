package com.mb.codegrip.service;

import static com.mb.codegrip.constants.CodeGripConstants.ACCESS_TOKEN;
import static com.mb.codegrip.constants.CodeGripConstants.BITBUCKET_PROVIDER;
import static com.mb.codegrip.constants.CodeGripConstants.BLOCKER_KEY;
import static com.mb.codegrip.constants.CodeGripConstants.BLOCKER_RATING_KEY;
import static com.mb.codegrip.constants.CodeGripConstants.CRITICAL_KEY;
import static com.mb.codegrip.constants.CodeGripConstants.GITHUB_PROVIDER;
import static com.mb.codegrip.constants.CodeGripConstants.GITLAB_PROVIDER;
import static com.mb.codegrip.constants.CodeGripConstants.MAINTAINABILITY_LABEL;
import static com.mb.codegrip.constants.CodeGripConstants.METRIC_KEY;
import static com.mb.codegrip.constants.CodeGripConstants.REFRESH_TOKEN;
import static com.mb.codegrip.constants.CodeGripConstants.SECURITY_RATING_KEY;
import static com.mb.codegrip.constants.CodeGripConstants.VALUE_KEY;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dao.ObjectDAO;
import com.mb.codegrip.dao.QualityDAO;
import com.mb.codegrip.dao.SourceControlDAO;
import com.mb.codegrip.dto.Company;
import com.mb.codegrip.dto.ProjectBranch;
import com.mb.codegrip.dto.Projects;
import com.mb.codegrip.dto.Quality;
import com.mb.codegrip.dto.QualityCondition;
import com.mb.codegrip.dto.Users;
import com.mb.codegrip.dto.UsersAccountDetails;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.AdminDashboardModel;
import com.mb.codegrip.model.ChartDataModel;
import com.mb.codegrip.model.CompanyActivePlanDetailsModel;
import com.mb.codegrip.model.CompanySubscriptionModel;
import com.mb.codegrip.model.LineChartDataModel;
import com.mb.codegrip.model.ProjectBranchModel;
import com.mb.codegrip.model.ProjectsModel;
import com.mb.codegrip.model.QualityModel;
import com.mb.codegrip.model.SingleProjectDashboardModel;
import com.mb.codegrip.model.SourceControlModel;
import com.mb.codegrip.model.UsersAccountDetailsModel;
import com.mb.codegrip.repository.ProjectsRepository;
import com.mb.codegrip.repository.UserRepository;
import com.mb.codegrip.repository.UsersAccountDetailsRepository;
import com.mb.codegrip.utils.BitbucketAPIUtil;
import com.mb.codegrip.utils.CommonUtil;
import com.mb.codegrip.utils.CustomDozerHelper;
import com.mb.codegrip.utils.GitLabAPIUtil;
import com.mb.codegrip.utils.GithubAPIUtil;

@Service
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:exception.properties"), @PropertySource("classpath:notifications.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class SourceControlServiceImpl implements SourceControlService, EnvironmentAware {
	private static final Logger LOGGER = Logger.getLogger(SourceControlServiceImpl.class);

	@Autowired
	private SourceControlDAO sourceControlDAO;

	@Autowired
	private UsersAccountDetailsRepository usersAccountDetailsRepository;

	@Autowired
	private ProjectsRepository projectsRepository;

	@Autowired
	private QualityDAO qualityDAO;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private BillingService billingService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;
	
	@Autowired
	private ObjectDAO objectDao;

	GitLabAPIUtil gitLabAPiUtils = new GitLabAPIUtil();

	CommonUtil commonUtil = new CommonUtil();

	private static Environment environment;

	public String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	Mapper mapper = new DozerBeanMapper();

	BitbucketAPIUtil bitbucketAPIUtil = new BitbucketAPIUtil();
	GitLabAPIUtil gitLabAPIUtil = new GitLabAPIUtil();
	GithubAPIUtil githubAPIUtil = new GithubAPIUtil();

	/**
	 * @Autowired private UserRepository userRepository;
	 * 
	 * 
	 * @Autowired public SourceControlServiceImpl(UserRepository accountRepository)
	 *            { this.userRepository = accountRepository; }
	 */

	/*********************************************************************************************************************
	 * Get repository from source control.
	 *********************************************************************************************************************/
	@Override
	public Map<String, Object> getSourceControlRepository(SourceControlModel sourceControlModel,
			HttpServletRequest request, String pageNo) throws CustomException, JSONException {
		LOGGER.info("<----- In getSoruceControlRepository ----->");
		// For getting user details.

		Map<String, Object> repolistAndAccountDetails = new HashMap<>();

		Optional<Users> users = userRepository.findById(sourceControlModel.getUserId());
		List<Projects> projects = new ArrayList<>();

		if (users.isPresent())
			projects = sourceControlDAO.getStartedProjectList(users.get().getOwnerCompanyId(), pageNo, null);
		LOGGER.info("Username: " + users.get().getEmail());
		UsersAccountDetails usersAccountDetails = new UsersAccountDetails();
		Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.

		JSONObject accessTokens = new JSONObject();
		if (sourceControlModel.getCode() != null) {
			if (sourceControlModel.getName().equals(BITBUCKET_PROVIDER)) {
				accessTokens = bitbucketAPIUtil.getNewAccesstokenBitbucket(sourceControlModel.getCode(),
						CodeGripConstants.CODE);
				calendar.add(Calendar.SECOND, accessTokens.getInt("expires_in"));
				usersAccountDetails.setRefreshToken(accessTokens.getString(REFRESH_TOKEN));
				usersAccountDetails.setSourceControlName(BITBUCKET_PROVIDER);
			} else if (sourceControlModel.getName().equals(GITHUB_PROVIDER)) {
				accessTokens = githubAPIUtil.getNewAccesstokenGithub(sourceControlModel.getCode(),
						CodeGripConstants.CODE);
				usersAccountDetails.setSourceControlName(GITHUB_PROVIDER);
			} else if (sourceControlModel.getName().equals(GITLAB_PROVIDER)) {
				accessTokens = gitLabAPIUtil.getNewAccessTokenApi(sourceControlModel.getCode(), CodeGripConstants.CODE);
				LOGGER.info("ACCESS TOKEN :------> " + accessTokens);
				usersAccountDetails.setRefreshToken(accessTokens.getString(REFRESH_TOKEN));
				usersAccountDetails.setSourceControlName(GITLAB_PROVIDER);
			}

			usersAccountDetails.setAccessToken(accessTokens.getString(ACCESS_TOKEN));

		} else {
			if (sourceControlModel.getName().equals(BITBUCKET_PROVIDER)) {
				accessTokens = bitbucketAPIUtil.getNewAccesstokenBitbucket((sourceControlModel.getRefreshToken() == null
						? sourceControlDAO.getRefreshTokenFromDB(users.get().getId())
						: sourceControlModel.getRefreshToken()), REFRESH_TOKEN);
				calendar.add(Calendar.SECOND, (accessTokens.getInt("expires_in")));
				usersAccountDetails.setRefreshToken(accessTokens.getString(REFRESH_TOKEN));
				usersAccountDetails.setAccessToken(accessTokens.getString(ACCESS_TOKEN));
				usersAccountDetails.setSourceControlName(BITBUCKET_PROVIDER);
			} else if (sourceControlModel.getName().equals(GITHUB_PROVIDER)) {
				accessTokens.put(ACCESS_TOKEN, sourceControlModel.getAccessToken());
				usersAccountDetails.setAccessToken(sourceControlModel.getAccessToken());
				usersAccountDetails.setSourceControlName(GITHUB_PROVIDER);
			} else if (sourceControlModel.getName().equals(GITLAB_PROVIDER)) {
				accessTokens = gitLabAPIUtil.getNewAccessTokenApi(sourceControlModel.getRefreshToken(), REFRESH_TOKEN);
				usersAccountDetails.setAccessToken(sourceControlModel.getAccessToken());
				usersAccountDetails.setSourceControlName(GITLAB_PROVIDER);
			}

		}
		List<ProjectsModel> repositoryListNew = new ArrayList<>();
		JSONObject getUserData = new JSONObject();
		if (sourceControlModel.getName().equals(BITBUCKET_PROVIDER)) {
			JSONObject getUserName = bitbucketAPIUtil.getUserNameFromBitbucket(accessTokens.getString(ACCESS_TOKEN));
			repositoryListNew = bitbucketAPIUtil.getRepositorylistFromBitbucket(
					getUserName.getString(CodeGripConstants.USERNAME), accessTokens.getString(ACCESS_TOKEN));
			usersAccountDetails.setAccountUsername(getUserName.getString("username"));
		} else if (sourceControlModel.getName().equals(GITHUB_PROVIDER)) {
			String token = sourceControlModel.getAccessToken() == null ? accessTokens.getString(ACCESS_TOKEN)
					: sourceControlModel.getAccessToken();
			if (sourceControlModel.getUserAccountNameOrId() == null)
				getUserData = githubAPIUtil.getUserDataFromGithub(token);

			LOGGER.info("Gihub user date: " + getUserData);
			repositoryListNew = githubAPIUtil.getRepositorylistFromGithub(getUserData.getString("repos_url"), token,
					Integer.parseInt(pageNo));

			usersAccountDetails.setAccessToken(token);
			usersAccountDetails.setAccountUsername(getUserData.getString("login"));
		} else if (sourceControlModel.getName().equals(GITLAB_PROVIDER)) {
			int id = 0;
			if (sourceControlModel.getUserAccountNameOrId() == null) {
				getUserData = gitLabAPIUtil.getGitLabUserDetails(accessTokens.getString(ACCESS_TOKEN));
				LOGGER.info("GITLAB User Data :" + getUserData);
				id = getUserData.getInt("id");
			} else {
				id = Integer.parseInt(sourceControlModel.getUserAccountNameOrId());
			}
			usersAccountDetails.setGitlabUserId(id);
			usersAccountDetails.setAccountUsername(getUserData.getString("username"));
			repositoryListNew = gitLabAPIUtil.getProjectListFromGitLab(id, accessTokens.getString(ACCESS_TOKEN));
		}
		/**
		 * Save user account details.
		 */
		UsersAccountDetails usersAccountDetailsOld = usersAccountDetailsRepository
				.findByAccountUsernameAndUserIdAndSourceControlName(usersAccountDetails.getAccountUsername(), users.get().getId(), usersAccountDetails.getSourceControlName());
		if (usersAccountDetailsOld != null)
			usersAccountDetails.setId(usersAccountDetailsOld.getId());
		usersAccountDetails.setIsDeleted(false);
		usersAccountDetails.setSourceControlName(sourceControlModel.getName());
		String format = new SimpleDateFormat(CodeGripConstants.DATE_FORMAT).format(calendar.getTime());
		usersAccountDetails.setTokenExpiresTime(Timestamp.valueOf(format));
		usersAccountDetails.setUserId(users.get().getId());
		usersAccountDetails.setCompanyId(users.get().getOwnerCompanyId());
		usersAccountDetailsRepository.save(usersAccountDetails);
		if (projects != null)
			repolistAndAccountDetails.put("repositoryList", sortRepoList(repositoryListNew, projects));
		else
			repolistAndAccountDetails.put("repositoryList", repositoryListNew);

		// get started project list count and plan data.
		repolistAndAccountDetails.put("subscriptionDetails",
				getSubscriptionAndProjectDetails(users.get().getOwnerCompanyId()));

		// get company's connected account list.
		repolistAndAccountDetails.put(CodeGripConstants.CONNECTED_ACCOUNTS, CustomDozerHelper.map(mapper,
				usersAccountDetailsRepository.findByUserId(users.get().getId()), UsersAccountDetailsModel.class));
		return repolistAndAccountDetails;
	}

	/*********************************************************************************************************************
	 * Get company subscription and project limit details.
	 *********************************************************************************************************************/
	@Override
	public CompanySubscriptionModel getSubscriptionAndProjectDetails(Integer ownerCompanyId) throws CustomException {
		CompanySubscriptionModel companySubscriptionModel = billingService
				.getSubscriptionDetailsByCompany(ownerCompanyId);
		Integer startedProjectCount = projectsRepository.findByCompanyId(ownerCompanyId).size();
		companySubscriptionModel.setTotalProjects(startedProjectCount);
		if ("Startup".equalsIgnoreCase(companySubscriptionModel.getProducts().getProductName()))
			companySubscriptionModel
					.setRemainingProjects(CodeGripConstants.STARTUP_PROJECT_LIMIT - startedProjectCount);
		return companySubscriptionModel;
	}

	/*********************************************************************************************************************
	 * Get started project list from DB.
	 *********************************************************************************************************************/
	@Override
	public Map<String, Object> getStartedProjectList(Integer companyId, String pageNo, String role, Integer userId,
			Integer projectId) throws CustomException {
		List<Projects> projects = new ArrayList<>();
		List<ProjectsModel> projectsModels = new ArrayList<>();
		CompanyActivePlanDetailsModel companyActivePlanDetailsModel = new CompanyActivePlanDetailsModel();
		try {
			if (projectId != null) {
				projects = projectsRepository.findByIdAndIsDeleted(projectId, false);
			} else {
				companyActivePlanDetailsModel = getCompanyActivePlanDetails(companyId, userId, role);
				LOGGER.info("Role: " + role);
				if ("ROLE_USER".equalsIgnoreCase(role)) {
					projects = sourceControlDAO.getStartedProjectListOfUser(userId, pageNo, companyId, null);
				} else {
					projects = sourceControlDAO.getStartedProjectList(companyId, pageNo, null);
				}
			}
			projectsModels = CustomDozerHelper.map(mapper, projects, ProjectsModel.class);
			Integer effortTotal = 0;
			double issuesTotal = 0;
			double totalLines = 0;
			String effortTotalResult = "";
			for (ProjectsModel projectModel : projectsModels) {
				int cnt = 0;
				for (ProjectBranchModel projectBranchModel : projectModel.getProjectBranches()) {
					List<SingleProjectDashboardModel> singleProjectDashboardModels = new ArrayList<>();
					/**
					 * String projectName = getProjectName(projectBranchModel.getName(),
					 * projectBranchModel.getBranchKey());
					 */

					String projectName = projectBranchModel.getName();
					LOGGER.info("PROJECT NAME:" + projectName);

					net.sf.json.JSONObject jsonObject = projectService.getIssueList(projectName,
							pageNo.equalsIgnoreCase("all") ? 1 : Integer.parseInt(pageNo), null);
					Integer signleProjectEffortTotal = Integer.parseInt(jsonObject.getString("effortTotal"));
					net.sf.json.JSONObject overAllIssues = projectService.createQualityConditions(projectName);

					cnt++;
					if (cnt == 1) {
						effortTotal = effortTotal + Integer.parseInt(jsonObject.getString("effortTotal"));
						issuesTotal = issuesTotal + Double.parseDouble(jsonObject.getString("total"));
						effortTotalResult = caluateEfforts(effortTotal);
						LOGGER.info("Total Issues = " + effortTotalResult);
						LOGGER.info("effort Total =" + effortTotal);
					}

					List<Quality> quality = qualityDAO.getQualityDetails(projectBranchModel.getId());
					if (!quality.isEmpty()) {
						List<QualityModel> qualityModels = CustomDozerHelper.map(mapper, quality, QualityModel.class);
						projectBranchModel.setQualities(qualityModels);

						// calculate project dashboard data.

						if (overAllIssues != null) {
							singleProjectDashboardModels.add(createSingleProjectData(qualityModels,
									quality.get(0).getProjectKey(), signleProjectEffortTotal, overAllIssues));
						}

						for (QualityCondition qualityCondition : quality.get(0).getQualityConditions()) {

							// Count number of lines in project.
							if ("ncloc_language_distribution".equals(qualityCondition.getMetric())
									&& qualityCondition.getValue() != null) {
								String[] data = qualityCondition.getValue().split(";");
								double singleProjectTotalLines = 0;
								for (String line : data) {
									String[] lines = line.split("=");
									double value = Double.parseDouble(lines[1]);
									singleProjectTotalLines += value;
								}
								totalLines += singleProjectTotalLines;
							}

							projectBranchModel.setSingleProjectDashboardModels(singleProjectDashboardModels);
						}
					} else {
						projectBranchModel.setQualities(null);
					}
				}
			}
			Map<String, Object> finalResult = new HashMap<>();

			LOGGER.info("Project Model size --> " + projectsModels.size());
			if (effortTotalResult.isEmpty()) {
				effortTotalResult = "0";
			}
			finalResult.put("userPlanDetails", companyActivePlanDetailsModel);
			finalResult.put("totalProjects", projectsModels.size());
			finalResult.put("projectDetails", projectsModels);
			finalResult.put("effortsTotal", effortTotalResult);
			finalResult.put("totalLinesOfCode", (totalLines > 999 ? formatProjectCodeLine(totalLines, 0) : totalLines));
			finalResult.put("issuesTotal", issuesTotal);

			return finalResult;
		} catch (Exception e) {
			LOGGER.info(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.NOT_FOUND));
		}
	}

	/***************************************************************************************************
	 * Get company active plan details.
	 * 
	 * @param role
	 * @throws CustomException
	 ***************************************************************************************************/
	CompanyActivePlanDetailsModel getCompanyActivePlanDetails(Integer companyId, Integer userId, String role)
			throws CustomException {
		LOGGER.info("-------- In getCompanyActivePlanDetails -----------");
		CompanyActivePlanDetailsModel companyActivePlanDetailsModel = new CompanyActivePlanDetailsModel();
		CompanySubscriptionModel companySubscriptionModel = billingService.getSubscriptionDetailsByCompany(companyId);

		if (!"ROLE_USER".equalsIgnoreCase(role)) {
			companyActivePlanDetailsModel.setTotalUserDetails(userService.userCountByCompany(companyId));
		}
		companyActivePlanDetailsModel.setCompanySubscriptionModel(companySubscriptionModel);
		LOGGER.info("UserId: " + userId);
		return filterUserSubscriptionData(companySubscriptionModel, companyActivePlanDetailsModel);
	}

	/***************************************************************************************************
	 * Filter user subscription data.
	 ***************************************************************************************************/
	CompanyActivePlanDetailsModel filterUserSubscriptionData(CompanySubscriptionModel companySubscriptionModel,
			CompanyActivePlanDetailsModel companyActivePlanDetailsModel) {
		companyActivePlanDetailsModel.setExpiredDate(companySubscriptionModel.getEndDate().toString());
		if (companySubscriptionModel.getStatus().equals("SUBSCRIPTION_INACTIVE"))
			companyActivePlanDetailsModel.setPaymentFailed(true);
		if (companySubscriptionModel.getEndDate().before(new Timestamp(System.currentTimeMillis())))
			companyActivePlanDetailsModel.setIsExpired(true);
		return companyActivePlanDetailsModel;
	}

	/***************************************************************************************************
	 * Create single project dashboard data.
	 ***************************************************************************************************/
	private SingleProjectDashboardModel createSingleProjectData(List<QualityModel> qualityModels, String projectKey,
			Integer signleProjectEffortTotal, net.sf.json.JSONObject overAllIssues) throws IOException {

		SingleProjectDashboardModel projectDashboardData = createProjectDashboardData(overAllIssues);
		projectDashboardData.setStatus(qualityModels.get(0).getStatus());
		projectDashboardData.setCommitDate(qualityModels.get(0).getAnalyzeAt());

		projectDashboardData.setTotalFiles(getTotalFilesOfProject(projectKey));
		projectDashboardData.setSingleBranchEffort(caluateEfforts(signleProjectEffortTotal));
		return projectDashboardData;
	}

	/***************************************************************************************************
	 * Create project card details.
	 ***************************************************************************************************/
	private SingleProjectDashboardModel createProjectDashboardData(net.sf.json.JSONObject overAllIssues) {
		SingleProjectDashboardModel singleProjectDashboardModel = new SingleProjectDashboardModel();
		net.sf.json.JSONObject component = overAllIssues.getJSONObject("component");
		net.sf.json.JSONArray measures = component.getJSONArray("measures");
		Long newIssues = 0L;
		Long issues = 0L;
		Integer issuesDept = 0;
		for (int i = 0; i < measures.size(); i++) {
			net.sf.json.JSONObject metric = measures.getJSONObject(i);
			if ("sqale_rating".equals(metric.getString(METRIC_KEY))) {
				singleProjectDashboardModel
						.setMaintainabilityRating(getRatingWithIndexValue(metric.getString(VALUE_KEY), "asc"));
			}
			if ("new_code_smells".equals(metric.getString(METRIC_KEY))
					|| "new_bugs".equals(metric.getString(METRIC_KEY))
					|| "new_vulnerabilities".equals(metric.getString(METRIC_KEY))) {
				net.sf.json.JSONArray newCodeSmells = metric.getJSONArray("periods");
				net.sf.json.JSONObject val = newCodeSmells.getJSONObject(0);
				newIssues += Long.parseLong(val.getString(VALUE_KEY));
			}
			if ("code_smells".equals(metric.getString(METRIC_KEY)) || "bugs".equals(metric.getString(METRIC_KEY))
					|| "vulnerabilities".equals(metric.getString(METRIC_KEY))) {
				issues += Long.parseLong(metric.getString(VALUE_KEY));
			}
			if ("coverage".equals(metric.getString(METRIC_KEY))) {
				singleProjectDashboardModel
						.setCoverageArrow(Double.parseDouble(metric.getString(VALUE_KEY)) > 20 ? "UP" : "DOWN");
				singleProjectDashboardModel.setCoverage(metric.getString(VALUE_KEY) + "%");
			}
			if ("duplicated_lines_density".equals(metric.getString(METRIC_KEY))) {
				singleProjectDashboardModel.setDuplications(metric.getString(VALUE_KEY) + "%");
			}
			if ("duplicated_lines".equals(metric.getString(METRIC_KEY))) {
				singleProjectDashboardModel.setDuplicatedLines(metric.getString(VALUE_KEY));
			}
			if ("lines_to_cover".equals(metric.getString(METRIC_KEY))) {
				singleProjectDashboardModel.setLinesToCover(metric.getString(VALUE_KEY));
			}
			if ("sqale_index".equals(metric.getString(METRIC_KEY))) {
				singleProjectDashboardModel
						.setCodeSmellDept(caluateEfforts(Integer.parseInt(metric.getString(VALUE_KEY))));
			}
			if ("code_smells".equals(metric.getString(METRIC_KEY))) {
				singleProjectDashboardModel.setCodeSmells(metric.getString(VALUE_KEY));
			}
			if ("reliability_remediation_effort".equals(metric.getString(METRIC_KEY))
					|| "sqale_index".equals(metric.getString(METRIC_KEY))
					|| "security_remediation_effort".equals(metric.getString(METRIC_KEY))) {
				issuesDept += Integer.parseInt(metric.getString(VALUE_KEY));
			}

		}
		singleProjectDashboardModel.setIssuesDept(caluateEfforts(issuesDept));
		singleProjectDashboardModel.setNewIssues(newIssues.toString());
		singleProjectDashboardModel.setIssues(issues.toString());
		return singleProjectDashboardModel;
	}

	/***************************************************************************************************
	 * Get total files of specific project.
	 * 
	 * @throws IOException
	 ***************************************************************************************************/
	private String getTotalFilesOfProject(String projectKey) throws IOException {
		String url = environment.getProperty(CodeGripConstants.GET_TOTAL_FILES);
		url = url.replace(CodeGripConstants.PROJECT_DIR, projectKey);
		LOGGER.info("URL to no of filest----------------- " + url);
		return filterTotalFileNo(net.sf.json.JSONObject.fromObject(projectService.callGetAPI(url).toString()));
	}

	/***************************************************************************************************
	 * Filter object to find total files in project.
	 ***************************************************************************************************/
	private String filterTotalFileNo(net.sf.json.JSONObject fromObject) {
		net.sf.json.JSONObject paging = fromObject.getJSONObject("paging");
		return Integer.toString(paging.getInt("total"));
	}

	/***************************************************************************************************
	 * Create single project dashboard model.
	 ***************************************************************************************************/
	/**
	 * private SingleProjectDashboardModel
	 * createSingleProjectDashboardData(List<QualityConditionModel> list) {
	 * SingleProjectDashboardModel singleProjectDashboardModel = new
	 * SingleProjectDashboardModel(); Integer issues = 0; Integer newIssues = 0; for
	 * (QualityConditionModel qualityCondition : list) {
	 * singleProjectDashboardModel.setSecurity(
	 * CodeGripConstants.VULNERABILITIES.equals(qualityCondition.getMetric()) ?
	 * qualityCondition.getValue() : singleProjectDashboardModel.getSecurity());
	 * issues = issues + ("bugs".equals(qualityCondition.getMetric()) ?
	 * Integer.parseInt(qualityCondition.getValue()) : 0); LOGGER.info("Single
	 * issue: " + issues); issues = issues +
	 * (CodeGripConstants.CODE_SMELLS.equals(qualityCondition.getMetric()) ?
	 * Integer.parseInt(qualityCondition.getValue()) : 0); LOGGER.info("Added issue:
	 * " + issues); singleProjectDashboardModel.setIssues(issues.toString());
	 * 
	 * // Calculate new issues. newIssues = newIssues +
	 * ("new_bugs".equals(qualityCondition.getMetric()) ?
	 * Integer.parseInt(qualityCondition.getValue()) : 0); LOGGER.info("Single
	 * issue: " + issues); newIssues = newIssues +
	 * ("new_code_smells".equals(qualityCondition.getMetric()) ?
	 * Integer.parseInt(qualityCondition.getValue()) : 0); LOGGER.info("Added issue:
	 * " + newIssues);
	 * singleProjectDashboardModel.setNewIssues(newIssues.toString());
	 * 
	 * singleProjectDashboardModel.setDuplications(
	 * "duplicated_lines_density".equals(qualityCondition.getMetric()) ?
	 * qualityCondition.getValue() + "%" :
	 * singleProjectDashboardModel.getDuplications()); singleProjectDashboardModel
	 * .setCoverage("coverage".equals(qualityCondition.getMetric()) ?
	 * qualityCondition.getValue() + "%" :
	 * singleProjectDashboardModel.getCoverage());
	 * singleProjectDashboardModel.setMaintainabilityRating(
	 * CodeGripConstants.CODE_SMELLS.equals(qualityCondition.getMetric()) ?
	 * qualityCondition.getRating() :
	 * singleProjectDashboardModel.getMaintainabilityRating());
	 * singleProjectDashboardModel.setUntestedArea("coverage".equals(qualityCondition.getMetric())
	 * ? Double.toString((100 - Double.parseDouble(qualityCondition.getValue()))) +
	 * "%" : null); singleProjectDashboardModel.setCodeSmells(
	 * CodeGripConstants.CODE_SMELLS.equals(qualityCondition.getMetric()) ?
	 * qualityCondition.getValue() : singleProjectDashboardModel.getCodeSmells()); }
	 * return singleProjectDashboardModel; }
	 */
	/***************************************************************************************************
	 * Convert number into k,m,b,t format.
	 ***************************************************************************************************/
	private String formatProjectCodeLine(double val, int iteration) {
		char[] c = new char[] { 'k', 'm', 'b', 't' };
		double d = ((long) val / 100) / 10.0;
		boolean isRound = (d * 10) % 10 == 0;// true if the decimal part is equal to 0 (then it's trimmed anyway)
		return (d < 1000 ? // this determines the class, i.e. 'k', 'm' etc
				((d > 99.9 || isRound || (!isRound && d > 9.99) ? // this decides whether to trim the decimals
						(int) d * 10 / 10 : d + "" // (int) d * 10 / 10 drops the decimal
				) + "" + c[iteration]) : formatProjectCodeLine(d, iteration + 1));
	}

	/***************************************************************************************************
	 * Method for calculating effort total
	 ************************************************************************************************/
	private String caluateEfforts(Integer effortTotal) {

		int seconds = effortTotal * 60;
		int secondsInMinute = 60;
		int secondsInHour = 60 * secondsInMinute;
		int secondsInDay = 8 * secondsInHour;
		int secondsInMonth = 30 * secondsInDay;
		int secondsInYear = 12 * secondsInMonth;

		int year = Math.floorDiv(seconds, secondsInYear);

		int monthSeconds = seconds % secondsInYear;
		int months = Math.floorDiv(monthSeconds, secondsInMonth);

		int daySecoands = monthSeconds % secondsInMonth;
		int days = Math.floorDiv(daySecoands, secondsInDay);

		int hourSecoands = daySecoands % secondsInDay;
		int hours = Math.floorDiv(hourSecoands, secondsInHour);

		int minuteSecoands = hourSecoands % secondsInHour;
		int minutes = Math.floorDiv(minuteSecoands, secondsInMinute);

		HashMap<String, String> effortVal = new LinkedHashMap<>();
		effortVal.put("year", year != 0 ? year + "y " : "");
		effortVal.put(CodeGripConstants.MONTH, months != 0 ? months + "m " : "");
		effortVal.put("days", days != 0 ? days + "d " : "");
		effortVal.put(CodeGripConstants.HOURS, hours != 0 ? hours + "h " : "");
		effortVal.put(CodeGripConstants.MINUTES, minutes != 0 ? minutes + "m " : "");

		String finalEffortData = effortVal.get("year") != null ? effortVal.get("year") : "";
		finalEffortData += effortVal.get("month") != null ? effortVal.get("month") : "";
		finalEffortData += effortVal.get("days") != null ? effortVal.get("days") : "";
		finalEffortData += effortVal.get(CodeGripConstants.HOURS) != null ? effortVal.get(CodeGripConstants.HOURS) : "";
		finalEffortData += effortVal.get(CodeGripConstants.MINUTES) != null ? effortVal.get(CodeGripConstants.MINUTES)
				: "";

		LOGGER.info(finalEffortData);

		StringBuilder newStr = new StringBuilder();
		int cnt = 0;
		for (int i = 0; i < finalEffortData.length(); i++) {
			newStr.append(finalEffortData.charAt(i));
			if ((int) finalEffortData.charAt(i) >= 65 && (int) finalEffortData.charAt(i) <= 122) {
				cnt++;
			}
			if (cnt == 2)
				break;
		}
		return newStr.toString();
	}

	/**
	 * private String getProjectName(String name, String branchKey) { return new
	 * StringBuilder(name).append(":").append(branchKey).toString(); }
	 */

	/*********************************************************************************************************
	 * Sort repository list
	 *********************************************************************************************************/
	private List<ProjectsModel> sortRepoList(List<ProjectsModel> repositoryListNew, List<Projects> repositoryListOld) {
		List<ProjectsModel> finalRepoList = new ArrayList<>();
		List<String> oldRepoName = new ArrayList<>();
		for (Projects projects : repositoryListOld) {
			oldRepoName.add(projects.getName());
		}
		for (ProjectsModel newRepo : repositoryListNew) {
			if (!oldRepoName.contains(newRepo.getName())) {
				newRepo.setIsAnalyzeStarted(false);
				finalRepoList.add(newRepo);
			} else {
				newRepo.setIsAnalyzeStarted(true);
				finalRepoList.add(newRepo);
			}
		}
		return finalRepoList;
	}

	@Override
	public String getFileContnet(SourceControlModel sourceControlModel) {
		return null;
	}

	/****************************************************************************************************
	 * Deletes all record of user.
	 *****************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public void hardDeleteUserData(List<String> emailList) {
		try {
			for (String emailId : emailList) {
				Users user = objectDao.getObjectByParam(Users.class, "email", emailId);
				if (null != user.getOwnerCompanyId()) {
					Company company = objectDao.getObjectById(Company.class, user.getOwnerCompanyId());
					if(null != company) {
						List<Projects> projectList = (List<Projects>) objectDao.listObjectByParam(Projects.class,
								"companyId", company.getId());
						if (null != projectList && !projectList.isEmpty()) {
							deleteProject(projectList);
						}
						objectDao.deleteObject(company);
					}
				}
				objectDao.deleteObject(user);
			}
	 }catch(Exception e) {
		 LOGGER.error(e.getMessage());
	 }
	}

	/****************************************************************************************************
	 * Get admin dashboard data.
	 *****************************************************************************************************/
	@Override
	public Map<String, Object> adminDashboardData(ProjectsModel projectModel) throws CustomException, ParseException {
		Map<String, Object> dashboardData = new HashMap<>();
		List<Projects> projects = new ArrayList<>();
		CompanyActivePlanDetailsModel companyActivePlanDetailsModel = getCompanyActivePlanDetails(
				projectModel.getCompanyId(), projectModel.getUserId(), projectModel.getRole());
		if ("ROLE_USER".equalsIgnoreCase(projectModel.getRole())) {
			projects = sourceControlDAO.getStartedProjectListOfUser(projectModel.getUserId(), "ALL",
					projectModel.getCompanyId(), projectModel.getProjectIds());
		} else {
			projects = sourceControlDAO.getStartedProjectList(projectModel.getCompanyId(), "ALL",
					projectModel.getProjectIds());
		}

		if (projects.isEmpty()) {
			throw new CustomException(environment.getProperty(CodeGripConstants.NO_PROJECTS_FOUND),
					HttpStatus.NO_CONTENT);
		}

		AdminDashboardModel adminDashboardModel = calculateCardDetails(projects);
		Map<String, Object> chartData = createChartData(projects);
		Map<String, Object> finalChartData = new HashMap<>();
		finalChartData.put(CodeGripConstants.DUPLICATION_KEY,
				createChartObject(chartData.get(CodeGripConstants.DUPLICATION_KEY), CodeGripConstants.DUPLICATION_COLOR,
						CodeGripConstants.TRANSPERENT_COLOR, CodeGripConstants.DUPLICATION_LABEL,
						CodeGripConstants.CIRCLE_POINT_STYLE));
		finalChartData.put(CodeGripConstants.MAINTAINABILITY_KEY,
				createChartObject(chartData.get(CodeGripConstants.MAINTAINABILITY_KEY),
						CodeGripConstants.MAINTAINABILITY_COLOR, CodeGripConstants.TRANSPERENT_COLOR,
						CodeGripConstants.MAINTAINABILITY_LABEL, CodeGripConstants.TRIANGLE_POINT_STYLE));
		finalChartData.put(CodeGripConstants.SECURITY_KEY,
				createChartObject(chartData.get(CodeGripConstants.SECURITY_KEY), CodeGripConstants.SECURITY_COLOR,
						CodeGripConstants.TRANSPERENT_COLOR, CodeGripConstants.SECURITY_LABEL,
						CodeGripConstants.RECT_POINT_STYLE));
		finalChartData.put(CodeGripConstants.RELIABILITY_KEY,
				createChartObject(chartData.get(CodeGripConstants.RELIABILITY_KEY),
						CodeGripConstants.REILAIBILITY_COLOR, CodeGripConstants.TRANSPERENT_COLOR,
						CodeGripConstants.RELIABILITY_LABEL, CodeGripConstants.CIRCLE_POINT_STYLE));
		List<String> datasets = new ArrayList<>();
		datasets.add(CodeGripConstants.MAINTAINABILITY_KEY);
		datasets.add(CodeGripConstants.SECURITY_KEY);
		datasets.add(CodeGripConstants.RELIABILITY_KEY);

		/**
		 * Double codeSmellRating = 0.0; for (Projects project : projects) { for
		 * (ProjectBranch projectBranch : project.getProjectBranches()) { List<Quality>
		 * qualities = qualityDAO.getQualityDetails(projectBranch.getId());
		 * if("master".equalsIgnoreCase(projectBranch.getName())) { for (Quality quality
		 * : qualities) { for (QualityCondition qualityCondition :
		 * quality.getQualityConditions()) { if
		 * ("code_smells".equalsIgnoreCase(qualityCondition.getMetric())) {
		 * codeSmellRating +=
		 * Double.parseDouble(getRatingWithIndexValue(qualityCondition.getRating(),
		 * "reverse")); } } }
		 * 
		 * } } }
		 */
		/**
		 * adminDashboardModel.setMaintainabilityRating(getRatingAsPerValues(codeSmellRating,
		 * projects.size(), MAINTAINABILITY_LABEL));
		 */

		List<String> duplicationDatasets = new ArrayList<>();
		duplicationDatasets.add(CodeGripConstants.DUPLICATION_KEY);

		finalChartData.put("healthData",
				createLineChartData(chartData.get(CodeGripConstants.RELIABILITY_KEY), datasets));

		finalChartData.put("duplicationData",
				createLineChartData(chartData.get(CodeGripConstants.RELIABILITY_KEY), duplicationDatasets));

		dashboardData.put("userPlanDetails", companyActivePlanDetailsModel);
		dashboardData.put("cardDetails", adminDashboardModel);
		dashboardData.put("chartDetails", finalChartData);
		return dashboardData;
	}

	/*************************************************************************************************
	 * Create line chart data.
	 **************************************************************************************************/
	@SuppressWarnings("unchecked")
	private ChartDataModel createLineChartData(Object object, List<String> datasets) {
		Map<String, String> objectData = (Map<String, String>) object;
		ChartDataModel chartDataModel = new ChartDataModel();
		List<String> labels = new ArrayList<>();
		for (Map.Entry<String, String> entry : objectData.entrySet()) {
			labels.add(entry.getKey());
		}
		chartDataModel.setLabels(labels);
		chartDataModel.setDatasets(datasets);
		return chartDataModel;
	}

	/*************************************************************************************************
	 * Create chart details.
	 **************************************************************************************************/
	@SuppressWarnings("unchecked")
	private LineChartDataModel createChartObject(Object object, String color, String bgColor, String label,
			String pointStyle) {
		LOGGER.info(object);
		LineChartDataModel lineChartDataModel = new LineChartDataModel();
		lineChartDataModel.setBackgroundColor(bgColor);
		lineChartDataModel.setBorderColor(color);
		lineChartDataModel.setFill(true);
		lineChartDataModel.setLabel(label);
		lineChartDataModel.setLineTension(CodeGripConstants.LINE_TENSION);
		lineChartDataModel.setPointBackgroundColor(color);
		lineChartDataModel.setPointBorderColor(color);
		lineChartDataModel.setPointBorderWidth(CodeGripConstants.POINT_BORDER_WIDTH);
		lineChartDataModel.setPointHitRadius(CodeGripConstants.POINT_HIT_RADIUS);
		lineChartDataModel.setPointHoverRadius(CodeGripConstants.POINT_HOVER_RADIUS);
		lineChartDataModel.setPointRadius(CodeGripConstants.POINT_RADIUS);
		lineChartDataModel.setPointStyle(pointStyle);
		lineChartDataModel.setShowInLegend(true);

		List<Double> dataSets = new ArrayList<>();
		Map<String, String> objectData = (Map<String, String>) object;
		for (Map.Entry<String, String> entry : objectData.entrySet()) {
			if (entry.getValue().equals("0.0")) {
				dataSets.add(null);
			} else {
				dataSets.add(Double.parseDouble(entry.getValue()));
			}
		}
		lineChartDataModel.setData(dataSets);
		return lineChartDataModel;
	}

	/*************************************************************************************************
	 * Create chart details.
	 **************************************************************************************************/
	private Map<String, Object> createChartData(List<Projects> projects) throws CustomException, ParseException {
		LinkedHashMap<String, String> reliability = new LinkedHashMap<>();
		LinkedHashMap<String, String> security = new LinkedHashMap<>();
		LinkedHashMap<String, String> maintainability = new LinkedHashMap<>();

		Map<String, Object> chartData = new HashMap<>();

		LinkedHashMap<String, String> duplication = createEmpltyMapWithDate(CodeGripConstants.CHART_DAYS);
		security.putAll(duplication);
		reliability.putAll(duplication);
		maintainability.putAll(duplication);

		for (Projects project : projects) {
			for (ProjectBranch projectBranch : project.getProjectBranches()) {

				if ("master".equals(projectBranch.getBranchKey())) {
					List<Quality> qualities = qualityDAO.getQualityDetailsAsPerDays(projectBranch.getId(),
							CodeGripConstants.CHART_DAYS);

					for (Quality quality : qualities) {

						Double addDuplication = 0.0;
						Long addBugs = 0L;
						Long addVulnerabilities = 0L;
						Long addCodeSmell = 0L;

						String chartDate = commonUtil.convertToChartDate(quality.getAnalyzeAt().toString());
						for (QualityCondition qualityCondition : quality.getQualityConditions()) {
							if ("duplicated_lines_density".equalsIgnoreCase(qualityCondition.getMetric())) {
								Double val = Double.parseDouble(qualityCondition.getValue());
								/**
								 * if (duplication.containsKey(chartDate)) { addDuplication +=
								 * Double.parseDouble(duplication.get(chartDate)); }
								 */
								addDuplication += val;
								duplication.put(chartDate, addDuplication.toString());
							} else if ("bugs".equalsIgnoreCase(qualityCondition.getMetric())) {
								Double val = Double.parseDouble(qualityCondition.getValue());
								/**
								 * if (reliability.containsKey(chartDate)) { Long tempVal = 0L; if
								 * ("0.0".equals(reliability.get(chartDate))) { tempVal =
								 * (long)Double.parseDouble(reliability.get(chartDate)); } else { tempVal =
								 * Long.parseLong(reliability.get(chartDate)); } addBugs += tempVal; }
								 */
								addBugs += val.longValue();
								reliability.put(chartDate, addBugs.toString());
							} else if (CodeGripConstants.VULNERABILITIES
									.equalsIgnoreCase(qualityCondition.getMetric())) {
								Double val = Double.parseDouble(qualityCondition.getValue());
								/**
								 * if (security.containsKey(chartDate)) { Long tempVal = 0L; if
								 * ("0.0".equals(security.get(chartDate))) { tempVal =
								 * (long)Double.parseDouble(security.get(chartDate)); } else { tempVal =
								 * Long.parseLong(security.get(chartDate)); } addVulnerabilities += tempVal; }
								 */
								addVulnerabilities += val.longValue();
								security.put(chartDate, addVulnerabilities.toString());
							} else if ("code_smells".equalsIgnoreCase(qualityCondition.getMetric())) {
								Double val = Double.parseDouble(qualityCondition.getValue());
								/**
								 * if (maintainability.containsKey(chartDate)) { Long tempVal = 0L; if
								 * ("0.0".equals(maintainability.get(chartDate))) { tempVal =
								 * (long)Double.parseDouble(maintainability.get(chartDate)); } else { tempVal =
								 * Long.parseLong(maintainability.get(chartDate)); } addCodeSmell += tempVal; }
								 */
								addCodeSmell += val.longValue();
								maintainability.put(chartDate, addCodeSmell.toString());

							}
						}
					}
				}
			}
		}

		chartData.put(CodeGripConstants.DUPLICATION_KEY, duplication);
		chartData.put(CodeGripConstants.MAINTAINABILITY_KEY, maintainability);
		chartData.put(CodeGripConstants.SECURITY_KEY, security);
		chartData.put(CodeGripConstants.RELIABILITY_KEY, reliability);
		return chartData;
	}

	/*************************************************************************************************
	 * Create empty map with date.
	 **************************************************************************************************/
	private LinkedHashMap<String, String> createEmpltyMapWithDate(Integer chartDays) {
		/* create one map of last 90 days with default value "0" */
		LinkedHashMap<String, String> chartData = new LinkedHashMap<>();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.add(Calendar.MINUTE, 0);
		LOGGER.info(cal);
		cal.add(Calendar.DAY_OF_YEAR, -chartDays);
		Double defaultValue = 0.0;
		for (int i = 0; i < chartDays; i++) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
			String datevalue = sdf.format(cal.getTime());
			try {
				String date1 = new SimpleDateFormat("MM/dd").format(sdf.parse(datevalue));
				chartData.put(date1, String.valueOf(defaultValue));
			} catch (ParseException e) {
				LOGGER.info(e);
			}
		}
		LOGGER.info(chartData);
		return chartData;
	}

	/**
	 * public static void main(String[] args) {
	 * SourceControlServiceImpl.createEmpltyMapWithDate(90); }
	 */

	/*************************************************************************************************
	 * Calculate card details data.
	 **************************************************************************************************/
	private AdminDashboardModel calculateCardDetails(List<Projects> projects) throws CustomException {
		AdminDashboardModel adminDashboardModel = new AdminDashboardModel();
		Long totalBlocker = 0L;
		Long totalSecurityCritical = 0L;
		Double totalPreviousSecurity = 0.0;
		int cnt = 0;
		Integer dayDiff = 0;
		List<Timestamp> dates = new ArrayList<>();
		List<String> projectKeys = new ArrayList<>();
		try {
			for (Projects project : projects) {
				for (ProjectBranch projectBranch : project.getProjectBranches()) {
					if ("master".equalsIgnoreCase(projectBranch.getBranchKey())) {
						cnt++;
						JSONObject blocker = getBlockerAndSecurityCriticalOfProject(projectBranch.getName(),
								BLOCKER_KEY, BLOCKER_KEY);
						totalBlocker += blocker.getInt("total");
						JSONObject securityCritical = getBlockerAndSecurityCriticalOfProject(projectBranch.getName(),
								"CRITICAL", CRITICAL_KEY);
						totalSecurityCritical += securityCritical.getInt("total");
						projectKeys.add(projectBranch.getName());

						// calculate previous rating of security.
						List<Quality> qualities = qualityDAO.getQualityDetails(projectBranch.getId());
						if (qualities.size() > 1) {
							totalPreviousSecurity += Double
									.parseDouble(calculatePreviousSecurityStatus(qualities.get(1)));
							dates.add(qualities.get(1).getAnalyzeAt());
						} else if (qualities.size() == 1) {
							totalPreviousSecurity += Double
									.parseDouble(calculatePreviousSecurityStatus(qualities.get(0)));
							dates.add(qualities.get(0).getAnalyzeAt());
						}

					}
				}
			}
			dates.sort(Comparator.reverseOrder());
			dayDiff = commonUtil.getDayDifference(dates.get(0), new Timestamp(System.currentTimeMillis()));

			adminDashboardModel
					.setSecurityPreviousStatus(getRatingAsPerValues(totalPreviousSecurity, cnt, MAINTAINABILITY_LABEL));
			adminDashboardModel.setSecurityPreviousStatusDay("" + dayDiff);
			if (totalBlocker > 0) {
				adminDashboardModel.setReliabilitySuggestion(totalBlocker.toString() + " Blocker bugs to ");
				adminDashboardModel.setReliabilitySuggestionRating("D");

			}
			if (totalSecurityCritical > 0) {
				adminDashboardModel.setSecuritySuggestion(totalSecurityCritical.toString() + " Critical to ");
				adminDashboardModel.setSecuritySuggestionRating("C");
			}

			adminDashboardModel = getAdminDashboardTotalIssues(adminDashboardModel, projectKeys, totalBlocker,
					totalSecurityCritical);

		} catch (Exception e) {
			LOGGER.info(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}

		return adminDashboardModel;
	}

	/*************************************************************************************************
	 * Calculate previous security status.
	 **************************************************************************************************/
	private String calculatePreviousSecurityStatus(Quality quality) {
		for (QualityCondition qualityCondition : quality.getQualityConditions()) {
			if ("vulnerabilities".equalsIgnoreCase(qualityCondition.getMetric()))
				return getRatingWithIndexValue(qualityCondition.getRating(), "reverse");
		}
		return "0.0";
	}

	/*************************************************************************************************
	 * Create admin dashboard card details.
	 **************************************************************************************************/
	private AdminDashboardModel getAdminDashboardTotalIssues(AdminDashboardModel adminDashboardModel,
			List<String> projectKeys, Long totalBlocker, Long totalSecurityCritical) throws CustomException {

		try {

			StringBuilder keys = new StringBuilder();
			for (String key : projectKeys) {
				keys.append("," + key);

			}
			JSONObject securityCritical = getCardDetails(keys.toString());
			Long totalBugs = 0L;
			Long totalVulnerabilities = 0L;
			Long totalCodeSmells = 0L;
			Double squaleRating = 0.0;
			Long sqaleIndex = 0L;
			JSONArray measures = securityCritical.getJSONArray("measures");

			for (int i = 0; i < measures.length(); i++) {
				JSONObject metric = measures.getJSONObject(i);
				if ("bugs".equals(metric.getString(METRIC_KEY)))
					totalBugs += Long.parseLong(metric.getString(VALUE_KEY));
				if (CodeGripConstants.VULNERABILITIES.equals(metric.getString(METRIC_KEY)))
					totalVulnerabilities += Long.parseLong(metric.getString(VALUE_KEY));
				if ("code_smells".equals(metric.get(METRIC_KEY)))
					totalCodeSmells += Long.parseLong(metric.getString(VALUE_KEY));
				if ("sqale_rating".equals(metric.get(METRIC_KEY)))
					squaleRating += Double.parseDouble(metric.getString(VALUE_KEY));
				if ("sqale_index".equals(metric.get(METRIC_KEY)))
					sqaleIndex += Long.parseLong(metric.getString(VALUE_KEY));

			}
			adminDashboardModel.setTotalBugs(totalBugs.toString() + " bugs");
			adminDashboardModel.setVulnerabilities(totalVulnerabilities.toString() + " vulnerabilities");
			adminDashboardModel.setCodeSmells(totalCodeSmells.toString() + " code smells");
			adminDashboardModel.setMaintainabilityRating(
					getRatingAsPerValues(squaleRating, projectKeys.size(), MAINTAINABILITY_LABEL));
			adminDashboardModel.setTechnicalDebt(caluateEfforts(sqaleIndex.intValue()) + " to technical dept");

			// calculate reliability rating
			Map<String, String> ratings = getReliabilityRatingAndSecurityRating(totalBlocker, totalSecurityCritical,
					projectKeys);
			if (ratings != null) {
				adminDashboardModel.setReliabilityRating(ratings.get(BLOCKER_RATING_KEY));
				adminDashboardModel.setSecurityRating(ratings.get(SECURITY_RATING_KEY));
			}
			
			if(adminDashboardModel.getMaintainabilityRating()==null)
				adminDashboardModel.setMaintainabilityRating("A");

			return adminDashboardModel;
		} catch (IOException | JSONException e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	/*************************************************************************************************
	 * Calculate squale rating.
	 **************************************************************************************************/
	private String getRatingAsPerValues(Double squaleRating, int length, String metric) {
		String maintainabilityRating = "";
		if (MAINTAINABILITY_LABEL.equalsIgnoreCase(metric))
			maintainabilityRating = environment.getProperty(CodeGripConstants.GET_MAINTAINABILITY_RATING);
		Map<String, String> map = Pattern.compile("\\s*-\\s*").splitAsStream(maintainabilityRating.trim())
				.map(s -> s.split("~", 2)).collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
		LOGGER.info(map);
		Double val = squaleRating / length;
		return map.get(val.toString());
	}

	/*************************************************************************************************
	 * Get rating with index value.
	 **************************************************************************************************/
	private String getRatingWithIndexValue(String squaleRating, String val) {
		String maintainabilityRating = "";
		if (!val.equalsIgnoreCase("reverse"))
			maintainabilityRating = environment.getProperty(CodeGripConstants.GET_MAINTAINABILITY_RATING);
		else
			maintainabilityRating = environment.getProperty(CodeGripConstants.GET_MAINTAINABILITY_RATING_REVERSE);
		Map<String, String> map = Pattern.compile("\\s*-\\s*").splitAsStream(maintainabilityRating.trim())
				.map(s -> s.split("~", 2)).collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
		LOGGER.info(map);
		return map.get(squaleRating);
	}

	/*************************************************************************************************
	 * Get reliability and security rating .
	 **************************************************************************************************/
	private Map<String, String> getReliabilityRatingAndSecurityRating(Long totalBlocker, Long totalSecurityCritical,
			List<String> projectKeys) throws CustomException {
		Map<String, String> ratings = new HashMap<>();
		Boolean blockerFlag = false;
		Boolean securityFlag = false;

		if (totalBlocker > 0) {
			blockerFlag = true;
			ratings.put(BLOCKER_RATING_KEY, "E");
		}
		if (totalSecurityCritical > 0) {
			securityFlag = true;
			ratings.put(SECURITY_RATING_KEY, "D");
		}
		try {
			for (String projectName : projectKeys) {
				if (!blockerFlag) {
					JSONObject critical = getBlockerAndSecurityCriticalOfProject(projectName, BLOCKER_KEY,
							CRITICAL_KEY);
					if (critical.getInt("total") > 0) {
						blockerFlag = true;
						ratings.put(BLOCKER_RATING_KEY, "D");
					} else {
						JSONObject major = getBlockerAndSecurityCriticalOfProject(projectName, BLOCKER_KEY, "MAJOR");
						if (major.getInt("total") > 0) {
							blockerFlag = true;
							ratings.put(BLOCKER_RATING_KEY, "C");
						} else {
							JSONObject minor = getBlockerAndSecurityCriticalOfProject(projectName, BLOCKER_KEY,
									"MINOR");
							if (minor.getInt("total") > 0) {
								blockerFlag = true;
								ratings.put(BLOCKER_RATING_KEY, "B");
							} else {
								blockerFlag = true;
								ratings.put(BLOCKER_RATING_KEY, "A");
							}
						}
					}
				}
				if (!securityFlag) {
					JSONObject major = getBlockerAndSecurityCriticalOfProject(projectName, CRITICAL_KEY, "MAJOR");
					if (major.getInt("total") > 0) {
						securityFlag = true;
						ratings.put(SECURITY_RATING_KEY, "C");
					} else {
						JSONObject minor = getBlockerAndSecurityCriticalOfProject(projectName, CRITICAL_KEY, "MINOR");
						if (minor.getInt("total") > 0) {
							securityFlag = true;
							ratings.put(SECURITY_RATING_KEY, "B");
						} else {
							securityFlag = true;
							ratings.put(SECURITY_RATING_KEY, "A");
						}
					}
				}
			}
		} catch (IOException | JSONException e) {
			LOGGER.info(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		return ratings;
	}

	/*************************************************************************************************
	 * Get project brief details.
	 **************************************************************************************************/
	private JSONObject getCardDetails(String keys) throws IOException, JSONException {
		String url = "";
		url = environment.getProperty(CodeGripConstants.GET_PROJECT_BRIEF_DETAILS);
		url = url.replace("<PROJECT_KEYS>", keys.substring(1));
		return new JSONObject(projectService.callGetAPI(url).toString());
	}

	/*************************************************************************************************
	 * Get blocker details from .
	 **************************************************************************************************/
	@Override
	public JSONObject getBlockerAndSecurityCriticalOfProject(String projectName, String severity, String severityVal)
			throws IOException, JSONException {
		String url = "";
		if (BLOCKER_KEY.equalsIgnoreCase(severity)) {
			url = environment.getProperty(CodeGripConstants.GET_BLOCKER_DETAILS);
			url = url.replace(CodeGripConstants.SEVERITY, severityVal);
		} else {
			url = environment.getProperty(CodeGripConstants.GET_CRITICAL_DETAILS);
			url = url.replace(CodeGripConstants.SEVERITY, severityVal);
		}
		url = url.replace(CodeGripConstants.PROJECT_KEY, projectName);
		return new JSONObject(projectService.callGetAPI(url).toString());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deleteProject(List<Projects> projectList) {
		try {
			for (Projects project : projectList) {
				List<UsersAccountDetails> userAccountDetailsList = (List<UsersAccountDetails>) objectDao.listObjectByParam(UsersAccountDetails.class,"userId", project.getUserId());
				for(UsersAccountDetails userAccount: userAccountDetailsList) {
					if(userAccount.getSourceControlName().equals("bitbucket")) {
						bitbucketAPIUtil.listWebhookFromBitbucket(project, userAccount);
					}
				}
				
				List<ProjectBranch> projectBranch = (List<ProjectBranch>) objectDao.listObjectByParam(ProjectBranch.class,
						"projects", project);

				if (null != projectBranch) {
					for (ProjectBranch proBranch : projectBranch) {
						List<Quality> qualitys = (List<Quality>) objectDao.listObjectByParam(Quality.class,
								"projectBranchId", proBranch.getId());
						if (null != qualitys) {
							for (Quality quality : qualitys) {
								List<QualityCondition> qualityConditions = (List<QualityCondition>) objectDao
										.listObjectByParam(QualityCondition.class, "quality", quality);
								if (null != qualityConditions) {
									for (QualityCondition qualityCondition : qualityConditions) {
										objectDao.deleteObject(qualityCondition);
									}
								}

								objectDao.deleteObject(quality);
							}
						}
						objectDao.deleteObject(proBranch);
					}

				}
				objectDao.deleteObject(project);
			}
		}catch(Exception e) {
			LOGGER.info(e.getMessage());
		}
		
	}

	/**
	 * @Override public String getFileContnet(SourceControlModel sourceControlModel)
	 *           throws CustomException,JSONException{
	 * 
	 * 
	 *           LOGGER. info("<------------------------------------------GET FILE
	 *           CONTENT METHOD-------------------------------->" ); JSONObject
	 *           accessToken=new JSONObject(); UsersAccountDetails
	 *           usersAccountDetails = new UsersAccountDetails();
	 *           if(sourceControlModel.getName().equals("github")) { accessToken =
	 *           githubAPIUtil.getNewAccesstokenGithub(sourceControlModel.getCode(),
	 *           CodeGripConstants.CODE); }
	 *           usersAccountDetails.setAccessToken(accessToken.getString(CodeGripConstants.
	 *           ACCESS_TOKEN));
	 * 
	 *           LOGGER.info("ACCESS_TOKEN----------------"+usersAccountDetails.getAccessToken
	 *           ());
	 * 
	 *           String fileContentData="";
	 *           if(sourceControlModel.getName().equals("github")) {
	 *           //fileContentData=githubAPIUtil.getGitHubFileContent(usersAccountDetails.
	 *           getAccessToken(),sourceControlModel.getUsername()); }
	 * 
	 *           with access token get user data
	 * 
	 * 
	 *           return fileContentData; }
	 */
}
