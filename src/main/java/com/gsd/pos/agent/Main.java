package com.gsd.pos.agent;

import java.io.File;

import org.apache.log4j.Logger;

import com.gsd.pos.agent.dao.impl.ShiftCloseReportDaoImpl;
import com.gsd.pos.jobs.JobManager;

public class Main {
	private static final Logger logger = Logger
			.getLogger(Main.class.getName());
	public static void main(String[] args) {

		String certpath = System.getProperty("posserveragent.home","")
				+  "posagent.ks";
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
		JobManager.getInstance().start();
	}
}
