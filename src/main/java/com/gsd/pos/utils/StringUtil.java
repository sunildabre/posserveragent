package com.gsd.pos.utils;

public class StringUtil {

	public static void main(String[] args) {

		String sql = "SELECT TENDER.TND_DESCR AS TndDesc"
				+ "	,sum(CS_ITEMTYPE_SLS_BY_TND.INSIDE_SLS_AMT) + sum(CS_ITEMTYPE_SLS_BY_TND.OUTSIDE_SLS_AMT) AS Amount"
				+ " FROM globalstore.dbo.CS_ITEMTYPE_SLS_BY_TND(NOLOCK)"
				+ " INNER JOIN globalstore.dbo.TENDER(NOLOCK)"
				+ "	ON CS_ITEMTYPE_SLS_BY_TND.TND_CD = TENDER.TND_CD"
				+ " WHERE CS_ITEMTYPE_SLS_BY_TND.PRD_AGGR_ID = ("
				+ "		SELECT prd_aggr.prd_aggr_id"
				+ "		FROM globalstore.dbo.prd_aggr" + "		WHERE '#DATE# 12:00:00' BETWEEN strt_dt" + "				AND end_dt"
				+ "			AND tbl_cd = 7" + "		)"
				+ "	AND CS_ITEMTYPE_SLS_BY_TND.SUBPRD_NBR != 0"
				+ " GROUP BY TENDER.TND_DESCR" + "";
		System.out.println(sql.replaceAll("\n", " ").replaceAll("\\s{2,}", " ").trim());
	}

}
