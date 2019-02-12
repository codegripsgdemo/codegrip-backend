package com.mb.codegrip.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.dao.ProjectDAO;
import com.mb.codegrip.dto.CustomPlan;
import com.mb.codegrip.dto.ProjectBranch;
import com.mb.codegrip.dto.Projects;
import com.mb.codegrip.dto.Quality;
import com.mb.codegrip.dto.UsersAccountDetails;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.mail.MailerHelper;
import com.mb.codegrip.model.CommonCommitModel;
import com.mb.codegrip.model.CustomPlanModel;
import com.mb.codegrip.model.ProjectsModel;
import com.mb.codegrip.repository.CustomPlanRepository;
import com.mb.codegrip.repository.ProjectBranchRepository;
import com.mb.codegrip.repository.QualityRepository;
import com.mb.codegrip.repository.UsersAccountDetailsRepository;
import com.mb.codegrip.utils.BitbucketAPIUtil;
import com.mb.codegrip.utils.GitLabAPIUtil;
import com.mb.codegrip.utils.GithubAPIUtil;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

@Service
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:messages.properties"),@PropertySource("classpath:notifications.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class WebhookServiceImpl implements WebhookService, EnvironmentAware {

	private static Environment environment;
	private static final Logger LOGGER = Logger.getLogger(WebhookServiceImpl.class);
	GithubAPIUtil githubAPIUtil = new GithubAPIUtil();
	BitbucketAPIUtil bitbucketAPIUtil = new BitbucketAPIUtil();
	GitLabAPIUtil gitLabAPIUtil = new GitLabAPIUtil();
	ProjectServiceImpl projectServiceImpl = new ProjectServiceImpl();

	@Autowired
	private ProjectDAO projectDAO;
	
	Mapper mapper = new DozerBeanMapper();
	
	@Autowired
	private MailerHelper mailerHelper;


	@Autowired
	private ProjectBranchRepository projectBranchRepository;
	
	@Autowired
	private CustomPlanRepository customPlanRepository;

	@Autowired
	private UsersAccountDetailsRepository usersAccountDetailsRepository;

	@Autowired
	private QualityRepository qualityRepository;
	
	@Value("${stripe.api.key}")
	private String stripeApiKey;


	public String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	/*********************************************************************************************
	 * Save commit details and start project analyze.
	 * 
	 * @throws JsonProcessingException
	 * @throws JSONException 
	 *********************************************************************************************/
	@Override
	public void saveCommitDetails(Object webhookObject, HttpServletRequest request, String sourceControlName)
			throws CustomException, JsonProcessingException, JSONException {
		JSONObject jsonObject = new JSONObject();
		if (webhookObject != null) {
			jsonObject = objectToJSONObject(webhookObject);
			if (CodeGripConstants.GITHUB_PROVIDER.equals(sourceControlName))
				saveSourceControlCommitDetails(githubAPIUtil.createCommitJSONofGithubWebhook(jsonObject),
						sourceControlName);
			else if (CodeGripConstants.BITBUCKET_PROVIDER.equals(sourceControlName))
				saveSourceControlCommitDetails(bitbucketAPIUtil.createCommitJSONofBitbucketWebhook(jsonObject),
						sourceControlName);
			else if (CodeGripConstants.GITLAB_PROVIDER.equals(sourceControlName))
				saveSourceControlCommitDetails(gitLabAPIUtil.createCommitJSONofGitlabWebhook(jsonObject),
						sourceControlName);
		} else {
			LOGGER.info("Webhook data is null");
		}
		LOGGER.info("Github Commit data: " + jsonObject);
	}

	/**************************************************************************************************
	 * Convert object to JSONObject method.
	 **************************************************************************************************/
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static org.json.JSONObject convertObjectToJSONObject(Object object) {
		org.json.JSONObject webhookObject = new org.json.JSONObject();
		if (object instanceof Map) {
			((Map) webhookObject).putAll((Map) object);
			LOGGER.info(webhookObject.toString());
		}
		return webhookObject;
	}

	/**************************************************************************************************
	 * Create project branch object.
	 **************************************************************************************************/
	private Quality createQualityObject(CommonCommitModel commonCommitModel, ProjectBranch dbProjectBranch,
			String projectKey) {
		Quality quality = new Quality();
		quality.setProjectKey(projectKey);
		quality.setProjectName(commonCommitModel.getProjectName());
		quality.setCommitterName(commonCommitModel.getPusherName());
		quality.setCommitId(commonCommitModel.getCommitId());
		quality.setProjectBranchId(dbProjectBranch.getId());
		return quality;
	}

	/**************************************************************************************************
	 * Create project branch object.
	 **************************************************************************************************/
	private ProjectBranch createProjectBranchData(String projectKey, CommonCommitModel githubCommitModel,
			Projects dbProjects) {
		ProjectBranch projectBranch = new ProjectBranch();
		projectBranch.setBranchKey(githubCommitModel.getBranchName());
		projectBranch.setName(projectKey);
		projectBranch.setProjects(dbProjects);
		projectBranch.setIsDeleted(false);
		return projectBranch;
	}

	/**************************************************************************************************
	 * Save source control commit details.
	 **************************************************************************************************/
	private void saveSourceControlCommitDetails(CommonCommitModel githubCommitModel, String sourceControl)
			throws CustomException {
		try {
			Projects dbProjects = projectDAO.getStartedProjectByUidAndUsername(githubCommitModel);
			ProjectBranch dbProjectBranch = projectDAO.getProjectBranchByBranchKeyAndProjectId(dbProjects,
					githubCommitModel);
			String projectKey = "";
			if (dbProjectBranch == null) {
				projectKey = githubCommitModel.getProjectName() + ":" + githubCommitModel.getBranchName()
						+ CodeGripConstants.SEPERATOR + githubCommitModel.getCommitId() + CodeGripConstants.SEPERATOR
						+ dbProjects.getCompanyId();
				dbProjectBranch = projectBranchRepository
						.save(createProjectBranchData(projectKey, githubCommitModel, dbProjects));
			} else {
				projectKey = dbProjectBranch.getName();
			}
			UsersAccountDetails usersAccountDetails = usersAccountDetailsRepository
					.findByUserIdAndCompanyIdAndSourceControlNameAndAccountUsername(dbProjects.getUserId(), dbProjects.getCompanyId(),
							sourceControl, dbProjects.getUserName());
			org.json.JSONObject accessTokens = new org.json.JSONObject();
			accessTokens.put(CodeGripConstants.ACCESS_TOKEN, usersAccountDetails.getAccessToken());
			accessTokens.put(CodeGripConstants.PROVIDER, CodeGripConstants.GITHUB_PROVIDER);

			Quality quality = qualityRepository
					.save(createQualityObject(githubCommitModel, dbProjectBranch, projectKey));
			LOGGER.info("Sonar project key: " + projectKey);
			LOGGER.info("Sonar project name: " + githubCommitModel.getProjectName() + CodeGripConstants.SEPERATOR + githubCommitModel.getCommitId()
			+ CodeGripConstants.SEPERATOR + quality.getId());
			projectServiceImpl.startProjectScanning(dbProjects.getGitCloneUrl(),
					githubCommitModel.getProjectName() + CodeGripConstants.SEPERATOR + githubCommitModel.getCommitId(),
					accessTokens, projectKey, githubCommitModel.getBranchCloningKey(), quality.getId(), mapper.map(dbProjects, ProjectsModel.class));

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
	}

	
	
	/**************************************************************************************************
	 * Object to JSONObject convertor.
	 * @throws JSONException 
	 **************************************************************************************************/
	public static JSONObject objectToJSONObject(Object object) throws JsonProcessingException, JSONException {
		ObjectMapper mapperObj = new ObjectMapper();
		String jsonStr = mapperObj.writeValueAsString(object);
		return new JSONObject(jsonStr);
	}

	
	/**************************************************************************************************
	 * Stripe charged webhook.
	 * @throws JSONException 
	 **************************************************************************************************/
	@SuppressWarnings("unchecked")
	@Override
	public void stripeChargeWebhook(HttpServletRequest request) throws CustomException {
		String rawJson = "";
		Stripe.apiKey = stripeApiKey;
		String sigHeader = request.getHeader("Stripe-Signature");
		Event event = null;
		try {
			rawJson = IOUtils.toString(request.getInputStream());
			event = Webhook.constructEvent(rawJson, sigHeader,
					environment.getProperty(CodeGripConstants.STRIPE_SIGNING_SECRET));

		} catch (IOException ex) {
			LOGGER.error(ex.getMessage());
		} catch (JsonSyntaxException e) {
			LOGGER.error(e.getMessage());
		} catch (SignatureVerificationException e) {
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.STRIPE_SIGNATURE_NOT_VALID));
		}
		event = Event.GSON.fromJson(rawJson, Event.class);
		LOGGER.info("stripe webhook" + event);
		// Converting event object to map
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> props = objectMapper.convertValue(event.getData(), Map.class);

		// Getting required data
		Object dataMap = props.get("object");
		Map<String, String> dataMapper = objectMapper.convertValue(dataMap, Map.class);
		try {
			Object source = dataMapper.get(CodeGripConstants.SOURCE);
			objectMapper.convertValue(source, Map.class);
		   // Payment payment = new 

		} catch (Exception exc) {
			LOGGER.error(exc.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG));
		}
		
	}

	
	/*****************************************************************************************************
	 * Send mail to super admin when new user registration to portal
	 *************************************************************************************************/
	@Override
	public void contactForCustomPlan(HttpServletRequest request, CustomPlanModel customPlanModel) {
		customPlanRepository.save(mapper.map(customPlanModel, CustomPlan.class));
		sendCustomPlanInquiryEmail(customPlanModel);
	}
	
	
	
	/*****************************************************************************************************
	 * Send mail to super admin regarding custom plan inquiry.
	 *************************************************************************************************/
	public void sendCustomPlanInquiryEmail(CustomPlanModel customPlanModel) {
		ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		emailExecutor.execute(() -> mailerHelper.sendCustomPlanInquiryEmail(customPlanModel));
		emailExecutor.shutdown();

	}

}
