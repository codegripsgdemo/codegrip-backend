package com.mb.codegrip.utils;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import com.mb.codegrip.constants.CodeGripConstants;
import com.mb.codegrip.exception.CustomException;

@Configuration
@PropertySources(value = { @PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:messages.properties"),
		@PropertySource("classpath:profiles/${spring.profiles.active}/application.properties") })
public class CommonUtil implements EnvironmentAware {

	private static final Logger LOGGER = Logger.getLogger(CommonUtil.class);

	private static Environment environment;

	public static String getProperty(String key) {
		return environment.getProperty(key);
	}

	@SuppressWarnings("static-access")
	@Override
	public void setEnvironment(final Environment environment) {
		this.environment = environment;
	}

	/**********************************************************************************************************
	 * Get current timestamp in string format.
	 ***********************************************************************************************************/
	public String getCurrentTimeStampInString() {
		Calendar calendar = Calendar.getInstance();
		return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(calendar.getTime());
	}

	/*********************************************************************************************************************
	 * Call rest POST API.
	 *********************************************************************************************************************/
	public String callRestPostApi(HttpHeaders headers, String url) throws CustomException {

		String response = "";
		try {
			HttpEntity<String> entity = new HttpEntity<>(headers);
			RestTemplate restTemplate = new RestTemplate();
			response = restTemplate.postForObject(url, entity, String.class);
		} catch (Exception exception) {
			LOGGER.info(exception);
			/**
			 * throw new
			 * CustomException(environment.getProperty(CodeGripConstants.POST_API_CALL_FAILED));
			 */
		}
		return response;
	}

	/********************************************************************************************
	 * @throws ParseException
	 *             Convert date string in MM/dd/yyyy format.
	 ********************************************************************************************/
	public String convertDateToString(String date) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date1 = simpleDateFormat.parse(date.substring(0, 10));
		SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("MM/dd/yyyy");
		LOGGER.info(simpleDateFormat1.format(date1));
		return simpleDateFormat1.format(date1);
	}

	/********************************************************************************************
	 * Create progress date format.
	 * 
	 * @throws ParseException
	 ********************************************************************************************/
	public String progressDateFormat(String date) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM d, hh:mm a");
		Date date1 = sdf.parse(date);
		return simpleDateFormat.format(date1);
	}

	/********************************************************************************************
	 * get random profile picture url from constants.
	 ********************************************************************************************/
	public String getRandomProfilePictureURL(String type) {
		String[] profilePictures;
		if ("USER".equalsIgnoreCase(type)) {
			profilePictures = environment.getProperty(CodeGripConstants.USER_AVATAR).split(",");
		} else {
			profilePictures = environment.getProperty(CodeGripConstants.COMPANY_AVATAR).split(",");
		}
		return profilePictures[getRandom() - 1];
	}

	/********************************************************************************************
	 * generate random number.
	 ********************************************************************************************/
	public static int getRandom() {
		return new Random().nextInt(4) + 1;
	}

	/********************************************************************************************
	 * Create startup end date.
	 ********************************************************************************************/
	public Timestamp getStartupEndDate(Integer plusDays) {
		Date date = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DATE, plusDays);
		return new Timestamp(c.getTime().getTime());
	}

	/********************************************************************************************
	 * Get expire date.
	 ********************************************************************************************/
	public Timestamp getExpiryDateByTimeInTimestamp(Integer plusHours, Timestamp startDate) {
		long time = startDate.getTime();
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.add(Calendar.HOUR, plusHours);
		return new Timestamp(cal.getTime().getTime());
	}

	/*********************************************************************************************************
	 * Get day difference method.
	 ********************************************************************************************************/
	public Integer getDayDifference(Timestamp currentDate, Timestamp endDate) {
		/**
		 * Calendar currentDateCal = Calendar.getInstance();
		 * currentDateCal.setTimeInMillis(currentDate.getTime());
		 * 
		 * Calendar endDateCal = Calendar.getInstance();
		 * endDateCal.setTimeInMillis(endDate.getTime());
		 */

		long milliseconds = endDate.getTime() - currentDate.getTime();
		long seconds = milliseconds / 1000;
		return (int) TimeUnit.SECONDS.toDays(seconds);
	}

	/*********************************************************************************************************
	 * Convert date to chart data.
	 ********************************************************************************************************/
	public String convertToChartDate(String analyzeAt) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date1 = simpleDateFormat.parse(analyzeAt.substring(0, 10));
		SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("MM/dd");
		LOGGER.info(simpleDateFormat1.format(date1));
		return simpleDateFormat1.format(date1);
	}

	/**
	 * public static void main(String[] args) throws ParseException {
	 * CommonUtil.convertToChartDate(new
	 * Timestamp(System.currentTimeMillis()).toString()); }
	 */

	
	/*********************************************************************************************************
	 * Generate random string.
	 ********************************************************************************************************/
	public static String generateRandomString(Integer length) {
		byte[] array = new byte[length]; // length of string
		new Random().nextBytes(array);
		return new String(array, Charset.forName("UTF-8"));
	}

}
