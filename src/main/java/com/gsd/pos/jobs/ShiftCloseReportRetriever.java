package com.gsd.pos.jobs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;

import com.google.gson.Gson;
import com.gsd.pos.agent.dao.impl.SQLServerConnectionProvider;
import com.gsd.pos.agent.dao.impl.ShiftCloseReportDaoImpl;
import com.gsd.pos.manager.dao.SitesDao;
import com.gsd.pos.manager.dao.impl.MySqlConnectionProvider;
import com.gsd.pos.manager.dao.impl.SitesDaoImpl;
import com.gsd.pos.model.ShiftReport;
import com.gsd.pos.model.Site;
import com.gsd.pos.utils.Config;

public class ShiftCloseReportRetriever implements Runnable {
	private SitesDao dao;
	private static final Logger logger = Logger
			.getLogger(ShiftCloseReportRetriever.class.getName());
	static {
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});
	}

	public void run() {
		logger.debug("Starting report retrieval thread ...");
		try {
			retrieveAndStoreReportsForAll();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	public void retrieveAndStoreReportsForAll() throws SQLException {
		logger.debug("Starting report retrieval ...");
		MySqlConnectionProvider connection = new MySqlConnectionProvider();

		dao = new SitesDaoImpl(connection);
		List<Site> sites = dao.getSites();
		logger.debug("Got [" + sites.size() + "] sites");
		for (Site s : sites) {
			retrieveAndStoreReports(s);
		}
	}

	public void retrieveAndStoreReports(Long siteId) throws SQLException {
		MySqlConnectionProvider connection = new MySqlConnectionProvider();

		dao = new SitesDaoImpl(connection);
		retrieveAndStoreReports(dao.getSite(siteId));
	}

	public void retrieveAndStoreReports(Site s) {
		DateTime now = new DateTime();
		try {
			logger.debug(String.format("Processing site [%s]  ", s.getName()));
			if ((s.getIp() == null) || (s.getIp().isEmpty())) {
				logger.warn(String.format(
						"Ip for site [%s] is not set,cannot fetch ",
						s.getName()));
				return;
			}
			logger.debug("Last Collected date is ["
					+ ((s.getLastCollectedDate() == null) ? null : s.getLastCollectedDate()) + "]");
			DateTime dt = (s.getLastCollectedDate() == null) ? now.minusDays(7)
					: new DateTime(s.getLastCollectedDate());
			long reportsToRetrieve = Days.daysBetween(dt.toDateMidnight() , now.toDateMidnight() ).getDays();
//			long reportsToRetrieve = new Duration(dt, now).getStandardDays();
			logger.debug(String.format("Collecting from [%s] , sending [%s] requests ", dt.toString(),
					reportsToRetrieve));
			int repeat = (int) reportsToRetrieve;
			for (int i = 0; i <= repeat; i++) {
				ShiftReport report = retrieveReport(s, dt);
				report.setSiteId(s.getSiteId());
				if ((report.getStartTime() == null) || (report.getEndTime() == null)) {
					logger.debug("No shift information found !!");
					dt = dt.plusDays(1);
					continue;
				}
				if (new Duration(new DateTime(report.getStartTime()), new DateTime(report.getEndTime()))
						.getStandardDays() > 7) {
					logger.debug("Shift Not Closed!!");
					dt = dt.plusDays(1);
					continue;
				}
				boolean saved = dao.saveReport(report);
				logger.debug("Report "
						+ ((saved) ? " inserted" : " already existed!"));
				dt = dt.plusDays(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn(String.format("Error fetching for site [%s] ", s.getName()));
			logger.warn(e.getMessage());
			try {
				dao.updateSiteReason(s.getSiteId(), e.getMessage());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	private ShiftReport pullDirect(Site site, DateTime date)
			throws MalformedURLException, IOException, ProtocolException,
			Exception {
		SQLServerConnectionProvider connection = new SQLServerConnectionProvider(site.getPosIp(), "", "", "");
		ShiftCloseReportDaoImpl dao = new ShiftCloseReportDaoImpl(connection, this.loadSQLs(site.getSqlVersion()));
		return dao.getReport(date.toDate());
		
	}

	
	private ShiftReport pullViaAgent(Site site, String date)
			throws MalformedURLException, IOException, ProtocolException,
			Exception {
		URL url = new URL("https://" + site.getIp()
				+ "/reports?name=shift_close&date=" + date);
		logger.debug("Connecting to [" + url.toString() + "]");
		System.out.println("Connecting to [" + url.toString() + "]");

		HttpsURLConnection conn = (HttpsURLConnection) url
				.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		if (conn.getResponseCode() != 200) {
			throw new Exception("Failed : HTTP error code : "
					+ conn.getResponseCode());
		}
		BufferedReader in = new BufferedReader(
				new InputStreamReader(conn.getInputStream()));
		StringBuffer r = new StringBuffer();
		String res = null;
		while ((res = in.readLine()) != null) {
			r.append(res);
		}
		conn.disconnect();
		Gson gson = new Gson();
		ShiftReport report = gson.fromJson(r.toString(),
				ShiftReport.class);

		return report;
	}


	private ShiftReport retrieveReport(Site site, DateTime date)
			throws MalformedURLException, IOException, ProtocolException,
			Exception {
		String c = site.getConnectionType();
		String d = date.toString("yyyy.MM.dd");

		if("direct".equalsIgnoreCase(c)) {
			return this.pullDirect(site, date);
		}
		return this.pullViaAgent(site, d);
	
	}


	
	private  Properties loadSQLs(String type) {
		Properties props = new Properties();
		try {
			File propsFile = new File(type + ".sqls.txt");
			if (propsFile.exists()) {
				logger.trace("loading file from current directory");
				props.load(new FileInputStream(propsFile));
			}
		} catch (IOException ex) {
			logger.warn(ex);
			logger.warn(ex.getMessage());
			ex.printStackTrace();
		}
		return props;
	}

	public static void main(String[] args) throws MalformedURLException, ProtocolException, IOException, Exception {
		String certpath = System.getProperty("posman.home", "/Users/sunildabre/Documents/workspace/posman") + 
				File.separator + 	"posagent.ks" ;
		logger.debug("Cert path [" + certpath + "]");
		System.setProperty("javax.net.ssl.keyStore", certpath);
		System.setProperty("javax.net.ssl.keyStorePassword", "password");
		System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
		System.setProperty("javax.net.ssl.trustStore", certpath);

		Site site = new Site();
		//801 washingtons
		site.setIp("96.91.220.2");
		site.setConnectionType	("agent");
		ShiftCloseReportRetriever retriever = new  ShiftCloseReportRetriever();
		ShiftReport s = retriever.retrieveReport(site, new DateTime("2016.12.14"));
		System.out.println(s.toString());
	}
}
