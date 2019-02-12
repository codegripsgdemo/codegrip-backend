package com.mb.codegrip.utils;

import static com.mb.codegrip.constants.CodeGripConstants.CG_SCANNER_FOLDER_NAME;
import static com.mb.codegrip.constants.CodeGripConstants.GITHUB_PROVIDER;
import static com.mb.codegrip.constants.CodeGripConstants.GITLAB_PROVIDER;
import static com.mb.codegrip.constants.CodeGripConstants.PROVIDER;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.exception.CustomException;
import com.mb.codegrip.model.ProjectsModel;
import com.mb.codegrip.service.ProjectService;

@Configuration
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:exception.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class CGScannerUtil implements EnvironmentAware {

	private static final Logger LOGGER = Logger.getLogger(CGScannerUtil.class);
	private static Environment environment;

	
	@Autowired
	private ProjectService projectService;

	public static String getProperty(String key) {
		return environment.getProperty(key);
	}

	@SuppressWarnings("static-access")
	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	public String startCGScan(String gitURL, String projectName, JSONObject accessTokens, String projectKey,
			String branchName, Integer qualityId, ProjectsModel projectsModel)
			throws IOException, JSONException, CustomException {

		String tempFilePath = System.getProperty("java.io.tmpdir");
		LOGGER.info(tempFilePath);
		Path file = Files.createTempDirectory(projectName);
		String output = "";
		String scannerURL = environment.getProperty(CodeGripConstants.SCANNER_URL);
		String sonarLoginKey = environment.getProperty(CodeGripConstants.SONAR_LOGIN_KEY);
		LOGGER.info("Project key: " + projectKey);
		LOGGER.info("Created Path: " + file.toString());
		String token = "";

		if (GITHUB_PROVIDER.equals(accessTokens.get(PROVIDER))) {
			token = accessTokens.getString(CodeGripConstants.ACCESS_TOKEN);
			if ("master".equals(branchName)) {
				LOGGER.info("Master branch Cloning github.");
				cloneMasterBranchGithub(token, gitURL, file, projectsModel, branchName);
			} else {
				LOGGER.info(branchName + " branch Cloning github.");
				cloneSpecificBranchGithub(token, gitURL, file, branchName, projectsModel);
			}

		} else if (CodeGripConstants.BITBUCKET_PROVIDER.equals(accessTokens.get(CodeGripConstants.PROVIDER))
				|| GITLAB_PROVIDER.equals(accessTokens.get(PROVIDER))) {
			if ("master".equals(branchName)) {
				LOGGER.info("Master branch Cloning bitbucket.");
				cloneMasterRepositoryUsingSSH(gitURL, file, projectsModel, branchName);
			} else {
				LOGGER.info(branchName + " branch Cloning bitbucket.");
				cloneSpecificBranchUsingSSH(gitURL, file, branchName, projectsModel);
			}
		} else {
			LOGGER.info("URL: " + gitURL);
			try {
				Git gitCloner = Git.cloneRepository().setURI(gitURL).setDirectory(file.toFile()).call();
				gitCloner.getRepository().close();
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
				projectService.updateStatusAndSendMailToAdmin(projectsModel, e.getMessage(), "Cloning Failed");
				throw new CustomException(
						environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG_WHILE_CLONING));
			}
		}

		String os = System.getProperty("os.name");
		String[] osSplitVal = os.split(" ");

		ClassLoader classLoader = getClass().getClassLoader();
		String scannerFilePath = "";
		if (!osSplitVal[0].equalsIgnoreCase("windows")) {
			scannerFilePath = File.separator + "opt" + File.separator + "codegrip" + File.separator
					+ CG_SCANNER_FOLDER_NAME;
		} else {
			scannerFilePath = classLoader.getResource(CG_SCANNER_FOLDER_NAME).getFile();
		}
		File scanner = new File(scannerFilePath);

		LOGGER.info("scanner path: " + scanner);

		LOGGER.info("Completed Cloning");
		String copiedScannerPath = File.separator + CG_SCANNER_FOLDER_NAME + File.separator + "bin";
		LOGGER.info("Copied scanner path: " + copiedScannerPath);
		LOGGER.info("File absolute path: " + file.toAbsolutePath());
		String cpOutput = "";

		String path = scanner.getAbsolutePath();
		path = path.replaceAll("%20", " ");
		FileUtils.copyDirectoryToDirectory(new File(path), file.toFile());

		try {

			LOGGER.info("Final file path: " + new File(path));
			if (osSplitVal[0].equalsIgnoreCase("windows")) {
				output = runPowerShellCommand("powerShell -command " + file.toAbsolutePath() + copiedScannerPath
						+ File.separator + "CG-scanner.bat" + " -D sonar.host.url=" + scannerURL + " -D sonar.login="
						+ sonarLoginKey + " -D sonar.sources=. -D sonar.analysis.mode=publish " + "-D sonar.projectKey="
						+ projectKey + " -D sonar.projectName=" + projectName
						+ " -D sonar.java.binaries=. -D sonar.analysis.buildNumber=" + qualityId);
				LOGGER.info("In win os");
			} else {
				output = executeCommand("bash " + file.toAbsolutePath() + copiedScannerPath + File.separator
						+ "sonar-scanner -Dsonar.projectBaseDir=" + file.toAbsolutePath() + " -Dsonar.host.url="
						+ scannerURL + " -Dsonar.login=" + sonarLoginKey + " -Dsonar.sources=" + file.toAbsolutePath()
						+ " -Dsonar.analysis.mode=publish " + "-Dsonar.projectKey=" + projectKey
						+ " -Dsonar.projectName=" + projectName
						+ " -Dsonar.java.binaries=. -Dsonar.analysis.buildNumber=" + qualityId);
			}
			LOGGER.info("CMD O/P :" + output);
			LOGGER.info("CMD O/P Copy :" + cpOutput);

		} catch (Exception e) {
			LOGGER.info("Exception occurred while analyzing repo.");
			projectService.updateStatusAndSendMailToAdmin(projectsModel, output, "Analyzing Failed");
			LOGGER.error(e.getMessage());
		} finally {
			FileUtils.deleteDirectory(file.toFile());
		}
		return output;
	}


	/*********************************************************************************************
	 * clone specific branch from github.
	 *********************************************************************************************/
	private void cloneSpecificBranchUsingSSH(String gitURL, Path file, String branchName, ProjectsModel projectsModel)
			throws CustomException {
		try {
			LOGGER.info(file);
			SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
				@Override
				protected void configure(Host host, Session session) {
					/** session.setConfig("StrictHostKeyChecking", "no"); */
					session.setUserInfo(new UserInfo() {

						@Override
						public void showMessage(String message) {
							LOGGER.info("Default message.");
						}

						@Override
						public boolean promptYesNo(String message) {
							return false;
						}

						@Override
						public boolean promptPassword(String message) {
							return false;
						}

						@Override
						public boolean promptPassphrase(String message) {
							return false;
						}

						@Override
						public String getPassword() {
							return null;
						}

						@Override
						public String getPassphrase() {
							return null;
						}
					});
				}

			};
			try (Git result = Git.cloneRepository().setURI(gitURL).setBranch(branchName)
					.setTransportConfigCallback(transport -> {
						SshTransport sshTransport = (SshTransport) transport;
						sshTransport.setSshSessionFactory(sshSessionFactory);
					}).setDirectory(file.toFile()).call()) {
				LOGGER.info("Having repository: " + result.getRepository().getDirectory());
			}

			LOGGER.info("Cloning completed");
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			projectService.updateStatusAndSendMailToAdmin(projectsModel, e.getMessage(), "Cloning Failed");
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG_WHILE_CLONING));

		}

	}

	/*********************************************************************************************
	 * clone specific branch from github.
	 * 
	 * @param projectsModel
	 * @param branchName2
	 * @throws CustomException
	 *********************************************************************************************/
	private void cloneSpecificBranchGithub(String token, String gitURL, Path file, String branchName,
			ProjectsModel projectsModel) throws CustomException {
		try {
			List<String> branches = new ArrayList<>();
			branches.add(branchName);
			Git gitCloner = Git.cloneRepository().setURI(gitURL).setDirectory(file.toFile())
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
					.setBranchesToClone(branches).setBranch(branchName).call();
			gitCloner.getRepository().close();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			projectService.updateStatusAndSendMailToAdmin(projectsModel, e.getMessage(), "Cloning Failed");
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG_WHILE_CLONING));
		}
	}

	/*********************************************************************************************
	 * clone master branch from github.
	 * 
	 * @param projectsModel
	 * @throws CustomException
	 *********************************************************************************************/
	private void cloneMasterBranchGithub(String token, String gitURL, Path file, ProjectsModel projectsModel,
			String branchName) throws CustomException {
		try {
			Git gitCloner = Git.cloneRepository().setURI(gitURL).setDirectory(file.toFile())
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, "")).call();
			gitCloner.getRepository().close();
		} catch (Exception e) {
			projectService.updateStatusAndSendMailToAdmin(projectsModel, e.getMessage(), "Cloning Failed");
			LOGGER.error(e.getMessage());
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG_WHILE_CLONING));
		}
	}

	/*********************************************************************************************
	 * clone repository using SSH.
	 * 
	 * @param projectsModel
	 *********************************************************************************************/
	private void cloneMasterRepositoryUsingSSH(String gitURL, Path file, ProjectsModel projectsModel, String branchName)
			throws CustomException {
		try {
			LOGGER.info(file);
			SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
				@Override
				protected void configure(Host host, Session session) {
					/** session.setConfig("StrictHostKeyChecking", "no"); */
					session.setUserInfo(new UserInfo() {

						@Override
						public void showMessage(String message) {
							// Message will be here
						}

						@Override
						public boolean promptYesNo(String message) {
							return false;
						}

						@Override
						public boolean promptPassword(String message) {
							return false;
						}

						@Override
						public boolean promptPassphrase(String message) {
							return false;
						}

						@Override
						public String getPassword() {
							return null;
						}

						@Override
						public String getPassphrase() {
							return null;
						}
					});
				}

			};
			try (Git result = Git.cloneRepository().setURI(gitURL).setTransportConfigCallback(transport -> {
				SshTransport sshTransport = (SshTransport) transport;
				sshTransport.setSshSessionFactory(sshSessionFactory);
			}).setDirectory(file.toFile()).call()) {
				LOGGER.info("Having repository: " + result.getRepository().getDirectory());
			}
			LOGGER.info("Cloning completed");
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			projectService.updateStatusAndSendMailToAdmin(projectsModel, e.getMessage(), "Cloning Failed");
			throw new CustomException(environment.getProperty(CodeGripConstants.SOMETHING_WENT_WRONG_WHILE_CLONING));

		}

	}

	/*******************************************************************************************************
	 * Command executor.
	 ********************************************************************************************************/
	private static String executeCommand(String command) {
		StringBuilder output = new StringBuilder();
		Process p;
		try {
			LOGGER.info("command: " + command);
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		return output.toString();
	}

	/*******************************************************************************************************
	 * Powershell excecutor.
	 ********************************************************************************************************/
	public static String runPowerShellCommand(String cmd) {
		Process powerShellProcess;
		StringBuilder error = new StringBuilder();
		try {
			powerShellProcess = Runtime.getRuntime().exec(cmd);

			// Getting the results
			powerShellProcess.getOutputStream().close();
			String line;
			LOGGER.info("Standard Output:");
			BufferedReader stdout = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream()));
			while ((line = stdout.readLine()) != null) {
				LOGGER.info(line);
			}
			stdout.close();
			LOGGER.info("Standard Error:");
			BufferedReader stderr = new BufferedReader(new InputStreamReader(powerShellProcess.getErrorStream()));
			while ((line = stderr.readLine()) != null) {
				LOGGER.info(line);
				error = error.append(" " + line);
			}
			stderr.close();
			LOGGER.info("Done");
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
		return error.toString();
	}

	public static class InputStreamConsumer implements Runnable {
		private InputStream is;

		public InputStreamConsumer(InputStream is) {
			this.is = is;
		}

		public static void consume(InputStream inputStream) {
			new Thread(new InputStreamConsumer(inputStream)).start();
		}

		@Override
		public void run() {
			int in = -1;
			try {
				while ((in = is.read()) != -1) {
					LOGGER.info((char) in);
				}
			} catch (IOException exp) {
				LOGGER.error(exp.getMessage());
			}
		}

	}

}
