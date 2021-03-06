package handler;

import access.Main;
import event.Event;
import event.ExcelFrameEvent;
import event.ExecuteSQLEvent;
import event.MakeExcelEvent;
import exception.EventQueueException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import scheduler.SchedulerPriorityBlockingQueue;
import tool.ExcelHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Desciption:
 * Author: jasonhan.
 * Creation time: 2018/4/23 3:09 PM.
 * © Copyright 2013-2018.
 */
public class ExcelFrameHandler extends Handler {

    private ExcelFrameHandler() {}

    /**
     * 配合getInstance方法，实现安全的多线程环境下的单实例创建。
     * 类级的内部类，只有被调用的时候才会被加载，从而实现了延迟加载。
     */
    private static class ExcelFrameHandlerHolder {
        private static ExcelFrameHandler EXCEL_FRAME_HOLDER_INSTANCE = new ExcelFrameHandler();
    }

    public static ExcelFrameHandler getInstance() {
        return ExcelFrameHandler.ExcelFrameHandlerHolder.EXCEL_FRAME_HOLDER_INSTANCE;
    }

    @Override
    public int handle(Event event) {
        ExcelFrameEvent excelFrameEvent = (ExcelFrameEvent) event;
        String nmExcel = excelFrameEvent.getNmExcel();
        if (null == nmExcel) {
            putLogError(event, " is null");
        }

        Workbook workbook = null;
        try {
            workbook = ExcelHelper.loadExcel(nmExcel);
        } catch (EventQueueException e) {
            putLogError(event, nmExcel + " " + e.getExcMsg());
        } catch (IOException e) {
            putLogError(event, "Excel is inexistence！！！！");
        }

        if (null != workbook) {
            int sheetNum = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetNum; i++) {
                Sheet sheet = workbook.getSheetAt(i);

                String prodExcelNm = sheet.getSheetName();
                int excelSheetUndefindVernier = 0;
                MakeExcelEvent makeExcelEvent =
                        new MakeExcelEvent(
                                "",
                                new Date(),
                                MakeExcelHandler.getInstance(),
                                Main.eventQueue
                        ).setFileName(prodExcelNm)
                         .setFilePath(System.getProperty("user.dir"))
                         .setTotalNumSheet(sheet.getLastRowNum() + 1);
                for (Row rowStep : sheet) {
                    String sheetNm = "";
                    String sql = "";
                    if (null == rowStep) {
                        makeExcelEvent.addSheet("undefind-"+(excelSheetUndefindVernier++), null);
                        continue;
                    }
                    try {
                        sheetNm = rowStep.getCell(excelFrameEvent.getCellNumSheetNm()).getStringCellValue();
                    } catch (Exception e) {
                        putLogInfo(event, "this cell is null.");
                    }
                    try {
                        sql = rowStep.getCell(excelFrameEvent.getCellNumSQL()).getStringCellValue();
                    } catch (Exception e) {
                        putLogInfo(event, "this cell is null.");
                    }

                    if (StringUtils.isNotEmpty(sheetNm) && StringUtils.isNotEmpty(sql)) {
                        ExecuteSQLEvent executeSQLEvent = new ExecuteSQLEvent("", new Date(), ExecuteSQLHandler.getInstance(), Main.eventQueue, makeExcelEvent)
                                .setSql(sql).setSheetNm(sheetNm);
                        Main.eventQueue.insertEvent(executeSQLEvent);
                    } else {
                        makeExcelEvent.addSheet("undefind-"+(excelSheetUndefindVernier++), null);
                        putLogError(event, "make executeSQLEvent is fail."+"prodExcelNm:"+prodExcelNm+","+"sheetNm:"+sheetNm+","+"sql:"+sql);
                    }
                }
            }
        } else {
            putLogError(event, "workbook is null.");
        }

        return 0;
    }

}
