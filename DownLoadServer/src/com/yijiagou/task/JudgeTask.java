package com.yijiagou.task;

import com.yijiagou.tools.StreamHandler;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangwei on 17-8-4.
 */
public class JudgeTask implements Runnable {
    private Socket socket;
    private ExecutorService workerpool;
    private ScheduledExecutorService timepool;
    private Map<String, Socket> map;
    private Map<String, String> sessionMap;

    public JudgeTask(Socket socket, ExecutorService workerpool,
                     ScheduledExecutorService timepool, Map<String, Socket> map, Map<String, String> sessionMap) {
        this.socket = socket;
        this.workerpool = workerpool;
        this.timepool = timepool;
        this.map = map;
        this.sessionMap = sessionMap;
    }

    public void homeDispose(String id, Socket socket) {
        //记录登录的冰箱
        this.map.put(id, socket);
        timepool.scheduleAtFixedRate(new PingPongTask(id, this.map), 1, 1, TimeUnit.MINUTES);
//        timepool.scheduleAtFixedRate(new PingPongTask(id,this.map), 1, 5, TimeUnit.MINUTES);
    }

    public boolean commandToHome(String id,String info){//
        int count = 0;
        while (count < 6) {
            Socket hsocket = this.map.get(id);
            try {
                Writer writer = new OutputStreamWriter(hsocket.getOutputStream());
                Reader reader = new InputStreamReader(hsocket.getInputStream());

                StreamHandler.streamWrite(writer, info);
                String data = StreamHandler.streamRead(reader);
                if(data != null) {
                    String infos[] = data.split("|");
                    if (infos[0].equals("0111") && infos.equals("ok")) {//有可能家居接受的包错误,则重新发送
                        return true;//成功只有一个可能
                    }
                }
                count++;
            } catch (IOException e) {
                count++;
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void run() {//记得分业务出去,给家电发送下载命令
        Writer out = null;
        Reader in = null;
        try {
            out = new OutputStreamWriter(socket.getOutputStream());
            in = new InputStreamReader(socket.getInputStream());
            String conninfo = StreamHandler.streamRead(in);//第一次收到的数据
            if(conninfo != null) {
                String[] infos = conninfo.split("\\|");//将数据拆分用于判断
                if (infos[0].equals("1111")) {//与家电的链接,与家电建立长链接
                    String req = "1111|ok\n";//返回的消息
                    if (infos[1] == null) {
                        req = "1111|err\n";
                    } else {
                        int number = Integer.parseInt(infos[1]);
                        if (infos[2].getBytes().length != number) {
                            req = "1111|err";
                        }
                    }
                    boolean succe = StreamHandler.streamWrite(out, req);
                    //-------------判断家电连接是否成功---------------
                    if (succe) {
                        homeDispose(infos[2], socket);
                    }
                    //-------------判断家电连接是否成功---------------
                } else if (infos[0].equals("0000")) {//与netty的链接,给家电发送下载命令
                    String sessionId = infos[1];
                    String data = sessionMap.get(sessionId);
                    //-------------判断是不是上一个netty会话没有发出去的消息-------------
                    if (data != null) {
                        boolean succe = StreamHandler.streamWrite(out, data);
                        if (succe) {
                            sessionMap.remove(sessionId);
                            return;
                        }
                    }
                    //-------------判断是不是上一个netty会话没有发出去的消息-------------
                    int number = Integer.parseInt(infos[2]);
                    int ilen = 0;

                    ilen += infos[3].length();
                    String[] ids = infos[3].split("#");//家电ID们
                    ilen += infos[4].length() + 1;
                    String type = infos[4];//家电类型
                    ilen += infos[5].length() + 1;
                    String aid = infos[5];//appID

                    if (ilen != number) {//错误消息
                        StreamHandler.streamWrite(out, "0000|err");
                        StreamHandler.closeWriter(out);
                        StreamHandler.closeReader(in);
                        return;
                    }

                    data = "0000";
                    String path = type + "/" + aid + ".py";
                    int pathLen = path.length();

                    String hinfo = "0111|lod|" + pathLen + "|" + path;
                    for (int i = 0; i < ids.length; i++) {
                        if (commandToHome(ids[i], hinfo)) {
                            data += "1";
                            continue;
                        }
                        data += "0";
                    }

                    boolean succe = StreamHandler.streamWrite(out, data);
                    if (!succe)
                        sessionMap.put(sessionId, data);
                    StreamHandler.closeReader(in);
                    StreamHandler.closeWriter(out);

                } else {
                    out.write("1001|err\n");
                    out.flush();
                }
            }else {
                StreamHandler.closeReader(in);
                StreamHandler.closeWriter(out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
