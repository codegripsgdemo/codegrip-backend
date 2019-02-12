package com.mb.codegrip.constants;

public class CodeGripConstants {

	private CodeGripConstants() {

	}

	// Static data
	public static final Integer REPO_PAGE_SIZE = 25;
	public static final String USER_ROLE = "ROLE_USER";
	public static final String ADMIN_ROLE = "ROLE_ADMIN";
	public static final String ACCESS_TOKEN = "access_token";
	public static final String USERNAME = "username";
	public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
	public static final String CONTROLLER_DATE_FORMAT = "MM/dd/yyyy h:mm:ss a";
	public static final String RESULT = "Result";
	public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
	public static final String APPLICATION_JSON = "application/json";
	public static final String GOOGLE_PROVIDER = "google";
	public static final String BITBUCKET_PROVIDER = "bitbucket";
	public static final String PROVIDER = "provider";
	public static final String GITHUB_PROVIDER = "github";
	public static final String GITLAB_PROVIDER = "gitlab";
	public static final String EMAIL_PROVIDER = "email";
	public static final String USER_DATA = "userData";
	public static final String TOKEN_DATA = "tokenData";
	public static final String CONNECTED_ACCOUNTS = "connectedAccounts";
	public static final String STARTED_REPO_LIST = "startedRepositoryList";
	public static final String ALL = "all";
	public static final Integer ZERO = 0;
	public static final Integer ONE = 1;
	public static final Integer TWENTY = 20;
	public static final String TEAM_CODEGRIP = "Team CodeGrip";
	public static final String USER_NAME_KEY = "<USER_NAME>";
	public static final String ACCESS_TOKEN_KEY = "<ACCESS_TOKEN>";
	public static final String VALUES = "values";
	public static final String LINKS = "links";
	public static final String CG_SCANNER_FOLDER_NAME = "CG-Scanner";
	public static final String SEPERATOR = "--CG--";
	public static final String EMAIL = "email";
	public static final String NODE_ID = "node_id";
	public static final String MASTER = "master";
	public static final String MONTH = "month";
	public static final String HOURS = "hours";
	public static final String MINUTES = "minutes";
	public static final String SONAR_BUILD_NUMBER_KEY = "sonar.analysis.buildNumber";
	public static final String USER_AVATAR = "user.avatar";
	public static final String COMPANY_AVATAR = "company.avatar";
	public static final String COMPANY_ID = "companyId";
	public static final String USER_ID = "userId";
	public static final Integer EXPIRING_DAY = 72;
	public static final String SSH_FILE_NAME = "id_rsa.pub";
	public static final String CODEGRIP = "Codegrip";
	public static final String LOGIN_PAGE_URL = "login_portal_url";
	public static final String AUTHOR = "author";
	public static final String SEVERITY = "<SEVERITY>";
	public static final String BLOCKER_RATING_KEY = "blockerRating";
	public static final String SECURITY_RATING_KEY = "securityRating";
	public static final String BLOCKER_KEY = "BLOCKER";
	public static final String CRITICAL_KEY = "CRITICAL";
	public static final String METRIC_KEY = "metric";
	public static final String VALUE_KEY = "value";
	public static final Integer CHART_DAYS = 90;
	public static final Integer SG_TRIAL_PERIOD_DAYS = 90;
	public static final Integer STARTUP_PROJECT_LIMIT = 2;

	// Stripe static data
	public static final String PLAN_NAME = "plan_name";
	public static final String SOURCE = "source";
	public static final String CUSTOMER = "customer";
	public static final String NICKNAME = "nickname";
	public static final String PRODUCT = "product";
	public static final String AMOUNT = "amount";
	public static final String STRIPE_CUSTOMER_ID = "stripeCustomerId";
	public static final Object STRIPE_FUNDING_STRING = "funding";
	public static final String STRIPE_QUANTITY_STRING = "quantity";
	public static final String STRIPE_SUBSCRIPTION_ID = "stripeSubscriptionId";
	public static final String STRIPE_PLAN_ID = "stripePlanId";
	public static final Object TRAIL_PERIOD_DAYS = 30;
	
/**	public static final Object TRAIL_PERIOD_DAYS = 1;*/	
	public static final String COMPANY = "company";
	public static final String PLAN = "plan";
	public static final String STRIPE_ID_STRING = "id";
	public static final String ITEMS = "items";

