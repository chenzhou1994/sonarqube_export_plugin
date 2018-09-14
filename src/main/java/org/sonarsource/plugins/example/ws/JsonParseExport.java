package org.sonarsource.plugins.example.ws;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Action核心处理类
 *
 * @author chenzhou
 */
public class JsonParseExport {
    private static final Logger LOGGER = Loggers.get(JsonParseExport.class);

    /**
     * 核心方法: 将json数据解析并导出为 excel
     *
     * @param measuresJsonData
     * @param issuesJsonData
     * @param imageJsonData
     * @param response
     */
    public static void exportExcel(String measuresJsonData, String issuesJsonData, String imageJsonData, Response response) {
        LOGGER.info("Plugin == [CustomExportAction] --->> exportExcel  start executing...");
        XSSFWorkbook workbook = new XSSFWorkbook();
        if (measuresJsonData != null) {
            //项目总览表
            parseJsonForResult(" 总览 ", measuresJsonData, workbook);
        }
        if (issuesJsonData != null) {
            //项目安全问题表
            parseJsonForTypeSeverityNum(" 安全性|问题 ", issuesJsonData, workbook);
        }
        if (imageJsonData != null) {
            //项目历史检测详情表
            parseJsonForImage(imageJsonData, workbook);
        }
        try {
            response.stream().setMediaType("application/vnd.ms-excel");
            response.setHeader("content-disposition", "attachment;filename=Report.xlsx");
            OutputStream out = response.stream().output();
            if (out != null) {
                workbook.write(out);
                out.close();
                LOGGER.info("Plugin == [CustomExportAction] --->> exportExcel_success  end executing...");
            } else {
                LOGGER.info("Plugin == [CustomExportAction] --->> OutputStream out maybe null...");
            }
        } catch (IOException e) {
            LOGGER.info("Plugin == [CustomExportAction] --->> exportExcel_IOException...");
            e.printStackTrace();
        }

    }

    /**
     * 项目总览表：解析json数据为Result
     *
     * @param data
     * @param workbook
     */
    private static void parseJsonForResult(String componentName, String data, XSSFWorkbook workbook) {
        LOGGER.info("Plugin == [CustomExportAction] --->> parseJsonForResult  start executing...");
        LinkedHashMap<String, String> propertyHeaderMap = new LinkedHashMap<>();
        propertyHeaderMap.put("bugs", "缺陷");
        propertyHeaderMap.put("vulnerabilities", "漏洞");
        propertyHeaderMap.put("codeSmells", "代码异味");
        propertyHeaderMap.put("coverage", "覆盖率(%)");
        propertyHeaderMap.put("duplications", "重复率(%)");
        propertyHeaderMap.put("qualityGate", "质量等级");
        HashMap<String, String> map = new HashMap<>();
        JSONObject obj = JSONObject.fromObject(data);

        JSONObject component = obj.getJSONObject("component");
        JSONArray measuresArray = component.getJSONArray("measures");
        for (int i = 0; i < measuresArray.size(); i++) {
            String metric = (String) measuresArray.getJSONObject(i).get("metric");
            String value = (String) measuresArray.getJSONObject(i).get("value");
            map.put(metric, value);
        }
        Result result = new Result(map);
        LinkedList<Result> list = new LinkedList();
        list.add(result);
        LOGGER.info("Plugin == [CustomExportAction] --->> parseJsonForResult  end executing...");
        generateXlxWorkbook(workbook, 0, componentName, propertyHeaderMap, list);
    }

    /**
     * 项目历史检测详情表：解析json数据为 Image
     *
     * @param jsonData
     * @param workbook
     */
    public static void parseJsonForImage(String jsonData, XSSFWorkbook workbook) {
        //构建 DefaultCategoryDataSet数据集
        DefaultCategoryDataset dataSet = makeDataSet(jsonData);
        //构造核心对象 JFreeChart，并声称文件
        File file = createChartLine("Project History (Issue)", "Time", "Num", dataSet, "10");
        //Image写出为Excel
        imageOut(workbook, file);
    }

