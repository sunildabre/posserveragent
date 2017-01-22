package com.gsd.pos.manager.dao;

import java.util.List;

import com.gsd.pos.model.ShiftReport;
import com.gsd.pos.model.Site;

public interface SitesDao {
	public boolean saveReport(ShiftReport report);

	Site getSite(Long siteId);

	List<Site> getSites();

	void updateSiteReason(Long siteId, String reason);


}