	// Notification data.
	public static final String CG_TOUR = "tour";
	public static final String WELCOME_NOTIFICATION = "Welcome to CodeGrip! Take a quick tour.";
	public static final String NEW_SIGNUP = "New signup";
	public static final String WELCOME_ABORD = "Welcome abord.";
	public static final String UNREAD = "UNREAD";

	public static final String STARTUP = "startup.product";
	public static final Integer STARTUP_DAYS = 30;
	public static final Integer LINK_EXPIRE_HOUR = 24;
	public static final String ASSIGN_PROJECT_TITLE = "assign.project.title";
	public static final String ASSIGN_PROJECT_MESSAGE = "assign.project.message";
	public static final String ASSIGN_PROJECT_REASON = "assign.project.reason";
	public static final String ASSIGN_PROJECT_DESTINATION_PAGE = "assign.project.destination.page";

	public static final String USER_SIGNUP_MESSAGE = "user.signup.message";
	public static final String USER_SIGNUP_REASON = "user.signup.reason";
	public static final String USER_SIGNUP_TITLE = "user.signup.title";
	public static final String USER_SIGNUP_DESTINATION_PAGE = "user.signup.destination.page";

	public static final String EXPIRING_SUBSCRIPTION_MESSAGE = "expiring.subscription.message";
	public static final String EXPIRING_SUBSCRIPTION_REASON = "expiring.subscription.reason";
	public static final String EXPIRING_SUBSCRIPTION_TITLE = "expiring.subscription.title";
	public static final String EXPIRING_SUBSCRIPTION_DESTINATION_PAGE = "expiring.subscription.destination.page";
	public static final String ADMIN_SHARED_DASHBOARD = "admin.shared.dashboard";
	public static final String ASSIGNED_TO_NEW_PROJECT = "shared.to.new.project";
	public static final String PAYMENT_FAILURE_MESSAGE = "payment.failure.msg";

	// notification images.
	public static final Integer IMAGE_BROADCAST = 1;
	public static final Integer IMAGE_NEW_PROJECT_ADDED = 2;
	public static final Integer IMAGE_NEW_USER_JOINED = 3;
	public static final Integer IMAGE_SHARE_DASHBOARD = 4;
	public static final Integer IMAGE_USER_SUSPEND = 5;
	public static final Integer IMAGE_PAYMENT_FAILED = 6;
	public static final Integer IMAGE_PAYMENT_SUCCESS = 7;
	public static final Integer IMAGE_CANCEL_SUBSCRIPTION = 8;

	// Client credentials
	public static final String CLIENT_ID = "app.client.id";
	public static final String CLIENT_SECRET = "app.client.secret";
	public static final String CLIENT_PASS = "app.client.password";

	// Sendgrid details.
	public static final String EMAIL_FORMAT_TYPE = "text/plain";
	public static final String SENDGRID_EMAIL = "sendgrid.email";
	public static final String SENDGRID_API_KEY = "spring.sendgrid.api-key";
	public static final String SENDGRID_ENDPOINT = "mail/send";
	public static final String SENDGRID_SUBJECT = "sendgrid.subject";
	public static final String SENDGRID_MESSAGE = "sendgrid.message";
	public static final String SENDGRID_TOKEN = "sendgrid.token";
	public static final String SEND_PROJECT_TEMPLATE_ID = "send.project.template.id";
	public static final String SENDGRID_CUSTOM_INQUIRY_EMAIL_SUBJECT = "sendgrid.custom.inquiry.email.subject";
	public static final String SENDGRID_CUSTOM_PLAN_INQUIRY_ID = "custom.plan.inquiry.id";
	public static final String PAYMENT_FAILURE_ID_SA = "payment.failure.id.sa";
	public static final String PAYMENT_FAILURE_ID_ADMIN = "payment.failure.id.admin";
	public static final String SENDGRID_EMAIL_MANAGER = "manager.email";
	public static final String SEDNGRID_ORG_NAME = "org_name";
	public static final String SEDNGRID_ORG_EMAIL = "org_admin_username_or_email";
	public static final String SENDGRID_NEW_SUBSCRIPTION_PLAN = "new_subscription_plan";
	public static final String SENDGRID_DATE_AND_TIME = "date_and_time";
	public static final String SENDGRID_END_DATE_AND_TIME = "end_date_and_time";
	public static final String SENDGRID_AMOUNT_BILLED = "amount_billed";
	public static final String BILLING_DATE = "billing_date";
	public static final String UPGRADE_PLAN_TEMPLATE_URL = "upgrade.plan.template.url";
	public static final String SENDGRID_SSGLOBAL_INVITE_TEMPLATE = "sendgrid.ssglobal.invite-template";

