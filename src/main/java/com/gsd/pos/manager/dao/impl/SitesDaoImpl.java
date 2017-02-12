package com.gsd.pos.manager.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.gsd.pos.manager.dao.SitesDao;
import com.gsd.pos.model.CarwashSales;
import com.gsd.pos.model.Discount;
import com.gsd.pos.model.FuelInventory;
import com.gsd.pos.model.FuelSales;
import com.gsd.pos.model.Payment;
import com.gsd.pos.model.ShiftReport;
import com.gsd.pos.model.Site;

public class SitesDaoImpl implements SitesDao {

	private static final Logger logger = Logger.getLogger(SitesDaoImpl.class
			.getName());
	private Connection con;
	private boolean overwrite = System.getProperty("overwrite_reports", "false").equalsIgnoreCase("true");
	
	public SitesDaoImpl(Connection connection)  {
		this.con = connection;
	}

	@Override
	public boolean saveReport(ShiftReport report) {
		return saveReport(report, overwrite);
	}

	public boolean saveReport(ShiftReport report, boolean overwrite) {
		try {
			Long shiftId = this.getShiftId(report.getSiteId(), new DateTime(report.getStartTime()).plusHours(5)
					.toDate());
			if (shiftId != null) {
				logger.warn("There is already a report for this site and day!");
				if (!overwrite) {
					updateSiteCollectedDate(report.getSiteId(), report.getEndTime(),
							con);
					this.updateSiteReason(report.getSiteId(), "");
					return false;
				} else {
					logger.debug("Deleting and inserting again...");
					this.deleteShift(shiftId, con);
				}
			}
			shiftId = addShift(report, con);
			if (report.getFuelSales() != null) {
				for (FuelSales f : report.getFuelSales()) {
					addFuelSales(shiftId, f, con);
				}
			}
			if (report.getPayments() != null) {
				for (Payment f : report.getPayments()) {
					addPayment(shiftId, f, con);
				}
			}
			if (report.getCarwashSales() != null && (!report.getCarwashSales().isEmpty())) {
				for (CarwashSales f : report.getCarwashSales()) {
					addCarWashSales(shiftId, f, con);
				}
			}
			if (report.getFuelInventory() != null && (!report.getFuelInventory().isEmpty())) {
				for (FuelInventory f : report.getFuelInventory()) {
					addFuelInventory(shiftId, f, con);
				}
			}
			if (report.getDiscounts() != null && (!report.getDiscounts().isEmpty())) {
				for (Discount f : report.getDiscounts()) {
					addDiscount(shiftId, f, con);
				}
			}

			updateSiteCollectedDate(report.getSiteId(), report.getEndTime(),
					con);
			if ((report.getStreet() != null) && (report.getCity() != null)
					&& (report.getState() != null) && (report.getZip() != null)) {
				this.updateSiteAddress(report.getSiteId(), report.getStreet(),
						report.getCity(), report.getState(), report.getZip(),
						con);
			}
			if ((report.getStoreName() != null) && (report.getStoreNumber() != null)) {
				this.updateSiteName(report.getSiteId(), report.getStoreName(),
						report.getStoreNumber(), con);
			}
			this.updateSiteReason(report.getSiteId(), "");
		} catch (SQLException e) {
			logger.warn(e);
			e.printStackTrace();
		} finally {
		}
		return true;
	}