    /**
     * 项目安全问题表：解析json数据为 TypeSeverityNum
     *
     * @param jsonData
     * @param workbook
     */
    private static void parseJsonForTypeSeverityNum(String componentName, String jsonData, XSSFWorkbook workbook) {
        LOGGER.info("Plugin == [CustomExportAction] --->> parseJsonForTypeSeverityNum  start executing...");
        LinkedHashMap<String, String> propertyHeaderMap = new LinkedHashMap<>();
        propertyHeaderMap.put("severity", " ");
        propertyHeaderMap.put("severityBugNum", "缺陷");
        propertyHeaderMap.put("severityCodeSmellNum", "代码异味");
        propertyHeaderMap.put("severityVulnerableNum", "漏洞");
        LinkedList<TypeSeverityNum> list = new LinkedList();
        String[] typeArray = {"BUG", "CODE_SMELL", "VULNERABILITY"};
        String[] severityArray = {"BLOCKER", "MINOR", "CRITICAL", "INFO", "MAJOR"};
        LinkedList numList = searchNum(jsonData, severityArray, typeArray);
        TypeSeverityNum t1 = new TypeSeverityNum("阻断", (int[]) numList.get(0));
        TypeSeverityNum t2 = new TypeSeverityNum("次要", (int[]) numList.get(1));
        TypeSeverityNum t3 = new TypeSeverityNum("严重", (int[]) numList.get(2));
        TypeSeverityNum t4 = new TypeSeverityNum("提示", (int[]) numList.get(3));
        TypeSeverityNum t5 = new TypeSeverityNum("主要", (int[]) numList.get(4));
        TypeSeverityNum t6 = new TypeSeverityNum("总计", (int[]) numList.get(5));
        list.add(t1);
        list.add(t2);
        list.add(t3);
        list.add(t4);
        list.add(t5);
        list.add(t6);
        LOGGER.info("Plugin == [CustomExportAction] --->> parseJsonForTypeSeverityNum  end executing...");
        generateXlxWorkbook(workbook, 1, componentName, propertyHeaderMap, list);
    }

    /**
     * 查找符合条件的个数
     *
     * @param jsonData
     * @param severityArray
     * @param typeArray
     * @return
     */
    private static LinkedList<int[]> searchNum(String jsonData, String[] severityArray, String[] typeArray) {
        //String[] typeArray = {"BUG", "CODE_SMELL", "VULNERABILITY"};
        //String[] severityArray = {"BLOCKER", "MINOR", "CRITICAL", "INFO", "MAJOR"};
        LinkedList<int[]> numList = new LinkedList<>();
        int[] numMajor = {0, 0, 0};
        int[] numMinor = {0, 0, 0};
        int[] numBlocker = {0, 0, 0};
        int[] numCritical = {0, 0, 0};
        int[] numInfo = {0, 0, 0};
        int[] numSum = new int[3];
        JSONObject obj = JSONObject.fromObject(jsonData);
        JSONArray issue_array = obj.getJSONArray("issues");
        for (int i = 0; i < issue_array.size(); i++) {
            JSONObject js = JSONObject.fromObject(issue_array.get(i));
            String type_value = (String) js.get("type");
            String severity_value = (String) js.get("severity");
            //(MAJOR、CODE_SMELL) (MINOR、CODE_SMELL)
            for (int j = 0; j < typeArray.length; j++) {
                if (severity_value.equals(severityArray[0]) && type_value.equals(typeArray[j])) {
                    numBlocker[j]++;
                }
                if (severity_value.equals(severityArray[1]) && type_value.equals(typeArray[j])) {
                    numMinor[j]++;
                }
                if (severity_value.equals(severityArray[2]) && type_value.equals(typeArray[j])) {
                    numCritical[j]++;
                }
                if (severity_value.equals(severityArray[3]) && type_value.equals(typeArray[j])) {
                    numInfo[j]++;
                }
                if (severity_value.equals(severityArray[4]) && type_value.equals(typeArray[j])) {
                    numMajor[j]++;
                }
            }
        }
        for (int i = 0; i < numSum.length; i++) {
            numSum[i] = numMajor[i] + numMinor[i] + numBlocker[i] + numCritical[i] + numInfo[i];
        }
        numList.add(numBlocker);
        numList.add(numMinor);
        numList.add(numCritical);
        numList.add(numInfo);
        numList.add(numMajor);
        numList.add(numSum);
        return numList;
    }