	// Landing page details.
	public static final String LANDING_PAGE_USER_SUBJECT = "landing.page.user.subject";
	public static final String LANDING_PAGE_USER_MESSAGE = "landing.page.user.message";
	public static final String ADDED_PROJECT_DASHBOARD_URL = "added.project.dashboard.url";

	public static final String ANALYSIS_SUBJECT = "analysis.subject";
	//
	public static final String OAUTH_URL = "oauth.url";

	// Exception Messages
	public static final String NOT_FOUND = "exception.not.found";
	public static final String SEND_ERROR_WARNING_MAIL = "send.error.warning.mail";
	public static final String USER_ALREADY_EXIST_EXCEPTION = "email.already.exist";
	public static final String LOG_IN_FAILED = "Invalid Credentials.";
	public static final String SOMETHING_WENT_WRONG = "something.went.wrong";
	public static final String INCORRECT_PASS = "incorrect.password";
	public static final String EMAIL_NOT_REGISTERED = "email.not.registered";
	public static final String SERVER_ERROR = "server.error.";
	public static final String UNAUTHORIZED = "unauthorized";
	public static final String NOT_ACCEPTABLE = "not.acceptable";
	public static final String SUCCESS = "success";
	public static final String RESOURCE_NOT_FOUND = "resource.not.found";
	public static final String CREATED = "created";
	public static final String UNSUPPORTED_TYPE = "unsupported.type";
	public static final String INVALID_EMAIL_OR_PASS = "invalid.email.or.password";
	public static final String POST_API_CALL_FAILED = "failed.to.call.post.api";
	public static final String REACHED_ADDING_REPO_LIMIT = "exception.reached.adding.repo.limit";
	public static final String REQUEST_NOT_AUTHORISED = "exception.request.not.authorised";
	public static final String NO_COMMIT_DETAILS_FOUND = "no.commit.details.found";
	public static final String PAGE_NO_REQUIRED = "page.no.required";
	public static final String EMAIL_NOT_FOUND = "email.not.found";
	public static final String SECURITY_REPORT_NOT_FOUND = "security.report.not.found";
	public static final String PROJECT_KEY_NOT_EMPTY = "projectkey.not.empty";
	public static final String FAILED_TO_ADD_SSH = "failed.to.add.ssh";
	public static final String SSH_FILE_UNABLE_TO_READ = "ssh.file.unable.to.read";
	public static final String SOMETHING_WENT_WRONG_WHILE_CLONING = "something.went.wrong.while.cloning";
	public static final String PROJECT_ASSIGNED_SUCCESS = "project.assigned.success";
	public static final String ADMIN_NOT_BELONGS_TO_PROJECT = "admin.not.belongs.to.project";
	public static final String LINK_EXPIRED = "link.expired";
	public static final String USER_DEACTIVATE = "user.deactivate";
	public static final String ADMIN_DEACTIVATE = "admin.deactivate";
	public static final String PAYMENT_FAILURE = "payment.failure";
	public static final String USER_LIMIT_EXCEEDED = "user.limit.exceeded";
	public static final String INVITE_CANCELLED_BY_ADMIN = "invite.cancelled.by.admin";
	public static final String USER_LIMIT_REACHED_AS_PER_PLAN = "user.limit.reached";
	public static final String NO_PROJECTS_FOUND = "no.projects.found";
	public static final String PROJECT_LIMIT_REACHED = "reached.project.limit";
	public static final String ACCOUNT_EXPIRED = "account.expired";
	public static final String INVALID_EMAIL = "invalid.email";
	public static final String SSH_UPLOAD_ISSUE_SUBJECT = "ssh.upload.issue.subject";

