package com.blacklist.common;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.*;

import org.apache.log4j.Logger;

import com.agiserver.helper.DBHelper;
import com.agiserver.helper.common.ConfigurationLoader;

public class Helper {

	private static Logger	logger	= Logger.getLogger(Helper.class);

	public static int sendUcip(String cellno, Connection conn) throws Exception {
		String ucipUrl = ConfigurationLoader.getProperty("UCIP_URL") + formatCellNumber(cellno) + "&dlr-url=";
		long requestId = System.currentTimeMillis();
		String responseUrl = ConfigurationLoader.getProperty("UCIP_RESPONSE_URL") + "&requestId=" + requestId + "&msisdn=" + cellno;
		ucipUrl += URLEncoder.encode(responseUrl, "UTF-8");

		// Sent ucip request
		if (invokeUrl(ucipUrl)) {
			// Wait for response
			Thread.sleep(200);
			Map<String, Object> result = null;
			long startTime = System.currentTimeMillis();
			long diff = 0;
			// Timeout is 10000 ms
			long timeOut = Long.valueOf(ConfigurationLoader.getProperty("UCIP_RESPONSE_TIME_OUT", "5000"));
			logger.info("Ucip time out:" + timeOut);
			while (result == null && diff < timeOut) {
				result = DBHelper.getInstance().firstRow("Select * from ucip_response where requestId=?", conn, requestId);
				diff = System.currentTimeMillis() - startTime;
				if (result == null) {
					// sleep for 200 seconds
					Thread.sleep(200);
				}
			}
			if (result != null) {
				if (result.get("response").toString().equals(ConfigurationLoader.getProperty("UCIP_SUCCESS_RESPONSE"))) {
					return 100;
				} else {
					return 0;
				}
			} else {
				return -100;
			}
		}

		return -999;
	}
	
	public static long sendUcip(String cellno) throws Exception {
		String ucipUrl = ConfigurationLoader.getProperty("UCIP_URL") + formatCellNumber(cellno) + "&dlr-url=";
		long requestId = System.currentTimeMillis();
		String responseUrl = ConfigurationLoader.getProperty("UCIP_RESPONSE_URL") + "&requestId=" + requestId + "&msisdn=" + cellno;
		ucipUrl += URLEncoder.encode(responseUrl, "UTF-8");

		// Sent ucip request
		if (invokeUrl(ucipUrl)) {
			return requestId;			
		}

		return -999;
	}

	public static Long sendLocationRequest(int id,String cellno) {
		String URL =ConfigurationLoader.getProperty("JMX_URL");
		long requestId = System.currentTimeMillis();
		try {
			 
			JMXServiceURL serviceUrl = new JMXServiceURL(URL);
			logger.info("service_url:"+ serviceUrl);
			JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl,	null);
			ObjectName objectName = new ObjectName(
					"org.mobicents.slee:name=SleeConnectivityExample");
			MBeanServerConnection mbeanConn = jmxConnector
					.getMBeanServerConnection();

			String requestParam = id+"-"+formatCellNumber(cellno);
			String ret = (String) mbeanConn.invoke(objectName, "fireEvent",
					new Object[] { requestParam },
					new String[] { java.lang.String.class.getName() });
			logger.info("JMX Call Returned" + ret);
			return requestId;
		} catch (Exception e) {
			logger.error("Exception", e);
			return null;
		}
		

	}
	public static String formatCellNumber(String cellNumber) {
		if (cellNumber.startsWith("+92")) {
			cellNumber = cellNumber.substring(1);
		} else if (cellNumber.startsWith("0092")) {
			cellNumber = cellNumber.substring(2);
		} else if (cellNumber.startsWith("03")) {
			cellNumber = "92" + cellNumber.substring(1);
		} else if (cellNumber.startsWith("3")) {
			cellNumber = "92" + cellNumber;
		}
		return cellNumber;
	}
	
	public static boolean invokeUrl(String url) throws java.io.IOException {
		boolean rtnValue = false;
		URL ourl = new URL(url);

		logger.info(url);
		HttpURLConnection c = (HttpURLConnection) ourl.openConnection();
		// Set timeout to 5sec
		c.setConnectTimeout(5000);
		c.setRequestMethod("GET");
		c.connect();
		if ((c.getResponseCode() == HttpURLConnection.HTTP_OK) || (c.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED)) {
			rtnValue = true;
		} else {
			rtnValue = false;
		}

		return rtnValue;
	}

}
