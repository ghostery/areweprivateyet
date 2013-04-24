package com.evidon.arewebetteryet;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONArray;
import org.json.JSONObject;

public class Aggregator {
	// VM prop: -Dawby_path=C:/Users/fixanoid-work/Desktop/arewebetteryet/bin/
	String path = System.getProperty("awby_path");

	Map<String, Analyzer> results = new LinkedHashMap<String, Analyzer>();
	Map<String, Map<String, String>> totals = new LinkedHashMap<String, Map<String, String>>();
	Map<String, Map<String, String>> decrease = new LinkedHashMap<String, Map<String, String>>();
	
	// list of baseline domains.
	List<String> domains = new ArrayList<String>();
	
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
			case "requestCountPerDomain":
				mapToUse = ra.requestCountPerDomain;
				break;
		}
		
		return mapToUse;
	}
	
	private void createContent(Workbook wb, Sheet s, String map) {
		Map<String, String> out = new HashMap<String, String>();
		
		int rownum = 2;
		int cellnum = 0;

		// create a merged list of domains.
		domains.clear();
		for (String database : results.keySet()) {
			if (database.equals("baseline")) {
				Analyzer ra = results.get(database);
				Map<String, Integer> mapToUse = this.getMap(map, ra);

				for (String domain : mapToUse.keySet()) {
					if (!domains.contains(domain)) {
						domains.add(domain);
						out.put(domain, "");
					}
				}
			}
		}

		CellStyle numberStyle = wb.createCellStyle();
		numberStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("number"));
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
								
				c.setCellStyle(numberStyle);
						
				cellnum++;
			}
			rownum++;
		}

		
		// Totals.
		rownum++;
		cellnum = 1;
		Row r = s.createRow(rownum);
		
		Cell c = r.createCell(0);
		c.setCellValue("Totals:");
		
		for (int i = 0; i < results.keySet().size(); i++) {
			c = r.createCell(cellnum);
			c.setCellType(Cell.CELL_TYPE_FORMULA);
			c.setCellFormula("SUM(" + getCellLetter(i) + "3:" + getCellLetter(i) + (domains.size() + 2) + ")");
			
			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
			evaluator.evaluateFormulaCell(c);

			if (!totals.containsKey(s.getRow(1).getCell(i + 1).getStringCellValue())) {
				Map<String, String> contents = new LinkedHashMap<String, String>();
				contents.put(s.getSheetName(), c.getNumericCellValue() + "");
				
				totals.put(s.getRow(1).getCell(i + 1).getStringCellValue(), contents);
			} else {
				Map<String, String> contents = totals.get(s.getRow(1).getCell(i + 1).getStringCellValue());
				contents.put(s.getSheetName(), c.getNumericCellValue() + "");
				
				totals.put(s.getRow(1).getCell(i + 1).getStringCellValue(), contents);
			}
			
			cellnum++;
		}

		// Delta/Reduction
		rownum++;
		cellnum = 1;
		r = s.createRow(rownum);
		
		c = r.createCell(0);
		c.setCellValue("Tracking Decrease:");
		
		for (int i = 0; i < results.keySet().size(); i++) {
			c = r.createCell(cellnum);
			c.setCellType(Cell.CELL_TYPE_FORMULA);
			c.setCellFormula("ROUND((100-(" + getCellLetter(i) + (rownum) + "*100/B" + (rownum) + ")),0)");
			
			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
			evaluator.evaluateFormulaCell(c);

			if (!decrease.containsKey(s.getRow(1).getCell(i + 1).getStringCellValue())) {
				Map<String, String> contents = new LinkedHashMap<String, String>();
				contents.put(s.getSheetName(), c.getNumericCellValue() + "");
				
				decrease.put(s.getRow(1).getCell(i + 1).getStringCellValue(), contents);
			} else {
				Map<String, String> contents = decrease.get(s.getRow(1).getCell(i + 1).getStringCellValue());
				contents.put(s.getSheetName(), c.getNumericCellValue() + "");
				
				decrease.put(s.getRow(1).getCell(i + 1).getStringCellValue(), contents);
			}
			
			cellnum++;
		}
	}
	
	private static String getCellLetter(int i) {
		String letter = "";

		if (i == 0) {
			letter = "B";
		} else if (i == 1) {
			letter = "C";
		} else if (i == 2) {
			letter = "D";
		} else if (i == 3) {
			letter = "E";
		} else if (i == 4) {
			letter = "F";
		} else if (i == 5) {
			letter = "G";
		} else if (i == 6) {
			letter = "H";
		} else if (i == 7) {
			letter = "I";
		} else if (i == 8) {
			letter = "J";
		} else if (i == 9) {
			letter = "K";
		} else if (i == 10) {
			letter = "L";
		} else if (i == 12) {
			letter = "M";
		} else if (i == 13) {
			letter = "N";
		} else if (i == 14) {
			letter = "O";
		}
		
		return letter;
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

	private void createJson() throws Exception {
		JSONObject json = new JSONObject();
		json.put("date", System.currentTimeMillis());
		JSONArray jsonArray = new JSONArray();
		
		for (String database : results.keySet()) {
			jsonArray.put(database);
		}
		
		json.put("dataset", jsonArray);
		
		JSONObject jsonTotals = new JSONObject();
		for (String database : totals.keySet()) {
			JSONObject jsonTypes = new JSONObject();
			for (String type : totals.get(database).keySet()) {
				jsonTypes.put(type, Double.parseDouble(totals.get(database).get(type)));
			}
			jsonTotals.put(database, jsonTypes);
		}
		
		json.put("totals", jsonTotals);
		
		jsonTotals = new JSONObject();
		for (String database : decrease.keySet()) {
			JSONObject jsonTypes = new JSONObject();
			for (String type : decrease.get(database).keySet()) {
				jsonTypes.put(type, Double.parseDouble(decrease.get(database).get(type)));
			}
			jsonTotals.put(database, jsonTypes);
		}
		
		json.put("decrease", jsonTotals);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(path + "json"));
		bw.write(json.toString());
		bw.close();
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

		
		// content: Pretty Chart
		s = wb.createSheet();
		wb.setSheetName(sheet, "Overall");
		
		int rownum = 0, cellnum = 0;

		// Header
		r = s.createRow(rownum);
		Cell c = r.createCell(0);
		s.setColumnWidth(0, 8000);
		c.setCellValue("Overall effectiveness measured by percentage of decrease vs baseline");
		
		rownum++;
		r = s.createRow(rownum);

		cellnum++;

		for (String database : decrease.keySet()) {
			if (database.equals("baseline")) {
				continue;
			}
			
			c = r.createCell(cellnum);
			c.setCellValue(database);
			
			CellStyle cs = wb.createCellStyle();
			Font f = wb.createFont();
			f.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cs.setFont(f);

			c.setCellStyle(cs);
			cellnum++;
		}
		
		CellStyle numberStyle = wb.createCellStyle();
		numberStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("number"));

		// Content
		for (String type : decrease.get("baseline").keySet()) {
			cellnum = 0;
			rownum++;
			
			r = s.createRow(rownum);
			
			c = r.createCell(cellnum);
			c.setCellValue(type);
			cellnum++;
			
			for (String database : decrease.keySet()) {
				if (database.equals("baseline")) {
					continue;
				}

				c = r.createCell(cellnum);
				c.setCellStyle(numberStyle);
				c.setCellValue(Double.parseDouble(decrease.get(database).get(type)));
				cellnum++;
			}
		}

		/*
		for (String database : decrease.keySet()) {
			for (String type : decrease.get(database).keySet()) {
				System.out.println(database + "|" + type + "|" + decrease.get(database).get(type));
			}
		}
		*/
		
		wb.write(file);
		file.close();
		
		
		// Create JSON output object
		createJson();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Aggregator agg = new Aggregator();

		String[] profiles = {"baseline", "ghostery", "dntme", "disconnect", "abp-fanboy", "abp-easylist", "trackerblock", "requestpolicy", "noscript", "cookies-blocked"};
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