	// Success Messages
	public static final String USER_REGISTERED_SUCCESSFULLY_MESSAGE = "user.registered.success";
	public static final String USER_LOGGED_IN_SUCCESSFULL_MESSAGE = "success.logged.in";
	public static final String NO_CONTENT = "no.content";
	public static final String REPOSITORY_LIST_SENT_SUCCESS = "repository.list.sent.success";
	public static final String USER_UPDATED_SUCCESS = "user.update.success";
	public static final String ALREADY_CODEGRIP_USER = "already.codegrip.user";
	public static final String PROJECT_LIST_SENT_SUCCESS = "project.list.sent.success";
	public static final String SONAR_QUALITY_WEBHOOK_SAVE_SUCCESS = "sonar.quality.saved.success";
	public static final String ACCOUNT_LIST_SENT_SUCCESS = "account.list.sent.success";
	public static final String PROGRESS_LIST_SENT_SUCCESS = "progress.list.sent.success";
	public static final String COMMIT_DATA_SENT_SUCCESS = "commit.data.sent.success";
	public static final String SAVE_COMMIT_DETAILS_SUCCESS = "save.commit.details";
	public static final String NOTIFICATION_LIST_SENT_SUCCESSFULLY = "notification.list.sent.success";
	public static final String NOTIFICATION_UPDATED_SUCCESSFULLY = "notification.updated.success";
	public static final String EMAIL_FOUND = "email.found";
	public static final String SECURITY_REPORT_FOUND = "security.report.found";
	public static final String EMAIL_UPDATED_SUCCESSFULLY = "email.updated.successfully";
	public static final String RECORD_UPDATED_SUCCESS = "record.updated.success";
	public static final String COMMIT_DATA_SAVED_SUCCESS = "commit.data.saved.success";
	public static final String USER_REMOVED_FROM_PROJECT = "remove.user.from.project";
	public static final String PROJECT_DELETED_SUCCESSFULLY = "project.deleted.successfully";
	public static final String USER_ROLE_CHANGED_SUCCESS = "user.role.changed.success";
	public static final String CUSTOM_PLAN_REQUEST_SENT_SUCCESS = "custom.plan.request.sent.success";
	public static final String LOGOUT_SUCCESSFULLY = "logout.successfully";
	public static final String RECORD_DELETED_SUCCESS = "record.deleted.success";
	public static final String DASHBOARD_DATA_GET_SUCCESS = "dashboard.data.sent.success";
	public static final String IMAGE_DELETED_SUCCESS = "image.delete.success";
	public static final String DASHBOARD_DATA_SENT_SUCCESS = "dashboard.data.sent.success";

	// GitHub Credentials
	public static final String GITURL = "https://github.com";

	/**
	 * Third party data.
	 */
	// Bitbucket
	public static final String BITBUCKET_KEY = "bitbucket.key";
	public static final String BITBUCKET_SECRET = "bitbucket.secret";
	public static final String ACCESS_TOKEN_URL = "access.token.url";
	public static final String BITBUCKET_ACCESS_CODE = "bitbucket.get.code";
	public static final String BITBUCKET_GET_USERNAME_URL = "bibucket.username.url";
	public static final String BITBUCKET_GET_REOSITORIES_URL = "bitbucket.get.repolist.url";
	public static final String GET_COMMIT_URL = "commit.url";
	public static final String BITBUCKET_ADD_WEBHOOK = "bitbucket.add.webhook.url";
	public static final String BITBUCKET_GET_EMAIL = "bitbucket.email.url";
	public static final String ADD_SSH_OVER_BITBUCLET = "bitbucket.add.ssh.url";
	public static final String BITBUCKET_COMMIT_WEBHOOK = "bitbucket.commit.webhook.url";

	// Github
	public static final String GITHUB_ACCESS_TOKEN_URL = "github.access.token.url";
	public static final String GITHUB_CLIENT_ID = "github.client.id";
	public static final String GITHUB_SECRET = "github.client.secret";
	public static final String GITHUB_SCOPE = "github.scope";
	public static final String GITHUB_USER_URL = "github.user.data";
	public static final String GTIHUB_GET_EMAIL = "github.get.email";
	public static final String GITHUB_COMMIT_WEBHOOK_URL = "github.commit.webhook.url";
	public static final String GITHUB_FILE_URL = "github.file.url";
	public static final String PROJECT_FILE_SENT_SUCCESS = "project.file.sent.success";
	public static final String GITHUB_REPO_COMMIT_URL = "github.repo.commit.url";
	public static final String GITHUB_ADD_WEBHOOK = "github.add.webhook.url";

