package com.mb.codegrip.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;

import com.mb.codegrip.constants.CodeGripConstants;

@PropertySources(value = { @PropertySource("classpath:messages.properties")})
public class MainMethodToTest {
	private static Environment environment;
	public static KeyPair buildKeyPair() throws NoSuchAlgorithmException {
	    final int keySize = 2048;
	    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
	    keyPairGenerator.initialize(keySize);      
	    return keyPairGenerator.genKeyPair();
	}

	public static byte[] encrypt(PrivateKey privateKey, String message) throws Exception {
	    Cipher cipher = Cipher.getInstance("RSA");  
	    cipher.init(Cipher.ENCRYPT_MODE, privateKey);  

	    return cipher.doFinal(message.getBytes());  
	}

	public static byte[] decrypt(PublicKey publicKey, byte [] encrypted) throws Exception {
	    Cipher cipher = Cipher.getInstance("RSA");  
	    cipher.init(Cipher.DECRYPT_MODE, publicKey);
	    
	    return cipher.doFinal(encrypted);
	}

	public String getProperty(String key) {
		return environment.getProperty(key);
	}


	public static void main(String args[])
			throws Exception {
		String url = "deepak_kumbhar46/amazing-apps-android";
		String[] projectUrlSplit = url.split("/");
		System.out.println(projectUrlSplit[1]);
		String projectName = projectUrlSplit[projectUrlSplit.length - 1];
		System.out.println(projectName.substring(0, projectName.length() - 4));
		
		String a = "<https://api.github.com/user/29866012/repos?access_token802199e5b405056af3ca2bcbdb8dc421d574bed5=&per_page=3&page=2>; rel=\"next\", <https://api.github.com/user/29866012/repos?access_token802199e5b405056af3ca2bcbdb8dc421d574bed5=&per_page=3&page=3>; rel=\"last\"";
		String[] val = a.split(",");
		System.out.println(val);
		Integer size = val.length;
		System.out.println(val[size-1]);
		String newStr = val[size-1].substring(val[size-1].indexOf("&page="), val[size-1].indexOf('>', val[size-1].indexOf("&page=")));
		System.out.println(newStr);
		String[] finalStr = newStr.split("=");
		System.out.println(finalStr[1]);
		
		String str = "INFO: Error during SonarQube Scanner execution\r\n" + 
				"ERROR: Fail to request https://qualitygateway.codegrip.tech/api/ce/submit?projectKey=i\r\n" + 
				"ERROR: Caused by: Read timed out\r\n" + 
				"ERROR:\r\n" + 
				"ERROR: Re-run SonarQube Scanner using the -X switch to enable full debug logging.";
		
	/*	if(str.contains("ERROR") && str.contains("Caused by")) {
			String newStr = str.substring(str.indexOf("Caused by:"), str.indexOf('\n', str.indexOf("Caused by:")));
			System.out.println(newStr);
		}*/
		
		
		
		
		
		
		
		
		
		
		
		 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String dt = "2019-02-02 12:20:57";
		Date date = dateFormat.parse(dt);
		
		long milliseconds = new Timestamp(date.getTime()).getTime() - new Timestamp(System.currentTimeMillis()).getTime();
		long seconds = milliseconds / 1000;
		System.out.println( (int) TimeUnit.SECONDS.toDays(seconds));
		
		
		Long tempVal = (long)Double.parseDouble("0.0");
		System.out.println(tempVal);
		
		List<String> key = new ArrayList<>();
		key.add("abc");
//		key.add("xyz");
		String k = "";
		for (String string : key) {
			k = k+","+string;
		}
		System.out.println(k);
		
		
		//System.out.println("decode: "+PasswordUtil.decodeUrl(url));
		/*String q = "random word £500 bank $";
		String url = "http://example.com/query?q=" + URLEncoder.encode(q, "UTF-8");
		String a =PasswordUtil.encodeUrl(url);
		System.out.println("encode: "+a);
		String b = PasswordUtil.decodeUrl(a);
		System.out.println("decode: "+b);*/
		
		
		  String text = "sgglobal2019";
          String key1 = "CGssEebglob2019W"; // 128 bit key
          // Create key and cipher
          Key aesKey = new SecretKeySpec(key1.getBytes(), "AES");
          Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
          System.out.println("key: "+aesKey);
          // encrypt the text
          cipher.init(Cipher.ENCRYPT_MODE, aesKey);
          byte[] encrypted = cipher.doFinal(text.getBytes());
          String aa = new String(encrypted);
          String accc= Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes("UTF-8")));
          System.err.println(accc);
          // decrypt the text
          cipher.init(Cipher.DECRYPT_MODE, aesKey);
          String decrypted = new String(cipher.doFinal(Base64.getDecoder().decode(accc)));
          System.err.println(new String(decrypted));
          
          String ddd = "2019-02-16 24:00:00.00";
          Date parsedDate = dateFormat.parse(ddd);
          Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
          
          String sDate1="17/02/2019";
			if(timestamp.before(new SimpleDateFormat("dd/MM/yyyy").parse(sDate1))) {
				System.out.println("TREEEEEEEEEEEEEEE:");
			}else {
				System.out.println("FALLSLSSLL");
			}
			
			// System.out.println("cooo: "+environment.getProperty(CodeGripConstants.COUPON_CODE));
			String abc = environment.getProperty(CodeGripConstants.CODEGRIP_SECRET_KEY);
			//LOGGER.info(environment.getProperty(CodeGripConstants.CODEGRIP_SECRET_KEY));
			 System.out.println("cooo: "+abc);
			//System.out.println("sring array "+environment.getProperty(CodeGripConstants.CODEGRIP_SECRET_KEY));
			// String[] a =environment.getProperty(CodeGripConstants.COUPON_CODE).split(",");
			
			/* for(String s: a) {
				 if(s.equals("sgglobal2019")) {
					 System.out.println("key: "+s);
				 }else {
					 System.out.println("key11: "+s);
				 }
			 }
			 String[][] array = new String[a.length][a.length];
			 for(int i = 0;i < a.length;i++) {
			      array[i] = a[i].split(",");
			      System.out.println("sasa: "+array[i]);
			  }
		  */
			
	}
		
		
		
		/**long milliseconds = endDate.getTime() - new Timestamp(System.currentTimeMillis()).getTime();
	    int seconds = (int) milliseconds / 1000;
	    int day = (int)TimeUnit.SECONDS.toDays(seconds); 
	    System.out.println(day);*/
	    
	/*	Date date= new Date();
		long time = date.getTime();
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.add(Calendar.HOUR, 72);
		System.out.println(new Date(cal.getTime().getTime()));
		
		
		
		Random rand = new Random();
		System.out.println(rand.nextInt(4) + 1);*/
		
	
		/**Path file = Files.createTempDirectory("privateGitRepos");
		System.out.println(file);
		List<String> branches = new ArrayList<>();
		branches.add("refs/heads/dev");
		Git gitCloner = Git.cloneRepository().setURI("https://github.com/deepak-kumbhar/Angular_Node_Demo.git").setDirectory(file.toFile())
				.setCredentialsProvider(new UsernamePasswordCredentialsProvider("f3021a00f8ac2e5a444a20bfa05240221deff84d",""))
				.setBranchesToClone(branches).setBranch("refs/heads/dev").call();
		gitCloner.getRepository().close();*/
		
		/**Path file = Files.createTempDirectory("privateGitRepos");
		System.out.println(file);
		SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
			@Override
			protected void configure(Host host, Session session) {
				// do nothing
			}

		};
		CloneCommand cloneCommand = Git.cloneRepository();
		cloneCommand.setURI("git@bitbucket.org:Deepak_kumbhar/portfolio-app.git");
		cloneCommand.setDirectory(file.toFile());
		cloneCommand.setTransportConfigCallback(new TransportConfigCallback() {
			@Override
			public void configure(Transport transport) {
				SshTransport sshTransport = (SshTransport) transport;
				sshTransport.setSshSessionFactory(sshSessionFactory);

			}
		});
		cloneCommand.call();
		System.out.println("Completed");*/

	
	 /* ClassLoader classLoader = MainMethodToTest.class.getClassLoader(); String
	  sshFile = classLoader.getResource("SSHKeys").getFile(); sshFile = sshFile +
	  File.separator + ("local") + File.separator + "id_rsa.pub"; sshFile =
	  sshFile.replaceAll("%20", " "); File scanner = new File(sshFile);
	  BufferedReader br = new BufferedReader(new FileReader(scanner));*/

	 
	 
	 /* String st; while ((st = br.readLine()) != null) System.out.println(st);
	 * 
	 * 
	 * 
	 * String finalEffortData = "45 m"; String newStr = ""; int cnt =0; for(int
	 * i=0;i<finalEffortData.length();i++) { newStr+=finalEffortData.charAt(i);
	 * if((int)finalEffortData.charAt(i) >=65 &&
	 * (int)finalEffortData.charAt(i)<=122) { cnt++; } if(cnt==2) break; }
	 * System.out.println(newStr);
	 * 
	 * 
	 * String incryptUrl = new
	 * StringBuilder("{\"p\":").append(1).append(",").append("\"u\":").toString();
	 * incryptUrl = incryptUrl + null + "}"; System.out.println(incryptUrl);
	 * 
	 * String val = "java=7983;xml=224"; String[] data = val.split(";"); double
	 * singleProjectTotalLines = 0; for (String string : data) { String[] lines =
	 * string.split("="); double value = Double.parseDouble(lines[1]);
	 * singleProjectTotalLines += value; }
	 * System.out.println(singleProjectTotalLines);
	 * System.out.println(coolFormat(1691, 0));
	 * 
	 * }
	 * 
	 * static String coolFormat(double n, int iteration) { char[] c = new char[] {
	 * 'k', 'm', 'b', 't' }; double d = ((long) n / 100) / 10.0; boolean isRound =
	 * (d * 10) % 10 == 0;// true if the decimal part is equal to 0 (then it's
	 * trimmed anyway) return (d < 1000 ? // this determines the class, i.e. 'k',
	 * 'm' etc ((d > 99.9 || isRound || (!isRound && d > 9.99) ? // this decides
	 * whether to trim the decimals (int) d * 10 / 10 : d + "" // (int) d * 10 / 10
	 * drops the decimal ) + "" + c[iteration]) : coolFormat(d, iteration + 1));
	 * 
	 * }
	 */

	/**
	 * DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); Date
	 * date = new Date(); SimpleDateFormat formatter6=new
	 * SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); Date
	 * date6=formatter6.parse(dateFormat.format(date)); System.out.println(date6);
	 * 
	 * int totalEfforts=299312;
	 * 
	 * String result=totalEfforts<8? (totalEfforts+" h"):((totalEfforts%8)==0 ?
	 * (totalEfforts/8+" d"):(totalEfforts/8+"d "+totalEfforts%8+"h")).toString();
	 * System.out.println(totalEfforts<8? (totalEfforts+" h"):((totalEfforts%8)==0 ?
	 * (totalEfforts/8+" d"):(totalEfforts/8+"d "+totalEfforts%8+"h")));
	 * System.out.println(result);
	 */

	/**
	 * String code=""; code = System.getProperty("os.name");
	 * System.out.println(code);
	 */

	/**
	 * String finalURL =
	 * environment.getProperty(CodeGripConstants.GITLAB_ACCESS_TOKEN_URL); try {
	 * finalURL=finalURL.replace("<CLIENT_ID>",environment.getProperty(CodeGripConstants.GITLAB_CLIENT_ID))
	 * .replace("<CLIENT_SECRET>",environment.getProperty(CodeGripConstants.GITLAB_CLIENT_SECRET))
	 * .replace("<CODE>", code); System.out.println("Final GITLAB
	 * URL--------->"+finalURL); HttpHeaders headers = new HttpHeaders();
	 * 
	 * MultiValueMap<String, String> body= new LinkedMultiValueMap<>();
	 * HttpEntity<?> httpEntity = new HttpEntity<>(body, headers); RestTemplate
	 * restTemplate = new RestTemplate(); String val =
	 * restTemplate.postForObject(finalURL, httpEntity, String.class); JSONObject
	 * jsonObject = new JSONObject(val); String accessToken =
	 * jsonObject.getString("access_token");
	 * 
	 * System.out.println("---------------------------------"+accessToken);
	 * System.out.println("--------------------"+jsonObject);
	 * 
	 * return jsonObject; } catch(Exception e) { LOGGER.info("get access token"+e);
	 * JSONObject jsonObject = new JSONObject();
	 * jsonObject.put("access_token",jsonObject); return jsonObject; }
	 * 
	 * /** String finalURL =
	 * environment.getProperty(CodeGripConstants.GITLAB_ACCESS_TOKEN_URL); try {
	 * finalURL=finalURL.replace("<CLIENT_ID>",environment.getProperty(CodeGripConstants.GITLAB_CLIENT_ID))
	 * .replace("<CLIENT_SECRET>",environment.getProperty(CodeGripConstants.GITLAB_CLIENT_SECRET))
	 * .replace("<CODE>", code); System.out.println("Final GITLAB
	 * URL--------->"+finalURL); HttpHeaders headers = new HttpHeaders();
	 * 
	 * MultiValueMap<String, String> body= new LinkedMultiValueMap<>();
	 * HttpEntity<?> httpEntity = new HttpEntity<>(body, headers); RestTemplate
	 * restTemplate = new RestTemplate(); String val =
	 * restTemplate.postForObject(finalURL, httpEntity, String.class); JSONObject
	 * jsonObject = new JSONObject(val); String accessToken =
	 * jsonObject.getString("access_token");
	 * 
	 * System.out.println("---------------------------------"+accessToken);
	 * System.out.println("--------------------"+jsonObject);
	 * 
	 * return jsonObject; } catch(Exception e) { LOGGER.info("get access token"+e);
	 * JSONObject jsonObject = new JSONObject();
	 * jsonObject.put("access_token",jsonObject); return jsonObject; }
	 * 
	 */

	/*
	 * String timestamp = new
	 * java.text.SimpleDateFormat("MM/dd/yyyy h:mm:ss a").format(new Date());
	 * System.out.println(timestamp);
	 */

	// get github project list.
	/*
	 * BitbucketAPIUtil bitbucketAPIUtil = new BitbucketAPIUtil(); String result =
	 * bitbucketAPIUtil.callRestAPI(
	 * "https://api.github.com/users/deepak-kumbhar/repos" + "?access_token" +
	 * "364483c62f5a7d0f994642359757131b2d1687c0"); JSONArray JSONArray = new
	 * JSONArray(result); System.out.println(JSONArray);
	 */

	// add seconds in current time.
	/*
	 * SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	 * Calendar calendar = Calendar.getInstance(); // gets a calendar using the
	 * default time zone and locale. // calendar.add(Calendar.SECOND, 7200); String
	 * format = new
	 * SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(calendar.getTime());
	 * System.out.println(format);
	 */

	// Get issue search code.

	/**
	 * FilterModel filterModel = new FilterModel(); filterModel.setBugs(true);
	 * filterModel.setInfo(true); filterModel.setBlocker(true);
	 * filterModel.setEndDate("1994-12-45 45:54");
	 * 
	 * String url =
	 * "https://qualitygateway.codegrip.tech/api/issues/search?additionalFields=_all&resolved=false&componentKeys=<PROJECT_KEY>&s=FILE_LINE&resolved=false&types=<TYPES>&severities=<SEVERITIES>";
	 * url = url.replace("<PROJECT_KEY>", "Snehal-Android-Assignment:master");
	 * String types =
	 * (filterModel.getBugs()?"BUG":"")+(filterModel.getCodesmell()?",CODE_SMELLS":"")+(filterModel.getVulnerability()?",VULNERABILITY":"");
	 * String severities =
	 * (filterModel.getBlocker()?"BLOCKER":"")+(filterModel.getInfo()?",INFO":"")+(filterModel.getCritical()?",CRITICAL":"")+(filterModel.getMajor()?",MAJOR":"")+(filterModel.getMinor()?",MINOR":"");
	 * 
	 * System.out.println(types); System.out.println(severities);
	 * 
	 * url = url.replace("<TYPES>",
	 * types.charAt(0)==','?types.substring(1):types).replace("<SEVERITIES>",
	 * severities.charAt(0)==','?severities.substring(1):severities); url =
	 * url+(filterModel.getEndDate()!=null?("&createdBefore="+filterModel.getEndDate().substring(0,
	 * 10)):""); url =
	 * url+(filterModel.getStartDate()!=null?("&createdAfter="+filterModel.getEndDate().substring(0,
	 * 10)):""); System.out.println(url);
	 */
	/*
	 * String htmlSourceURL =
	 * "https://s3-us-west-2.amazonaws.com/dev-qa-scanner/DeepakCGScannerPublic.zip";
	 * downloadUsingNIO(htmlSourceURL, "C:/Users/Dell/Data");
	 * System.out.println("done");
	 */

	/*
	 * String encodedBytes = Base64.getEncoder()
	 * .encodeToString(("LSqpgATqnZMvGRzAXR" +":"+
	 * "3ZN6phHJg87FPwYPSUhGBMdShpfFdBwY").getBytes());
	 */
