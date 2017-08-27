package com.yijiagou.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yijiagou.pojo.JsonKeyword;
import com.yijiagou.tools.JedisUtils.SJedisPool;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by zgl on 17-7-29.
 */

public class DownLoadHandler extends ChannelHandlerAdapter {
    private SJedisPool sJedisPool;
    private static Logger logger = Logger.getLogger(DownLoadHandler.class.getName());
    private static final BlockingQueue<String> randomnums = new ArrayBlockingQueue<String>(100000);

    public DownLoadHandler(SJedisPool sJedisPool) {
        this.sJedisPool = sJedisPool;
    }

    static {
        int[] randomnum = new int[100000];
        Random random = new Random();
        for (int i = 0; i < randomnum.length; i++) {
            int number = random.nextInt(100000) + 1;
            for (int j = 0; j <= i; j++) {
                if (number != randomnum[j]) {
                    randomnum[i] = number;
                }
            }
        }
        for (int i = 0; i < randomnum.length; i++) {
            String num = "" + randomnum[i];
            int length = num.length();
            if (length <= 6) {
                num += "0" + num;
            }
            randomnums.offer(num);
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        JSONObject jsonObject = (JSONObject) msg;
        String type = (String) jsonObject.get(JsonKeyword.TYPE);
        if (type.equals(JsonKeyword.DOWNLOAD)) {


//            System.out.println("xxxxxxxxx");
            String devicetype = (String) jsonObject.get(JsonKeyword.DEVICETYPE);
            JSONArray jsonArray = jsonObject.getJSONArray(JsonKeyword.DEVICE);

            //----------测试-------------
            System.out.println(jsonArray);
            //----------测试-------------

            String appid = (String) jsonObject.get(JsonKeyword.APPID);
            String urlhead = "0000|";
            String deviceids = "";
            JSONObject jsonObject1;
            for (int i = 0; i < jsonArray.size(); i++) {
                jsonObject1 = (JSONObject) jsonArray.get(i);
                deviceids += jsonObject1.get(JsonKeyword.DEVICEID) + "#";
            }
            String deviceidd = deviceids.substring(0, deviceids.length() - 1);
            String url = deviceidd + "|" + devicetype + "|" + appid;

            int urlsize = url.length();
            BufferedWriter bw = null;
            BufferedReader br = null;
            String ip = "127.0.0.1";
            int port = 9999;
            int count = 0;
            //String ip, int port, int initSocket, int maxSocket, int minSocket, int requestSocket
//            SocketPool socketPool = new SocketPool("192.168.56.101", 2333, 200, 500, 100, 20);

            String sessionid = null;
            try {
                sessionid = randomnums.take();
            } catch (InterruptedException e) {
                logger.error(e);
            }
            while (true) {
                String request = urlhead + sessionid + "|" + urlsize + "|" + url + "\n";
                Socket socket = null;
                try {
                    socket = new Socket(ip, port);
                    bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    System.out.println(request);
                    bw.write(request);
                    bw.flush();
                    String response = br.readLine();
                    System.out.println(response);
                    if (response.equals("0000|err")) {
                        continue;
                    } else {
                        updateScore(sJedisPool, devicetype, appid);
                        System.out.println(response);
                        String[] s = response.split("|");
                        String[] ss = s[1].split("");
                        String json = "";
                        for (int i = 1; i < ss.length; i++) {
                            json += "[{\"state\":\"" + ss[i] + "\"},";
                        }
                        String jsonArray1 = json.substring(0, json.length() - 1) + "]\n";
                        System.out.println(jsonArray1);
                        ctx.writeAndFlush(jsonArray1);
                    }
                    if (response == null) {
                        continue;
                    }
                    try {
                        randomnums.put(sessionid);
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }

                    break;
                } catch (IOException e) {
                    logger.error(e + "==>channelRead");
                    if (count++ > 2) {
                        break;
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e1) {
                        logger.error(e1 + "==>channelRead");
                    }
                    continue;
                } catch (IndexOutOfBoundsException e) {
                    System.out.println(e);
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }

    }

    private void updateScore(SJedisPool sJedisPool, String deviceType, String appid) {
        Jedis jedis = null;
        int count = 0;
        while (true) {
            try {
                jedis = sJedisPool.getConnection();
                jedis.zincrby(deviceType, 1, appid);
                sJedisPool.putbackConnection(jedis);
                break;
            } catch (JedisConnectionException e) {
                if (count++ > 2) {
                    break;
                } else {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e1) {
                        logger.error(e1 + "thread sleep error in updateScore");
                    }
                    continue;
                }
            }
        }

    }
}