	// GitLab
	public static final String GITLAB_ACCESS_TOKEN_URL = "gitlab.access.token.url";
	public static final String GITLAB_CLIENT_ID = "gitlab.client.id";
	public static final String GITLAB_CLIENT_SECRET = "gitlab.client.secret";
	public static final String GITLAB_USER_INFO = "gitlab.user.info";
	public static final String GET_ID_FROM_ACCESS_TOKEN = "get.id.from.access.token";
	public static final String ERROR_IN_GETTING_ACCESS_TOKEN = "error.in.getting.access.token";
	public static final String GITLAB_PROJECT_LIST_API = "gitlab.project.list.api";
	public static final String GITLAB_COMMIT_URL = "gitlab.commit.details";
	public static final String GITLAB_ADD_WEBHOOK = "gitlab.add.webhook.url";
	public static final String ADD_SSH_OVER_GITLAB_REPO = "gitlab.add.ssh.repo";

	// swagger
	public static final String SWAGGER_CORS_URL = "swagger.cors.url";
	public static final String SWAGGER_BASE_PACKAGE = "swagger.basepackage";
	public static final String SWAGGER_TITLE = "swagger.title";
	public static final String SWAGGER_DESCRIPTION = "swagger.description";
	public static final String SWAGGER_TERMSOFSERVICE = "swagger.termsofservice";
	public static final String SWAGGER_DEVELOPER_CONTACT_NAME = "swagger.developer.contact.name";
	public static final String SWAGGER_CONTACT_EMAIL = "swagger.contact.email";
	public static final String SWAGGER_LICENSE_URL = "swagger.license.url";
	public static final String SWAGGER_VERSION = "swagger.version";
	public static final String SWAGGER_LICENSE_TYPE = "Private";
	public static final String READ = "read";
	public static final String WRITE = "write";
	public static final String READ_DETAILS = "for read operations.";
	public static final String WRITE_DETAILS = "for write operations.";
	public static final String SPRING_OAUTH = "spring_oauth";

	// Bypass URL's
	public static final String REGISTRATION = "registration";
	public static final String LOGIN = "login";
	public static final String HEALTH_CHECK = "health.check";
	public static final String ISSUE_LIST = "issuelist";
	public static final String SWAGGER = "swagger";
	public static final String WEBHOOK = "webhook";
	public static final String LANDING_PAGE_EMAIL = "landing.page.email";

	public static final String SSH_PATH = "sshkey.path";

	/**
	 * Third party static data.
	 */
	public static final String AUTHORIZATION = "Authorization";
	public static final String BASIC = "Basic";
	public static final String BEARER = "Bearer";
	public static final String GRANT_TYPE = "grant_type";
	public static final String REFRESH_TOKEN = "refresh_token";
	public static final String AUTORIZATION_CODE = "authorization_code";
	public static final String CODE = "code";

	public static final String PROJECT_KEY = "<PROJECT_KEY>";
	public static final String PROJECT_NAME = "<PROJECT_NAME>";
	public static final String ISSUE_TYPE = "<ISSUE_TYPE>";
	public static final String PAGE_SIZE = "<PAGE_SIZE>";
	public static final String SEVERITIES = "<SEVERITIES>";
	public static final String PAGE_NO = "<PAGE_NO>";
	public static final String METRIC_KEYS = "<METRIC_KEYS>";
	public static final String RULE = "<RULE>";
	public static final String COVERAGE_METRIC = "coverage.metric";
	public static final String DUPLICATION_METRIC = "duplication.metric";
	public static final String BUG = "BUG";
	public static final String CODE_SMELL = "CODE_SMELL";
	public static final String CODE_SMELLS = "code_smells";
	public static final String VULNERABILITY = "VULNERABILITY";
	public static final String VULNERABILITIES = "vulnerabilities";
	public static final String COVERAGE = "coverage";
	public static final String COMMIT_DETAILS = "commitDetails";
	public static final String DESCRIPTION = "description";
	public static final String URL = "url";
	public static final String ACTIVE = "active";
	public static final String EVENTS = "events";
	public static final String NAME = "name";
	public static final String CONFIG = "config";
	public static final String PUSH_EVENTS = "push_events";
	public static final String COMMIT_WEBHOOK_URL_CG_SERVER = "commit.webhook.url.cg.server";
	public static final String SCANNER_URL = "scanner.url";
	public static final String SSH_KEY_NAME = "key";
	public static final String SSH_LABEL = "label";
	public static final String CG_SSH_TITLE = "cg.ssh.title";

