package com.evidon.arewebetteryet;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class ResultsAggregator {
	HashMap<String, ResultsAnalyzer> results = new HashMap<String, ResultsAnalyzer>();
	
	public ResultsAggregator() { }
	
	public void addResults(String name, String dbFileName) {
		try {
			ResultsAnalyzer ra = new ResultsAnalyzer(dbFileName);
			results.put(name, ra);
		} catch (Exception e) {
			e.printStackTrace();
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
		int rownum = 2, cellnum = 0;
		FileOutputStream file = new FileOutputStream("analysis.xls");

		Workbook wb = new HSSFWorkbook();

		Sheet s = wb.createSheet();
		wb.setSheetName(0, "Content Length");
		this.createHeader(wb, s, "Total Content Length in MB", 0);

		// content: total content length sheet.
		Row r = s.createRow(rownum);
		for (String database : results.keySet()) {
			Cell c = r.createCell(cellnum);
			c.setCellValue(results.get(database).totalContentLength / 1024 / 1024);
			cellnum ++;
		}

		// content: HTTP Requests
		s = wb.createSheet();
		wb.setSheetName(1, "HTTP Requests");
		this.createHeader(wb, s, "Pages with One or More HTTP Requests to the Public Suffix", 1);

		HashMap<String, String> out = new HashMap<String, String>();
		ArrayList<String> domains = new ArrayList<String>();

		// create a merged list of domains.
		for (String database : results.keySet()) {
			ResultsAnalyzer ra = results.get(database);
			
			for (String domain : ra.requestCountPerDomain.keySet()) {
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

			r = s.createRow(rownum);
			Cell c = r.createCell(cellnum);
			c.setCellValue(domain);
			cellnum++;
			
			for (String database : results.keySet()) {
				ResultsAnalyzer ra = results.get(database);

				c = r.createCell(cellnum);
				if (ra.requestCountPerDomain.containsKey(domain)) {
					c.setCellValue(ra.requestCountPerDomain.get(domain));
				} else {
					c.setCellValue(0);
				}
				
				c.setCellStyle(cs);
				
				cellnum++;
			}

			rownum++;
		}

		
		// content: HTTP Set-Cookie Responses
		s = wb.createSheet();
		wb.setSheetName(2, "HTTP Set-Cookie Responses");
		this.createHeader(wb, s, "Pages with One or More HTTP Responses from the Public Suffix That Include a Set-Cookie Header", 1);

		out = new HashMap<String, String>();
		domains = new ArrayList<String>();
		rownum = 2;
		cellnum = 0;

		// create a merged list of domains.
		for (String database : results.keySet()) {
			ResultsAnalyzer ra = results.get(database);
			
			for (String domain : ra.cookiesAdded.keySet()) {
				if (!domains.contains(domain)) {
					domains.add(domain);
					out.put(domain, "");
				}
			}
		}

		cs = wb.createCellStyle();
		cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("number"));
		s.setColumnWidth(0, 5000);

		for (String domain : domains) {
			cellnum = 0;

			r = s.createRow(rownum);
			Cell c = r.createCell(cellnum);
			c.setCellValue(domain);
			cellnum++;
			
			for (String database : results.keySet()) {
				ResultsAnalyzer ra = results.get(database);

				c = r.createCell(cellnum);
				if (ra.cookiesAdded.containsKey(domain)) {
					c.setCellValue(ra.cookiesAdded.get(domain));
				} else {
					c.setCellValue(0);
				}
				
				c.setCellStyle(cs);
				
				cellnum++;
			}

			rownum++;
		}

		wb.write(file);
		file.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ResultsAggregator agg = new ResultsAggregator();

		//String[] profiles = {"baseline", "ghostery", "dntme", "abp-fanboy", "abp-easylist", "trackerblock", "collusion", "disconnect", "noscript"};
		String[] profiles = {"baseline", "ghostery"};
		for (String profile : profiles) {
			agg.addResults(profile, profile + "-fourthparty.sqlite");	
		}
		
		try {
			agg.createSpreadSheet();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
