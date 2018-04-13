package com.blacklist.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.spi.FormatConversionProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import com.SCTServer.Scheduler.SchedulerClass;
import com.agiserver.helper.DBConnectionManager;
//import com.blacklist.common.DBConnectionManager;
import com.agiserver.helper.DBHelper;
import com.agiserver.helper.common.ConfigurationLoader;
import com.blacklist.common.Helper;

public class blacklistScheduler implements Job{
	private static final Logger logger = Logger.getLogger(blacklistScheduler.class);
	boolean syncBlackList = false;
	boolean syncSubscriberIVR = false;
	
	public void execute(JobExecutionContext context) {
		Object obj = null;
		try {
			obj = new blacklistScheduler();

			SchedulerClass localSchedulerClass = new SchedulerClass(context, obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void syncBlackList() {
		try {
			if(syncBlackList){
				logger.info("scheduler already running");
				return;
			}
				syncBlackList = true;
				String query = "SELECT * FROM srvc_def WHERE status > 0 and srvc_name IN ('mystatus','drama','introme','audio_game')";
				Connection conn = DBConnectionManager.getInstance().getConnection();

				List<Map<String, Object>> rows = DBHelper.getInstance().query(query, conn, new Object[0]);
				
				if ((rows != null) && (rows.size() > 0)) {
					JSONArray rlist = new JSONArray();
					int i = 0;
					for (Map<String, Object> row : rows) {
						logger.info("Getting list of msisdns to be sync");
						String getBlList = "SELECT *,IF(bl_type='BLOCK',1,IF(bl_type='PURGE',2,IF(bl_type='UNSUB',3,IF(bl_type='QOUTA',4,0)))) as blacklist_type, DATE_FORMAT(dt,'%d-%m-%Y %T') as formated_dt FROM bl_"+row.get("srvc_id").toString()+" WHERE is_sync = -100 AND (bl_type IN ('PURGE','UNSUB') OR (bl_type = 'BLOCK' AND mode = 'SMS')) LIMIT "+ ConfigurationLoader.getProperty("SYNC_LIMIT").toString();
						List<Map<String, Object>> list = DBHelper.getInstance().query(getBlList, conn, new Object[0]);
						
						if((list != null) && (list.size() > 0)){
							
							for(Map<String, Object> l: list){
								String srvcName = row.get("srvc_name").toString();
								String cellno = Helper.formatCellNumber(l.get("cellno").toString());
								String blType = l.get("blacklist_type").toString();
								String mode = l.get("mode").toString();
								String timestamp = l.get("formated_dt").toString();
								String is_sub;
								Map<String, Object> params = new HashMap<String, Object>();
								params.put("msisdn", cellno);
								params.put("service_name", srvcName);
								params.put("blacklist_type", blType);
								params.put("timestamp", timestamp);
								
								Map<String, Object> res = DBHelper.getInstance().firstRow(row.get("sql_is_subscribed").toString(), DBConnectionManager.getInstance().getConnection(row.get("srvc_db_url").toString(), row.get("srvc_db_usr").toString(), row.get("srvc_db_pwd").toString()), new Object[]{l.get("cellno").toString()});
								if(res != null){
									is_sub = "true";
									
								}else{
									is_sub = "false";
								}
								params.put("is_subscriber", is_sub);
								String[] response = sendPostReq(params, ConfigurationLoader.getProperty("BASE_URL"));
							//	conn = DBConnectionManager.getInstance().getConnection();
								logger.info("msisdn:"+cellno+", service_name:"+srvcName+", blacklist_type:"+blType+", is_subscriber:"+is_sub+", timestamp:"+timestamp+", updating record");
								if(response[0].equals("200")){
									if(response[1].equals("{\"responseCode\":1, \"description\":\"Success\"}")){
										String updatequery = "update bl_"+row.get("srvc_id")+" set is_sync=100 where cellno='"+l.get("cellno").toString()+"'";
									int result =	DBHelper.getInstance().executeDml(updatequery, conn,new Object[0]);
										logger.info("msisdn:"+cellno+", service_name:"+srvcName+", blacklist_type:"+blType+", is_subscriber:"+is_sub+", timestamp:"+timestamp+", has been synced");
									}else{
										String updatequery = "update bl_"+row.get("srvc_id")+" set is_sync=-500 where cellno='"+l.get("cellno").toString()+"'";
										int result =	DBHelper.getInstance().executeDml(updatequery, conn,new Object[0]);
										logger.info("msisdn:"+cellno+", service_name:"+srvcName+", blacklist_type:"+blType+", is_subscriber:"+is_sub+", timestamp:"+timestamp+", issue syncing data");
									}	
								}else{
									logger.error("http client request failed:"+response[0]+" "+ response[1]);
									String updatequery = "update bl_"+row.get("srvc_id")+" set is_sync=-500 where cellno='"+l.get("cellno").toString()+"'";
									int result =	DBHelper.getInstance().executeDml(updatequery, conn,new Object[0]);
									continue;
								}
							}
						}else{
							logger.info("no msisdn found to be synced");
						}
					}
			}
		} catch (Exception e) {
			syncBlackList = false;
			logger.error(e);
			StringBuilder sb = new StringBuilder();
		    for (StackTraceElement element : e.getStackTrace()) {
		        sb.append(element.toString());
		        sb.append("\n");
		    }
			logger.error(sb);
		}finally{
			syncBlackList = false;
		}
	}
	
	public void syncSubscriberIVR() {
		try {
			if(syncSubscriberIVR){
				logger.info("scheduler already running");
				return;
			}
			syncSubscriberIVR = true;
				String query = "SELECT * FROM srvc_def WHERE status > 0 and srvc_name IN ('mystatus','drama','introme','audio_game')";
				Connection conn = DBConnectionManager.getInstance().getConnection();

				List<Map<String, Object>> rows = DBHelper.getInstance().query(query, conn, new Object[0]);
				if ((rows != null) && (rows.size() > 0)) {
					JSONArray rlist = new JSONArray();
					int i = 0;
					for (Map<String, Object> row : rows) {
						logger.info("Getting list of msisdns to be sync");
						String getBlList = "SELECT *, DATE_FORMAT(dt,'%d-%m-%Y %T') as formated_dt FROM bl_"+row.get("srvc_id").toString()+" WHERE is_sync = -100 AND bl_type = 'SUBSCRIBED' LIMIT "+ ConfigurationLoader.getProperty("SYNC_LIMIT").toString();
						List<Map<String, Object>> list = DBHelper.getInstance().query(getBlList, conn, new Object[0]);
						
						if((list != null) && (list.size() > 0)){
							
							for(Map<String, Object> l: list){
								String srvcName = row.get("srvc_name").toString();
								String cellno = Helper.formatCellNumber(l.get("cellno").toString());
								String expTimestamp ;
								String subStatus;
								Map<String, Object> params = new HashMap<String, Object>();
								params.put("msisdn", cellno);
								params.put("service_name", srvcName);
								Map<String, Object> res = DBHelper.getInstance().firstRow(row.get("sql_is_subscribed").toString(), DBConnectionManager.getInstance().getConnection(row.get("srvc_db_url").toString(), row.get("srvc_db_usr").toString(), row.get("srvc_db_pwd").toString()), new Object[]{l.get("cellno").toString()});
								if(res != null){
									subStatus = "1";
									logger.info("expiry_timestamp:"+(String) res.get("expiry_timestamp"));
									expTimestamp =(String) res.get("expiry_timestamp");
									
								}else{
									subStatus = "2";
									expTimestamp = null;
								}
								params.put("subscription_status", subStatus);
								params.put("expiry_timestamp", expTimestamp);
								String[] response = sendPostReq(params, ConfigurationLoader.getProperty("SUB_SYNC_URL"));
							//	conn = DBConnectionManager.getInstance().getConnection();
								logger.info("msisdn:"+cellno+", service_name:"+srvcName+", subscription_status:"+subStatus+", expiry_timestamp:"+expTimestamp+" updating record");
								if(response[0].equals("200")){
									if(response[1].equals("{\"responseCode\":1, \"description\":\"Success\"}")){
										String updatequery = "update bl_"+row.get("srvc_id")+" set is_sync=100 where cellno='"+l.get("cellno").toString()+"'";
										int result =	DBHelper.getInstance().executeDml(updatequery, conn,new Object[0]);
										logger.info("msisdn:"+cellno+", service_name:"+srvcName+", subscription_status:"+subStatus+", expiry_timestamp:"+expTimestamp+" has been synced");
									}else{
										String updatequery = "update bl_"+row.get("srvc_id")+" set is_sync=-500 where cellno='"+l.get("cellno").toString()+"'";
										int result = DBHelper.getInstance().executeDml(updatequery, conn,new Object[0]);
										logger.info("msisdn:"+cellno+", service_name:"+srvcName+", subscription_status:"+subStatus+", expiry_timestamp:"+expTimestamp+", issue syncing data");
									}	
								}else{
									logger.error("http client request failed:"+response[0]+" "+ response[1]);
									String updatequery = "update bl_"+row.get("srvc_id")+" set is_sync=-500 where cellno='"+l.get("cellno").toString()+"'";
									int result =	DBHelper.getInstance().executeDml(updatequery, conn,new Object[0]);
									continue;
								}
							}
						}else{
							logger.info("no msisdn found to be synced");
						}
					}
			}
		} catch (Exception e) {
			syncSubscriberIVR = false;
			logger.error(e);
			StringBuilder sb = new StringBuilder();
		    for (StackTraceElement element : e.getStackTrace()) {
		        sb.append(element.toString());
		        sb.append("\n");
		    }
			logger.error(sb);
		}finally{
			syncSubscriberIVR = false;
		}
	}
	
	String[] sendPostReq(Map<String, Object> params, String url) throws Exception {
		
		logger.info("calling webservice addInServiceBlacklist");
		
		String[] res = HttpUtil.executePostFormDataRequest(url, params, params);
//		HttpClient httpClient = new DefaultHttpClient();
//		logger.info("httpclient created");
//		String params = "?msisdn="+ msisdn+"&service_name="+ service_name+"&blacklist_type="+ blacklist_type+"&is_subscriber="+ Is_subscriber+"&timestamp="+URLEncoder.encode(Timestamp, "UTF-8");
//		HttpPost request = new HttpPost(ConfigurationLoader.getProperty("BASE_URL") + params);
////		request.setParams(Htt);
////		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
////		entityBuilder.addTextBody("msisdn", msisdn);
////		entityBuilder.addTextBody("service_name", service_name);
////		entityBuilder.addTextBody("blacklist_type", blacklist_type);
////		entityBuilder.addTextBody("Is_subscriber", Is_subscriber);
////		entityBuilder.addTextBody("Timestamp", Timestamp);
////		HttpEntity entity = entityBuilder.build();
//		//request.setEntity(entity);
//		request.setHeader("content-type", "application/x-www-form-urlencoded");
//		//post.setHeader("Content-type", "application/json");
//		logger.info(request.toString());
//		logger.info(request);
//		HttpResponse response = httpClient.execute(request);
//
//		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//
//		StringBuffer result = new StringBuffer();
//		String line = "";
//		while ((line = rd.readLine()) != null) {
//			result.append(line);
//		}
//
//		logger.info("Response: " + result.toString());
//		
//		return result.toString();
		logger.info(res[0]+res[1]);
		return res;

	}
		
}