	/**
	 * SONAR API
	 */
	public static final Integer PAGE_SIZE_VALUE = 100;
	public static final String SONAR_QUALITY_SUMMARY_SEND_SUCCESS = "quality.gate.send.success";
	public static final String SONAR_QUALITY_GATES_DETAILS_URL = "quality.gate.detail.url";
	public static final String SONAR_DUPLICATION_COVERAGE_URL = "coverage.duplication.detail.url";
	public static final String SONAR_GET_ISSUE_LIST_URL = "get.issue.list.url";
	public static final String SONAR_RULE_SEND_SUCCESS = "sonar.rule.send.success";
	public static final String ISSUE_SOLUTION_URL = "issue.solution.url";
	public static final String GET_FILE_LINES_URL = "get.file.lines";
	public static final String SONAR_LOGIN_KEY = "sonar.user.login.key";
	public static final String GET_FILE_ISSUE_URL = "get.file.issue.url";
	public static final String GET_FILE_ISSUE_URL_FULL = "get.file.issue.url.full";
	public static final String QUALITY_RATES = "quality.rate";
	public static final String MEASURE_LIST = "measure.list";
	public static final String SONAR_QUALITY_GATES = "quality.gate.url";
	public static final String PROJECT_DASHBOARD_URL = "project.dashboard.url";
	public static final String REPOSITORY_ANALYSE_START_SUCCESS = "repository.analyse.start.success";
	public static final String SONAR_DYNAMIC_TOKEN_CREATION_URL = "sonar.dynamic.token.url";
	public static final String GET_CODE_LIST_URL = "get.code.list.url";
	public static final String GET_DUPLICATION_URL = "get.duplication.url";
	public static final String PROJECT_DIR = "<PROJECT_DIR>";
	public static final String FILE_LIST_SEND_SUCCESS = "file.list.send.success";
	public static final String DUPLICATION_SEND_SUCCESS = "duplication.send.success";
	public static final String TOKEN_SENT_ON_MAIL = "token.sent.on.mail";
	public static final String DASHBOARD_SHARED_SUCCESS = "dashboard.shared.success";
	public static final String GENERATE_TOKEN_URL = "generate.token.url";
	public static final String LANDING_PAGE_EMAIL_SENT_SUCCESS = "landing.page.email.success";
	public static final String SECURITY_REPORT_URL = "security.report.url";
	public static final String OWASPTOP = "owaspTop10=unknown";
	public static final String SENDGRID_LANDING_PAGE_USER_TEMPLATE = "sendgrid.landing.page.user.template";
	public static final String SENDGRID_RECEIVE_TEMPLATE = "sendgrid.receive.template";
	public static final String NEW_USER_SIGN_UP_TEMPLATE = "new.user.sign.up.template";
	public static final String SUBSCRIPTION_PLAN = "subscription.plan";
	public static final String SUPER_ADMIN_MAIL = "super.admin.mail";
	public static final String ACCOUNTANT_MAIL = "accountant.mail";
	public static final String SENDGRID_WELCOME_TEMPLATE = "sendgrid.welcome.template";
	public static final String GET_TOTAL_FILES = "get.total.files";
	public static final String GET_BLOCKER_DETAILS = "get.blocker.details";
	public static final String GET_CRITICAL_DETAILS = "get.critical.details";
	public static final String GET_PROJECT_BRIEF_DETAILS = "get.all.project.brief.details";
	public static final String GET_MAINTAINABILITY_RATING = "maintainability.rating";
	public static final String GET_MAINTAINABILITY_RATING_REVERSE = "maintainability.rating.reverse";

