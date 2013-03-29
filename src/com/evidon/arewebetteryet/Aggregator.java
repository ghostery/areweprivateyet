package com.evidon.arewebetteryet;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class Aggregator {
	// VM prop: -Dawby_path=C:/Users/fixanoid-work/Desktop/arewebetteryet/bin/
	String path = System.getProperty("awby_path");

	Map<String, Analyzer> results = new LinkedHashMap<String, Analyzer>();
	
	public Aggregator() { }
	
	public void addResults(String name, String dbFileName) {
		try {
			System.out.println("Adding " + name + " from " + dbFileName);
			Analyzer ra = new Analyzer(dbFileName);
			results.put(name, ra);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Map<String, Integer> getMap(String map, Analyzer ra) {
		Map<String, Integer> mapToUse = null;
		
		switch (map) {
			case "localStorageContents":
				mapToUse = ra.localStorageContents;
				break;
			case "cookieTotals":
				mapToUse = ra.cookieTotals;
				break;
			case "cookiesAdded":
				mapToUse = ra.cookiesAdded;
				break;
//			case "requestCountPerDomainMinusFirstParties":
//				mapToUse = ra.requestCountPerDomainMinusFirstParties;
//				break;
			case "requestCountPerDomain":
				mapToUse = ra.requestCountPerDomain;
				break;
		}
		
		return mapToUse;
	}
	
	private void createContent(Workbook wb, Sheet s, String map) {
		Map<String, String> out = new HashMap<String, String>();
		List<String> domains = new ArrayList<String>();
		int rownum = 2;
		int cellnum = 0;

		// create a merged list of domains.
		for (String database : results.keySet()) {
			Analyzer ra = results.get(database);
			Map<String, Integer> mapToUse = this.getMap(map, ra);
	
			for (String domain : mapToUse.keySet()) {
				if (!domains.contains(domain)) {
					domains.add(domain);
					out.put(domain, "");
				}
			}
		}

		CellStyle cs = wb.createCellStyle();
		cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("number"));
		s.setColumnWidth(0, 5000);

		for (String domain : domains) {
			cellnum = 0;

			Row r = s.createRow(rownum);
			Cell c = r.createCell(cellnum);
			c.setCellValue(domain);
			cellnum++;
							
			for (String database : results.keySet()) {
				Analyzer ra = results.get(database);

				Map<String, Integer> mapToUse = this.getMap(map, ra);

				c = r.createCell(cellnum);
				try {
					if (mapToUse.containsKey(domain)) {
						c.setCellValue(mapToUse.get(domain));
					} else {
						c.setCellValue(0);
					}
				} catch (Exception e) {
					c.setCellValue(0);
				}
								
				c.setCellStyle(cs);
						
				cellnum++;
			}
			rownum++;
		}
	}
	
	private void createHeader(Workbook wb, Sheet s, String sheetTitle, int skipCell) {
		int rownum = 0, cellnum = 0;
		Row r = null;
		Cell c = null;

		// Header
		r = s.createRow(rownum);
		c = r.createCell(0);
		c.setCellValue(sheetTitle);
		
		rownum++;
		r = s.createRow(rownum);
		
		if (skipCell > 0) {
			c = r.createCell(cellnum);
			c.setCellValue("");
			cellnum++;
		}

		for (String database : results.keySet()) {
			c = r.createCell(cellnum);
			c.setCellValue(database);
			
			CellStyle cs = wb.createCellStyle();
			Font f = wb.createFont();
			f.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cs.setFont(f);

			c.setCellStyle(cs);
			cellnum++;
		}
	}

	public void createSpreadSheet() throws Exception {
		int row = 2, cell = 0, sheet = 0;
		FileOutputStream file = new FileOutputStream(path + "analysis.xls");

		Workbook wb = new HSSFWorkbook();

		// content: total content length sheet.
		Sheet s = wb.createSheet();
		wb.setSheetName(sheet, "Content Length");
		this.createHeader(wb, s, "Total Content Length in MB", 0);

		Row r = s.createRow(row);
		for (String database : results.keySet()) {
			Cell c = r.createCell(cell);
			c.setCellValue(results.get(database).totalContentLength / 1024 / 1024);
			cell ++;
		}
		sheet++;


		// content: HTTP Requests
		s = wb.createSheet();
		wb.setSheetName(sheet, "HTTP Requests");
		this.createHeader(wb, s, "Pages with One or More HTTP Requests to the Public Suffix", 1);
		this.createContent(wb, s, "requestCountPerDomain");
		sheet++;

/*
		// content: HTTP Requests minus First Parties
		s = wb.createSheet();

		wb.setSheetName(sheet, "HTTP Requests minus First Parties");
		this.createHeader(wb, s, "Pages with One or More HTTP Requests to the Public Suffix minus First Parties", 1);
		this.createContent(wb, s, "requestCountPerDomainMinusFirstParties");
		sheet++;
*/		
		
		// content: HTTP Set-Cookie Responses
		s = wb.createSheet();
		wb.setSheetName(sheet, "HTTP Set-Cookie Responses");
		this.createHeader(wb, s, "Pages with One or More HTTP Responses from the Public Suffix That Include a Set-Cookie Header", 1);
		this.createContent(wb, s, "cookiesAdded");
		sheet++;

		
		// content: Cookie Added - Cookie Deleted
		s = wb.createSheet();
		wb.setSheetName(sheet, "Cookies Added-Deleted");
		this.createHeader(wb, s, "Cookies Added - Cookies Deleted Per Domain", 1);
		this.createContent(wb, s, "cookieTotals");
		sheet++;

		
		// content: Local Storage counts per domain
		s = wb.createSheet();
		wb.setSheetName(sheet, "Local Storage");
		this.createHeader(wb, s, "Local Storage counts per domain", 1);
		this.createContent(wb, s, "localStorageContents");
		sheet++;
		
		
		wb.write(file);
		file.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Aggregator agg = new Aggregator();

		String[] profiles = {"baseline", "ghostery", "dntme", "abp-fanboy", "abp-easylist", "trackerblock", "requestpolicy", "disconnect", "noscript"};
		//String[] profiles = {"baseline", "ghostery"};
		for (String profile : profiles) {
			agg.addResults(profile, "fourthparty-" + profile + ".sqlite");	
		}
		
		try {
			agg.createSpreadSheet();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