    /**
     * 操作excel工作簿
     *
     * @param workbook          工作簿
     * @param sheetNum          表格编号
     * @param sheetTitle        表名
     * @param propertyHeaderMap <property,header>（<T中的property名称、有getter就行, 对应显示在Excel sheet中的列标题>） 用LinkedHashMap保证读取的顺序和put的顺序一样
     * @param dataSet           实体类集合
     * @param <T>
     */
    private static <T> void generateXlxWorkbook(XSSFWorkbook workbook, int sheetNum, String sheetTitle, LinkedHashMap<String, String> propertyHeaderMap, Collection<T> dataSet) {
        LOGGER.info("Plugin == [CustomExportAction] --->> generateXlxWorkbook  start executing...");
        // 生成一个表格
        XSSFSheet sheet = workbook.createSheet();
        workbook.setSheetName(sheetNum, sheetTitle);
        // 设置表格默认列宽度为15个字节
        sheet.setDefaultColumnWidth((int) 15);
        if (sheetNum == 0) {
            // 生成第一行标题行
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        } else if (sheetNum == 1) {
            // 生成第一行标题行
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        }
        XSSFRow r = sheet.createRow(0);
        XSSFCell c = r.createCell(0);
        // 设置第一行标题行内容
        XSSFRichTextString t = new XSSFRichTextString("项目分析详情---[" + sheetTitle + "]");
        c.setCellValue(t);
        // 生成第二行标题行
        XSSFRow row = sheet.createRow(1);
        int i = 0;
        if (sheetNum == 0) {
            for (String key : propertyHeaderMap.keySet()) {
                XSSFCell cell = row.createCell(i);
                XSSFRichTextString text = new XSSFRichTextString(propertyHeaderMap.get(key));
                cell.setCellValue(text);
                i++;
            }
        } else {
            for (String key : propertyHeaderMap.keySet()) {
                if (i > 0) {
                    XSSFCell cell = row.createCell(i);
                    XSSFRichTextString text = new XSSFRichTextString(propertyHeaderMap.get(key));
                    cell.setCellValue(text);
                }
                i++;
            }
        }
        // 生成第三行数据行
        // 循环dataSet，每一条对应一行
        int index = 1;
        for (T data : dataSet) {
            index++;
            row = sheet.createRow(index);
            int j = 0;
            for (String property : propertyHeaderMap.keySet()) {
                XSSFCell cell = row.createCell(j);
                // 拼装getter方法名
                String getMethodName = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
                try {
                    // 利用反射机制获取dataSet中的属性值，填进cell中
                    Class<? extends Object> tCls = data.getClass();
                    Method getMethod = tCls.getMethod(getMethodName, new Class[]{});
                    // 调用getter从data中获取数据
                    Object value = getMethod.invoke(data, new Object[]{});
                    // 判断值的类型后进行类型转换
                    String textValue = null;
                    if (value instanceof Date) {
                        Date date = (Date) value;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        textValue = sdf.format(date);
                    } else {
                        // 其它数据类型都当作字符串简单处理
                        if (value == null) {
                            value = "";
                        }
                        textValue = value.toString();
                    }
                    XSSFRichTextString richString = new XSSFRichTextString(textValue);
                    cell.setCellValue(richString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                j++;
            }
        }
    }

    /**
     * 根据jsonData构造 DefaultCategoryDataSet数据集
     *
     * @param data
     * @return
     */
    private static DefaultCategoryDataset makeDataSet(String data) {
        LOGGER.info("Plugin == [CustomExportAction] --->> makeDataSet  start executing...");
        JSONObject obj = JSONObject.fromObject(data);
        JSONArray measuresArray = obj.getJSONArray("measures");
        JSONArray historyOne = measuresArray.getJSONObject(0).getJSONArray("history");
        JSONArray historyTwo = measuresArray.getJSONObject(1).getJSONArray("history");
        JSONArray historyThree = measuresArray.getJSONObject(2).getJSONArray("history");
        LinkedList<String> timeList = new LinkedList<>();
        LinkedList<Integer> bugList = new LinkedList<>();
        LinkedList<Integer> codeList = new LinkedList<>();
        LinkedList<Integer> VulList = new LinkedList<>();
        for (int i = 0; i < historyOne.size(); i++) {
            String data_ori = (String) historyOne.getJSONObject(i).get("date");
            String value = (String) historyOne.getJSONObject(i).get("value");
            timeList.add(data_ori.substring(0, 10));
            bugList.add(Integer.valueOf(value));
        }
        for (int i = 0; i < historyTwo.size(); i++) {
            String value = (String) historyTwo.getJSONObject(i).get("value");
            codeList.add(Integer.valueOf(value));
        }
        for (int i = 0; i < historyThree.size(); i++) {
            String value = (String) historyThree.getJSONObject(i).get("value");
            VulList.add(Integer.valueOf(value));
        }
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        for (int i = 0; i < timeList.size(); i++) {
            String time = timeList.get(i);
            dataSet.addValue(bugList.get(i), "Bug", time);
            dataSet.addValue(codeList.get(i), "CodeSmell", time);
            dataSet.addValue(VulList.get(i), "Vul", time);
        }
        LOGGER.info("Plugin == [CustomExportAction] --->> makeDataSet  end executing...");
        return dataSet;
    }

    /**
     * 构造核心对象 JFreeChart
     *
     * @param title
     * @param xTitle
     * @param yTitle
     * @param dataSet
     * @param num
     * @return
     */
    private static File createChartLine(String title, String xTitle, String yTitle, DefaultCategoryDataset dataSet, String num) {
        LOGGER.info("Plugin == [CustomExportAction] --->> createChartLine  start executing...");
        //核心对象：图形的主标题、X轴外标签的名称、轴外标签的名称、图形的显示方式（水平和垂直）、是否显示子标题、是否在图形上显示数值的提示、是否生成URL地址
        JFreeChart chart = ChartFactory.createLineChart(title, xTitle, yTitle, dataSet, PlotOrientation.VERTICAL, true, true, true);
        //解决主标题的乱码
        chart.getTitle().setFont(new Font("宋体", Font.BOLD, 12));
        //解决子标题的乱码
        chart.getLegend().setItemFont(new Font("宋体", Font.BOLD, 12));
        //右侧显示子菜单
        chart.getLegend().setPosition(RectangleEdge.RIGHT);
        //获取图表区域对象
        CategoryPlot categoryPlot = (CategoryPlot) chart.getPlot();
        //获取X轴对象
        CategoryAxis categoryAxis = (CategoryAxis) categoryPlot.getDomainAxis();
        categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(0.95D));
        //获取Y轴对象
        NumberAxis numberAxis = (NumberAxis) categoryPlot.getRangeAxis();
        //解决X轴上的乱码
        categoryAxis.setTickLabelFont(new Font("宋体", Font.BOLD, 15));
        //解决X轴外的乱码
        categoryAxis.setLabelFont(new Font("宋体", Font.BOLD, 15));
        //解决Y轴上的乱码
        numberAxis.setTickLabelFont(new Font("宋体", Font.BOLD, 15));
        //解决Y轴外的乱码
        numberAxis.setLabelFont(new Font("宋体", Font.BOLD, 15));
        //改变Y轴的刻度，默认值从1计算
        numberAxis.setAutoTickUnitSelection(false);
        NumberTickUnit unit = new NumberTickUnit(Integer.parseInt(num));
        numberAxis.setTickUnit(unit);
        //获取绘图区域对象
        LineAndShapeRenderer lineAndShapeRenderer = (LineAndShapeRenderer) categoryPlot.getRenderer();
        //设置转折点
        lineAndShapeRenderer.setBaseShapesVisible(true);
        //让数值显示到页面上
        lineAndShapeRenderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        lineAndShapeRenderer.setBaseItemLabelsVisible(true);
        lineAndShapeRenderer.setBaseItemLabelFont(new Font("宋体", Font.BOLD, 12));
        //用时间作为文件名防止重名的问题发生
        String filename = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss") + ".png";
        //保存文件到web容器中
        File file = new File(filename);
        try {
            ChartUtilities.saveChartAsPNG(file, chart, 600, 500);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Plugin == [CustomExportAction] --->> createChartLine  end executing...");
        return file;
    }

    /**
     * image 导出到 excel
     *
     * @param wb
     * @param file
     */
    private static void imageOut(XSSFWorkbook wb, File file) {
        LOGGER.info("Plugin == [CustomExportAction] --->> imageOut  start executing...");
        XSSFSheet sheet = wb.createSheet();
        wb.setSheetName(2, "image");
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        XSSFRow r = sheet.createRow(0);
        XSSFCell c = r.createCell(0);
        // 设置第一行标题行内容
        XSSFRichTextString t = new XSSFRichTextString("项目分析详情---[ 代码历史问题 ]");
        c.setCellValue(t);
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        BufferedImage bufferImg;
        try {
            bufferImg = ImageIO.read(file);
            ImageIO.write(bufferImg, "png", byteArrayOut);
            Drawing dp = sheet.createDrawingPatriarch();
            //设置图表在excel中位置
            XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, (short) 2, 5, (short) 26, 20);
            anchor.setAnchorType(2);
            dp.createPicture(anchor, wb.addPicture(byteArrayOut.toByteArray(), Workbook.PICTURE_TYPE_PNG)).resize(0.8);
            if (file.exists() && file.isFile()) {
                file.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Plugin == [CustomExportAction] --->> imageOut  end executing...");
    }
}

class TypeSeverityNum {
    private String severity;
    private int severityBugNum;
    private int severityCodeSmellNum;
    private int severityVulnerableNum;

    public TypeSeverityNum() {
    }

    public TypeSeverityNum(String severity, int[] num) {
        this.severity = severity;
        this.severityBugNum = num[0];
        this.severityCodeSmellNum = num[1];
        this.severityVulnerableNum = num[2];
    }


    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getSeverityBugNum() {
        return severityBugNum;
    }

    public void setSeverityBugNum(int severityBugNum) {
        this.severityBugNum = severityBugNum;
    }

    public int getSeverityCodeSmellNum() {
        return severityCodeSmellNum;
    }

    public void setSeverityCodeSmellNum(int severityCodeSmellNum) {
        this.severityCodeSmellNum = severityCodeSmellNum;
    }

    public int getSeverityVulnerableNum() {
        return severityVulnerableNum;
    }

    public void setSeverityVulnerableNum(int severityVulnerableNum) {
        this.severityVulnerableNum = severityVulnerableNum;
    }
}

class Result {
    private String bugs;
    private String vulnerabilities;
    private String codeSmells;
    private String coverage;
    private String duplications;
    private String qualityGate;

    public Result() {
    }

    public Result(HashMap<String, String> map) {
        this.bugs = map.get("bugs");
        this.vulnerabilities = map.get("vulnerabilities");
        this.codeSmells = map.get("code_smells");
        this.coverage = map.get("coverage");
        this.duplications = map.get("duplicated_lines_density");
        if (map.get("alert_status").equals("OK")) {
            this.qualityGate = "PASS";
        } else {
            this.qualityGate = "FAIL";
        }
    }

    public String getBugs() {
        return bugs;
    }

    public void setBugs(String bugs) {
        this.bugs = bugs;
    }

    public String getVulnerabilities() {
        return vulnerabilities;
    }

    public void setVulnerabilities(String vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    public String getCodeSmells() {
        return codeSmells;
    }

    public void setCodeSmells(String codeSmells) {
        this.codeSmells = codeSmells;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public String getDuplications() {
        return duplications;
    }

    public void setDuplications(String duplications) {
        this.duplications = duplications;
    }

    public String getQualityGate() {
        return qualityGate;
    }

    public void setQualityGate(String qualityGate) {
        this.qualityGate = qualityGate;
    }
}
