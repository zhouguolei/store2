package com.yijiagou.tools;

import java.io.*;

/**
 * Created by wangwei on 17-8-20.
 */
public class StreamHandler {
    public static boolean streamWrite(Writer writer,String data){
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(writer);
            bw.write(data);
            bw.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String streamRead(Reader reader){
        BufferedReader br = new BufferedReader(reader);
        try {
            String data = br.readLine();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean streamWrite(OutputStream out,byte[] bytes){
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(out);
            bos.write(bytes);
            bos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void streamRead(InputStream in,byte[] bytes,int start,int end){
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(in);
            bis.read(bytes,start,end);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void colseOutputStream(OutputStream out){
        BufferedOutputStream bos = null;
        if(out != null) {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeInputStream(InputStream in){
        BufferedInputStream bis = null;
        if(in != null) {
            bis = new BufferedInputStream(in);
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeWriter(Writer writer){
        BufferedWriter bw = null;
        if(writer != null) {
            bw = new BufferedWriter(writer);
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeReader(Reader reader){
        BufferedReader br = null;
        if(reader != null){
            br = new BufferedReader(reader);
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
