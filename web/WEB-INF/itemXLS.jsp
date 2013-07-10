<%-- 
    Document   : itemcsvjsp
    Created on : 25 juin 2013, 17:05:15
    Author     : clem
--%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="rssagregator.beans.Flux"%>
<%@page import="rssagregator.beans.Item"%>
<%@page import="java.util.List"%>
<%@page import="java.io.CharArrayReader"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page import="java.io.StringReader"%>
<%@page import="com.sun.xml.internal.messaging.saaj.util.ByteOutputStream"%>
<%@page import="java.io.OutputStream"%>
<%@page import="java.io.FileNotFoundException"%>
<%@page import="java.io.FileOutputStream"%>
<%@page import="java.io.IOException"%>
<%@page import="java.util.logging.Level"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="org.apache.poi.hssf.usermodel.HSSFDataFormat"%>
<%@page import="org.apache.poi.hssf.usermodel.HSSFWorkbook"%>
<%@page import="org.apache.poi.ss.usermodel.Cell"%>
<%@page import="org.apache.poi.ss.usermodel.CellStyle"%>
<%@page import="org.apache.poi.ss.usermodel.DataFormat"%>
<%@page import="org.apache.poi.ss.usermodel.Font"%>
<%@page import="org.apache.poi.ss.usermodel.Row"%>
<%@page import="org.apache.poi.ss.usermodel.Sheet"%>
<%@page import="org.apache.poi.ss.usermodel.Workbook"%>
<%@page contentType="document/xls" pageEncoding="UTF-8"%>
<%


    List<Item> listItem = (List<Item>) request.getAttribute("listItem");



    // create a new workbook
    Workbook wb = new HSSFWorkbook();
    // create a new sheet
    Sheet s = wb.createSheet();

    // declare a row object reference
    Row r = null;
    // declare a cell object reference
    Cell c = null;


    // create 3 cell styles
    CellStyle cs = wb.createCellStyle();
    CellStyle cs2 = wb.createCellStyle();
    CellStyle cs3 = wb.createCellStyle();
    DataFormat df = wb.createDataFormat();
    // create 2 fonts objects
    Font f = wb.createFont();
    Font f2 = wb.createFont();
    //set font 1 to 12 point type
    f.setFontHeightInPoints((short) 12);
    //make it blue
    f.setColor((short) 0xc);
    // make it bold
    //arial is the default font
    f.setBoldweight(Font.BOLDWEIGHT_BOLD);
    //set font 2 to 10 point type
    f2.setFontHeightInPoints((short) 10);
    //make it red
    f2.setColor((short) Font.COLOR_RED);
    //make it bold
    f2.setBoldweight(Font.BOLDWEIGHT_BOLD);
    f2.setStrikeout(true);
    //set cell stlye
    cs.setFont(f);
    //set the cell format
    cs.setDataFormat(df.getFormat("#,##0.0"));
    //set a thin border
    cs2.setBorderBottom(cs2.BORDER_THIN);
    //fill w fg fill color
    cs2.setFillPattern((short) CellStyle.SOLID_FOREGROUND);
    //set the cell format to text see DataFormat for a full list
    cs2.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"));
    // set the font
    cs2.setFont(f2);
    // set the sheet name in Unicode
    wb.setSheetName(0, "\u0422\u0435\u0441\u0442\u043E\u0432\u0430\u044F "
            + "\u0421\u0442\u0440\u0430\u043D\u0438\u0447\u043A\u0430");

    // in case of plain ascii
    // wb.setSheetName(0, "HSSF Test");
    // create a sheet with 30 rows (0-29)
    int rownum;


    Map<Long, Sheet> assocsfluxSet = new HashMap<Long, Sheet>();

    for (rownum = 0; rownum < listItem.size(); rownum++) {


        Integer j;
        for (j = 0; j < listItem.get(rownum).getListFlux().size(); j++) {

            Flux fl = listItem.get(rownum).getListFlux().get(j);

            Sheet sheet = assocsfluxSet.get(fl.getID());

            if (sheet == null) {
                String nomsheet = fl.toString();
                nomsheet = nomsheet.replace(":", "-");
                nomsheet = nomsheet.replace("/", "");
                nomsheet = nomsheet.substring(0, 10);
                sheet = wb.createSheet(nomsheet + rownum + j.toString());
                assocsfluxSet.put(fl.getID(), sheet);


                r = sheet.createRow(0);
                c = r.createCell(0);
                c.setCellValue("ID");


                c = r.createCell(1);
                c.setCellValue("Titre");
            }


            // On ajoute la donnée à la sheet

            Integer newindex = sheet.getLastRowNum();

            if (newindex == null) {
                newindex = 0;
            } else {
                newindex++;
            }


            r = sheet.createRow(newindex);

            c = r.createCell(0);
            c.setCellValue(listItem.get(rownum).getID());
//
//            
//                    // Gestion des titre;
            c = r.createCell(1);
            c.setCellValue(listItem.get(rownum).getTitre());


            // Gestion de la description
            c = r.createCell(2);
            c.setCellValue(listItem.get(rownum).getDescription());

        }
    }
    //draw a thick black border on the row at the bottom using BLANKS
    // advance 2 rows
//    rownum++;
    rownum++;
//    r = s.createRow(rownum);
    // define the third style to be the default
    // except with a thick black border at the bottom
    cs3.setBorderBottom(cs3.BORDER_THICK);
    //create 50 cells
//    for (short cellnum = (short) 0; cellnum < 50; cellnum++) {
//        //create a blank type cell (no value)
//        c = r.createCell(cellnum);
//        // set it to the thick black border style
//        c.setCellStyle(cs3);
//    }

    //end draw thick black border
    // demonstrate adding/naming and deleting a sheet
    // create a sheet, set its title then delete it
    s = wb.createSheet();
    wb.setSheetName(1, "DeletedSheet");
    wb.removeSheetAt(1);
    //end deleted sheet
    // write the workbook to the output stream
    // close our file (don't blow out our file handles



    OutputStream outt = response.getOutputStream();
    wb.write(outt);
    outt.close();


%>