	// stripe
	public static final String CARD_NOT_VALID = "card.not.valid";
	public static final String TOO_MANY_REQUESTS = "too.many.requests";
	public static final String INVALID_PARAMETERS = "invalid.parameters";
	public static final String INVALID_STRIPE_API = "invalid.stripe.api";
	public static final String STRIPE_GENERIC_ERROR = "stripe.generic.error";
	public static final String STRIPE_PRODUCT_ID = "stripe.product.id";
	public static final String CUSTOMER_CREATED_SUCCESS = "customer.created.success";
	public static final String PRODUCT_CREATED_SUCCESS = "product.created.success";
	public static final String PLAN_CREATED_SUCCESS = "plan.created.success";
	public static final String STRIPE_CHARGE_SUCCESS = "stripe.charge.success";
	public static final String FAIL_API_CONNECTION = "fail.api.connection";
	public static final String STRIPE_SUBSCRIPTION_UPDATE_SUCCESS = "stripe.subscription.update.success";
	public static final String STRIPE_SUBSCRIPTION_CANCEL_SUCCESS = "stripe.subscription.cancel.success";
	public static final String CARD_UPDATED_SUCCESS = "card.updated.success";
	public static final String PRODUCT_PLAN_LIST_SEND_SUCCESS = "product.plan.list.send.success";
	public static final String USER = "user";
	public static final String USER_DETAILS_SENT_SUCCESS = "user.details.sent.success";
	public static final String USER_DETAILS_UPDATED_SUCCESSFULLY = "user.details.updated.success";
	public static final String USER_INVITED_SUCCESS = "user.invited.sucess";
	public static final String SENDGRID_INVITE_USER_TEMPLATE = "sendgrid.invite.user.template";
	public static final String INVITE_LINK = "invite.link";
	public static final String SUBSCRIPTION_CREATE_NOTIFICATION = "subscription.create.notification";
	public static final String NOTIFICATION_UNREAD_STATUS = "notification.unread.status";
	public static final String NOTIFICATION_SUBSCRIPTION_TITLE = "notification.subscription.title";
	public static final String NOTIFICATION_SUBSCRIPTION_REASON = "notification.subscription.reason";
	public static final String NOTIFICATION_SUBSCRIPTION_DESTINATION_PAGE = "notification.subscription.destination.page";
	public static final String SUBSCRIPTION_EDIT_NOTIFICATION = "subscription.edit.notification";
	public static final String NOTIFICATION_EDIT_SUBSCRIPTION_TITLE = "notification.edit.subscription.title";
	public static final String SUBSCRIPTION_CANCEL_NOTIFICATION = "subscription.cancel.notification";
	public static final String NOTIFICATION_CANCEL_SUBSCRIPTION_TITLE = "notification.cancel.subscription.title";
	public static final String SUBSCRIPTION_CHARGED_SUCCESSFULL_NOTIFICATION = "subscription.charged.successfull.notification";
	public static final String NOTIFICATION_CHARGED_SUBSCRIPTION_TITLE = "notification.charged.subscription.title";
	public static final String NOTIFICATION_SUBSCRIPTION_CHARGED_REASON = "notification.subscription.charged.reason";
	public static final String NOTIFICATION_SUBSCRIPTION_CHARGE_DESTINATION_PAGE = "notification.subscription.charge.destination.page";
	public static final String COMPANY_DETAILS_UPDATED_SUCCESSFULLY = "company.details.updated.successfully";
	public static final String USER_COUNT_SENT_SUCCESS = "user_count_sent_success";
	public static final String PLAN_USER_LIMIT = "plan.user.limit";
	public static final String USER_INVITATION_CANCEL_SUCCESS = "user.invitation.cancel.succesS";
	public static final String REMAINING_USER_COUNT = "remaining.user.count";
	public static final String NOTIFICATION_INVITE_USER_MESSAGE = "notification.invite.user.message";
	public static final String NOTIFICATION_INVITE_USER_TITLE = "notification.invite.user.title";
	public static final String NOTIFICATION_INVITE_USER_REASON = "notification.invite.user.reason";
	public static final String NOTIFICATION_INVITE_USER_DESTINATION_PAGE = "notification.invite.user.destination.page";
	public static final String SUBSCRIPTION_CHARGED_FAIL_NOTIFICATION = "subscription.charged.fail.notification";
	public static final String NOTIFICATION_CHARGE_FAIL_SUBSCRIPTION_TITLE = "notification.charge.fail.subscription.title";
	public static final String NOTIFICATION_SUBSCRIPTION_CHARGE_FAIL_REASON = "notification.subscription.charge.fail.reason";
	public static final String NOTIFICATION_SUBSCRIPTION_CHARGE_FAIL_DESTINATION_PAGE = "notification.subscription.charge.fail.destination.page";
	public static final String CARD_UPDATE_NOTIFICATION = "card.updated.successfully";
	public static final String CARD_UPDATE_TITLE = "card.update.title";
	public static final String CARD_UPDATE_CHARGED_REASON = "card.update.charged.reason";
	public static final String CARD_UPDATE_DESTINATION_PAGE = "card.update.destination.page";
	public static final String STRIPE_SIGNATURE_NOT_VALID = "stripe.signature.not.valid";
	public static final String BILLING_INFO_SAVED_SUCCEESS = "billing.info.saved.succeess";
	public static final String BILLING_DETAILS_SEND_SUCCESS = "billing.details.send.success";
	public static final String SUBSCRIPTION_DETAILS_SEND_SUCCESS = "subscription.details.send.success";
	public static final String STRIPE_SIGNING_SECRET = "stripe.signing.secret";
	public static final String STRIPE_API_KEY = "stripe.api.key";
	public static final String PURCHASE_HISTORY_LIST_SEND_SUCCESS = "purchase.history.list.send.success";
	public static final String USER_LIST_SENT_SUCCESS = "user.list.sent.success";
	public static final String SENDGRID_ASSIGN_PROJECT_TEMPLATE = "sendgrid.assign.project.template";
	public static final String SENDGRID_ASSIGN_PROJECT_SUBJECT = "sendgrid.assign.project.subject.name";
	public static final String BUSINESS_PLAN = "business";
	public static final String ENTERPRISE_PLAN = "enterprise";
	public static final String TRAIL_SUBSCRIPTION_ACTIVATED_SUCCESSFULLY = "trail.subscription.activated.successfully";
	public static final String SENDGRID_TRAIL_PERIOD_END_TEMPLATE = "sendgrid.trail.period.end.template";
	public static final String SENDGRID_ORG_NEW_PLAN_SUBSCRIBE_TEMPLATE = "sendgrid.org.new.plan.subscribe.template";
	public static final String SENDGRID_ORG_PLAN_CANCELED_TEMPLATE = "sendgrid.org.plan.canceled.template";
	public static final String SENDGRID_ADMIN_NEW_PLAN_SUBSCRIBE_TEMPLATE = "sendgrid.admin.new.plan.subscribe.template";
	public static final String SENDGRID_ADMIN_PLAN_CANCEL_TEMPLATE = "sendgrid.admin.plan.cancel.template";
	public static final String SENDGRID_ORG_PAYMENT_RECIVED_TEMPLATE = "sendgrid.org.payment.recived.template";
	public static final String SENDGRID_ADMIN_PAYMENT_RECIVED_TEMPLATE = "sendgrid.admin.payment.recived.template";
	public static final String COUPON_CODE = "coupon.code";
	public static final String CODEGRIP_SECRET_KEY = "codegrip.secret.key";
	public static final String SG_USER_DETAILS_SAVED_SUCCESS = "sg.user.details.saved.successfully";
	public static final String PROMOTION_LIST_SENT_SUCCESSFULLY = "promotion.list.sent.successfully";
	public static final String SSGLOBLE_DATE = "ssgloble.date";
	public static final String SSGLOBLE_COUPON = "ssgloble.coupon";

