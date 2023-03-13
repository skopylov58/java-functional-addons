//package com.github.skopylov58.jdbc;
//
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.logging.Formatter;
//import java.util.logging.LogRecord;
//
//class JDBCLogFormatter extends Formatter {
//
//    private static ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>() {
//        public SimpleDateFormat initialValue() {
//            return new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
//        }
//    };
//
//    @Override
//    public String format(LogRecord record) {
//
//        SimpleDateFormat format = formatter.get();
//        String date = format.format(new Date(record.getMillis()));
//        
//        StringBuffer buf = new StringBuffer();
//        buf.append(record.getThreadID())
//        .append("\t")
//        .append(date)
//        .append("\t")
//        .append(record.getMessage())
//        .append("\n");
//        Throwable thrown = record.getThrown();
//        if (thrown != null) {
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            thrown.printStackTrace(pw);
//            pw.close();
//            buf.append(sw.toString());
//        }
//        return buf.toString();
//    }
//
//}