//		TFNxcGdBVHFuWk12R1J6QVhSM1pONnBoSEpnODdGUHdZUFNVaEdCTWRTaHBmRmRCd1k=

	/* System.out.println(encodedBytes); */
	/*
	 * HttpHeaders headers = new HttpHeaders();
	 * headers.setContentType(MediaType.APPLICATION_JSON);
	 * headers.set("access_token",
	 * "R2HUfGOUHHPlIVKy6QmY-XflzKVxB42ti_kc_Qe7ziKeHoAUeeaNb22szDoAr8knZ4skUc-JnU7UzGoa2l4="
	 * ); MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
	 * body.add("description", "Demo webhook for bitbucket."); body.add("url",
	 * "https://dev-api.codegrip.tech/source/webhooks"); body.add("active", "true");
	 * body.add("events", "[\"repo:push\"]"); HttpEntity<?> httpEntity = new
	 * HttpEntity<>(body, headers); RestTemplate restTemplate = new RestTemplate();
	 * String url =
	 * "https://bitbucket.org/!api/2.0/repositories/Deepak_kumbhar/schoolsoftware/hooks";
	 * String val = restTemplate.postForObject(url,httpEntity, String.class);
	 * System.out.println(val);
	 */

	/*
	 * SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd"); Date
	 * date1 = simpleDateFormat.parse(date.substring(0, 10));
	 * System.out.println(date1); SimpleDateFormat simpleDateFormat1 = new
	 * SimpleDateFormat("MM/dd/yyyy");
	 * System.out.println(simpleDateFormat1.format(date1));
	 */

	/*
	 * int h = 18; System.out.println(h<8? (h+" h"):((h%8)==0 ?
	 * (h/8+" d"):(h/8+"d "+h%8+"h")));
	 */

	/*
	 * String finalURL =
	 * "https://gitlab.com/oauth/token?client_id=<CLIENT_ID>&client_secret=<CLIENT_SECRET>&code=<CODE>&state=test&grant_type=authorization_code&redirect_uri=https://dev-app.codegrip.tech";
	 * finalURL = finalURL.replace("<CLIENT_ID>",
	 * "7de03cf3ad24168b82bec593b7a6f631422c98b39d3b3613869a17092c144306")
	 * .replace("<CLIENT_SECRET>",
	 * "b3522967c9bf5b0c49cacb3b3496c50264aadb1714c7f4939ad53a4a6b1244fa")
	 * .replace("<CODE>",
	 * "3e52e9ff3479d41ab2eeb3c15bd9e91d8bb02e266b2484e272d3fdd6816327b0");
	 * System.out.println("Final GITLAB URL--------->" + finalURL); HttpHeaders
	 * headers = new HttpHeaders(); // headers.set("content_type",
	 * "appliacation/json"); // QueryParam
	 * 
	 * MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
	 * 
	 * // Note the body object as first parameter! HttpEntity<?> httpEntity = new
	 * HttpEntity<>(body, headers); RestTemplate restTemplate = new RestTemplate();
	 * String val = restTemplate.postForObject(finalURL, httpEntity, String.class);
	 * 
	 * JSONObject jsonObjectResponse = new JSONObject(val);
	 * System.out.println("JSON OBJECT----------------------> " +
	 * jsonObjectResponse);
	 */
	/*
	 * List<ProjectBranchModel> movies = new ArrayList<>(); ProjectBranchModel m =
	 * new ProjectBranchModel(); m.setBranchKey(""); movies.add(m);
	 * m.setBranchKey("Lord of the rings"); movies.add(m);
	 * m.setBranchKey("Back to the future"); movies.add(m);
	 * m.setBranchKey("Carlito's way"); movies.add(m);
	 * m.setBranchKey("Pulp fiction"); movies.add(m);
	 * movies.sort(Comparator.comparing(ProjectBranchModel::getBranchKey).reversed()
	 * ); movies.forEach(System.out::println);
	 */
	/*
	 * Git gitCloner = Git.cloneRepository().setURI(
	 * "https://github.com/deepak-kumbhar/passwordChekerAngularJs.git")
	 * .setDirectory(Paths.get("E:\\Deepak Project Data\\TestData").toFile())
	 * .setCredentialsProvider(new UsernamePasswordCredentialsProvider("",
	 * "")).call(); gitCloner.getRepository().close(); gitCloner.close();
	 */

	private static void downloadUsingNIO(String urlStr, String file) throws IOException {
		URL url = new URL(urlStr);
		ReadableByteChannel rbc = Channels.newChannel(url.openStream());
		FileOutputStream fos = new FileOutputStream(file);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
		rbc.close();
	}



}