	// Line chart data.
	public static final Double LINE_TENSION = 0.3;
	public static final String MAINTAINABILITY_COLOR = "#1EC4B4";
	public static final String SECURITY_COLOR = "#5099DE";
	public static final String REILAIBILITY_COLOR = "#D93025";
	public static final String DUPLICATION_COLOR = "#D93025";
	public static final String TRANSPERENT_COLOR = "transparent";
	public static final Integer POINT_RADIUS = 5;
	public static final Integer POINT_HOVER_RADIUS = 10;
	public static final Integer POINT_HIT_RADIUS = 30;
	public static final Integer POINT_BORDER_WIDTH = 2;
	public static final String TRIANGLE_POINT_STYLE = "triangle";
	public static final String MAINTAINABILITY_LABEL = "Maintainability";
	public static final String RELIABILITY_LABEL = "Reliability";
	public static final String SECURITY_LABEL = "Security";
	public static final String DUPLICATION_LABEL = "Duplication";

	public static final String RECT_POINT_STYLE = "rect";
	public static final String CIRCLE_POINT_STYLE = "circle";

	public static final String DUPLICATION_KEY = "duplication";
	public static final String MAINTAINABILITY_KEY = "maintainability";
	public static final String SECURITY_KEY = "security";
	public static final String RELIABILITY_KEY = "reliability";
	
	public static final String SEND_ERROR_EMAIL = "send.error.email";
	public static final String ERROR_EMAIL_TEMPLATE_ID = "sendgrid.error.message.template";
	public static final String BITBUCKET_GET_WEBHOOK = "bitbucket.get.webhook";
	public static final String BITBUCKET_DELETE_WEBHOOK = "bitbucket.delete.webhook.url";
	public static final String BITBUCKET_DELETE_SSHKEY_URL = "bitbucket.delete.sshkey.url";

}
