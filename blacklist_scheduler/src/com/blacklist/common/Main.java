package com.blacklist.common;

import org.apache.log4j.Logger;

import com.SCTServer.DB.DB;
import com.SCTServer.Scheduler.SchedulerServer;
import com.agiserver.helper.common.ConfigurationLoader;

public class Main {
	private static final Logger	logger	= Logger.getLogger(Main.class);

	public static void main(String[] args) {
		init();
	}

	public static void init() {
		
		
		 try
		      {
		        logger.debug("Getting config Properties");
		        logger.debug("Creating  DB Instance");
		        DB db = new DB();
		        logger.debug("Setting DB Credentials");
		        
		        db.setPassword(ConfigurationLoader.getProperty("db.password"));
		        db.setUsername(ConfigurationLoader.getProperty("db.user"));
		        db.setSqlURL(ConfigurationLoader.getProperty("db.url"));
		        String serverID=ConfigurationLoader.getProperty("serverid");
		        logger.info("Starting SchedulerServer");
		        
		        SchedulerServer sc = new SchedulerServer();
		        logger.info("scheduler:======================------" + SchedulerServer.isStarted);
		        if (!SchedulerServer.isStarted)
		        {
		          sc.runServer(db, serverID);
		          logger.info("scheduler:------" + SchedulerServer.isStarted);
		        }
		      }
		      catch (Exception se)
		      {
		        logger.info("Exception While Starting Scheduler",se);
		      }
		
	}
	
}