	private void addCarWashSales(Long shiftId, CarwashSales f, Connection con) throws SQLException {
		if ((f.getGrossSales() == null) || (f.getNetCount() == null) || (f.getNetCount().intValue() == 0)) {
			logger.debug("No Car wash sales values ..");
			return;
		}
		PreparedStatement st = null;
		try {
			String sql = "insert into carwash_sales (shift_id, gross_sales, refund,"
					+ "discount, net_sales, item_count, refund_count, net_count) "
					+ "values (?,?,?,?,?, ?,?,?) ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setLong(1, shiftId);
			st.setBigDecimal(2, f.getGrossSales());
			st.setBigDecimal(3, f.getRefund());
			st.setBigDecimal(4, f.getDiscount());
			st.setBigDecimal(5, f.getNetSales());
			st.setLong(6, f.getItemCount());
			
			st.setLong(7, f.getRefundCount());
			st.setLong(8, f.getNetCount());
			int inserted = st.executeUpdate();
			logger.debug("Inserted");
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	private void addFuelInventory(Long shiftId, FuelInventory f, Connection con) throws SQLException {
		PreparedStatement st = null;
		try {
			String sql = "insert into fuel_inventory (shift_id, tank_number, product_name,"
					+ "volume, date_time) "
					+ "values (?,?,?,?, ?) ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setLong(1, shiftId);
			st.setLong(2, f.getTankId());
			st.setString(3, f.getGradeName());
			st.setBigDecimal(4, f.getVolume());
			if (f.getDatetime() != null) {
				st.setTimestamp(5, new java.sql.Timestamp(f.getDatetime().getTime()));
				
			} else {
				st.setNull(5, Types.DATE);
	
			}
	
			int inserted = st.executeUpdate();
			logger.debug("Inserted");
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	private void addFuelSales(Long shiftId, FuelSales f, Connection con) throws SQLException {
		PreparedStatement st = null;
		try {
			String sql = "insert into fuel_sales (shift_id, grade, grade_name,"
					+ "volume, sales, percent_of_total_sales) "
					+ "values (?,?,?,?,?, ?) ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setLong(1, shiftId);
			st.setString(2, f.getGrade());
			st.setString(3, f.getGradeName());
			st.setBigDecimal(4, f.getVolume());
			st.setBigDecimal(5, f.getSales());
			st.setBigDecimal(6, f.getPercentOfTotalSales());
			int inserted = st.executeUpdate();
			logger.debug("Inserted");
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	
	private void addDiscount(Long shiftId, Discount d, Connection con) throws SQLException {
		PreparedStatement st = null;
		try {
			if (d.getCount() ==0  ) {
				logger.debug("Not inserting discount , since it is 0");
				return;
			}
			String sql = "insert into discount (shift_id, grade,"
					+ "count, amount) "
					+ "values (?,?,?,?) ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setLong(1, shiftId);
			st.setString(2, d.getGrade());
			st.setLong(3, d.getCount());
			st.setBigDecimal(4, d.getAmount());
			int inserted = st.executeUpdate();
			logger.debug("Inserted");
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	
	private void addPayment(Long shiftId, Payment p, Connection con) throws SQLException {
		PreparedStatement st = null;
		try {
			String sql = "insert into payment (shift_id,payment_type, amount, count  ) "
					+ "values (?,?,?,?) ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setLong(1, shiftId);
			st.setString(2, p.getType());
			st.setBigDecimal(3, p.getAmount());
			if (p.getCount() == null) {
				st.setNull(4, Types.NUMERIC);
			} else {
				st.setLong(4, p.getCount());
			}
			int inserted = st.executeUpdate();
			logger.debug("Inserted");
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	private Long addShift(ShiftReport sr,
			Connection con) throws SQLException {
		PreparedStatement st = null;
		Long shiftId = null;
		try {
			String sql = "insert into shift (site_id, start_time, end_time,"
					+ " pos_shift_id, operator_name, operator_id,  total_fuel_sales,"
					+ "total_fuel_discounts, total_other_discounts, total_tax_collected, total_non_fuel_sales )" +
					" values (?,?,?,?,?,?, ?,?,?,?, ?) ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			st.setLong(1, sr.getSiteId());
			st.setTimestamp(2, new java.sql.Timestamp(sr.getStartTime().getTime()));
			st.setTimestamp(3, new java.sql.Timestamp(sr.getEndTime().getTime()));
			if (sr.getPosShiftId() == null) {
				st.setNull(4, Types.NUMERIC);
			} else {
				st.setLong(4, sr.getPosShiftId());
			}
			st.setString(5, (sr.getOperatorName() == null) ? "" : sr.getOperatorName());
			if (sr.getOperatorId() == null) {
				st.setNull(6, Types.NUMERIC);
			} else {
				st.setNull(6, Types.NUMERIC);
			}
			if (sr.getTotals() != null) {
				st.setBigDecimal(7, sr.getTotals().getTotalFuelSales());
				st.setBigDecimal(8, sr.getTotals().getFuelDiscounts());
				st.setBigDecimal(9, sr.getTotals().getOtherDiscounts());
				st.setBigDecimal(10, sr.getTotals().getTotalTax());
				st.setBigDecimal(11, sr.getTotals().getTotalDeptSales());
			} else {
				st.setNull(7, Types.NUMERIC);
				st.setNull(8, Types.NUMERIC);
				st.setNull(9, Types.NUMERIC);
				st.setNull(10, Types.NUMERIC);
				st.setNull(11, Types.NUMERIC);
			}
			int rows = st.executeUpdate();
			logger.trace("Inserted [" + rows + "] records");
			ResultSet rs = st.getGeneratedKeys();
			if (rs.next()) {
				shiftId = rs.getLong(1);
				logger.debug("Shift id is [" + shiftId + "]");
			}
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
		return shiftId;
	}

	private void updateSiteCollectedDate(Long siteId, Date endTime,
			Connection con) throws SQLException {
		PreparedStatement st = null;
		try {
			String sql = "update site set last_collected_date = ? where  site_id = ? ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setTimestamp(1, new java.sql.Timestamp(endTime.getTime()));
			st.setLong(2, siteId);
			int updated = st.executeUpdate();
			logger.debug("updated");
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	@Override
	public void updateSiteReason(Long siteId, String reason) {
		PreparedStatement st = null;
		try {
			String sql = "update site set reason = ? where  site_id = ? ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setString(1, reason);
			st.setLong(2, siteId);
			int updated = st.executeUpdate();
			logger.debug("updated");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	private void deleteShift(Long shiftId, Connection con) throws SQLException {
		PreparedStatement st = null;
		try {
			String sql = "delete from shift where shift_id = ? ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setLong(1, shiftId);
			int deleted = st.executeUpdate();
			logger.debug("deleted");
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	public Long getShiftId(Long siteId, Date date) {
		PreparedStatement st = null;
		Long shiftId = null;
		try {
			String sql = "select shift_id from shift where site_id = ?" +
					" and ? between start_time and end_time    ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setLong(1, siteId);
			st.setTimestamp(2, new java.sql.Timestamp(date.getTime()));
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				shiftId = rs.getLong(1);
				logger.debug("Shift id is [" + shiftId + "]");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
		return shiftId;
	}

	private void updateSiteAddress(Long siteId, String street, String city,
			String state, String zip, Connection con) throws SQLException {
		PreparedStatement st = null;
		try {
			String sql = "update site set street = ?, city = ? , state = ?, zip = ? where  site_id = ? ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setString(1, street);
			st.setString(2, city);
			st.setString(3, state);
			st.setString(4, zip);
			st.setLong(5, siteId);
			int updated = st.executeUpdate();
			logger.debug("updated");
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	private void updateSiteName(Long siteId, String siteName, String siteNumber, Connection con) throws SQLException {
		PreparedStatement st = null;
		try {
			String sql = "update site set site_name = ?, store_number = ?  where  site_id = ? ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setString(1, siteName);
			st.setString(2, siteNumber);
			st.setLong(3, siteId);
			int updated = st.executeUpdate();
			logger.debug("updated");
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
	}

	@Override
	public Site getSite(Long siteId) {
		Site c = null;
		PreparedStatement st = null;
		try {
			String sql = "select * from site where site_id = ?";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			st.setLong(1, siteId);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				c = create(rs);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
		return c;
	}

	@Override
	public List<Site> getSites() {
		List<Site> sites = new ArrayList<Site>();
		PreparedStatement st = null;
		try {
			String sql = "select * from site  ";
			logger.trace("Executing sql " + sql);
			st = con.prepareStatement(sql);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				Site c = create(rs);
				sites.add(c);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException se) {
			}
		}
		return sites;
	}

	private Site create(ResultSet rs) throws SQLException {
			Site s = new Site();
			s.setName(rs.getString("site_name"));
			s.setSiteId(rs.getLong("site_id"));
			s.setStoreNumber(rs.getString("store_number"));
			s.setLastCollectedDate(rs.getTimestamp("last_collected_date"));
			s.setStreet(rs.getString("street"));
			s.setState(rs.getString("state"));
			s.setZip(rs.getString("zip"));
			s.setIp(rs.getString("ip"));
			s.setCity(rs.getString("city"));
			s.setReason(rs.getString("reason"));
			s.setPosIp(rs.getString("pos_ip"));
			s.setConnectionType(rs.getString("connection_type"));
			s.setSqlVersion(rs.getString("sql_version"));
	//		s.setCarwashEnabled(rs.getBoolean("enable_carwash"));
	//		s.setFuelInventoryEnabled(rs.getBoolean("enable_fuel_inventory"));
			return s;
		}

}
