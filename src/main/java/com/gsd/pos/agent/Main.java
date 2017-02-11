package com.gsd.pos.agent;

import org.apache.log4j.Logger;

import com.gsd.pos.jobs.JobManager;
import com.gsd.pos.jobs.ShiftCloseReportRetriever;

public class Main {
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) {

		String certpath = System.getProperty("posserveragent.home", "")
				+ "posagent.ks";
		logger.debug("Cert path [" + certpath + "]");
		System.setProperty("javax.net.ssl.keyStore", certpath);
		System.setProperty("javax.net.ssl.keyStorePassword", "password");
		System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
		System.setProperty("javax.net.ssl.trustStore", certpath);

		try {
			new Main().start(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start(String args[]) throws Exception {
		if (args.length > 0) {
			String siteId = args[0];
			logger.info("Retrieving reports for siteId [" + siteId + "]");
			ShiftCloseReportRetriever retriever = new ShiftCloseReportRetriever();
			retriever.retrieveAndStoreReports(Long.parseLong(siteId));
		} else {
			logger.info("Starting job ro retrieve all reports ....");

			JobManager.getInstance().start();
		}
	}
}
