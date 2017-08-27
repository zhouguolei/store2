package com.yijiagou.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yijiagou.pojo.JsonKeyword;
import com.yijiagou.pojo.UrlAppinfo;
import com.yijiagou.tools.JedisUtils.SJedisPool;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerAppender;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.*;

/**
 * Created by zgl on 17-8-11.
 */
public class ShowAppStoreHandler extends ChannelHandlerAdapter {
    private SJedisPool sJedisPool;
    private static Logger logger =Logger.getLogger(ShowAppStoreHandler.class.getName());
    public ShowAppStoreHandler(SJedisPool sJedisPool){
        this.sJedisPool=sJedisPool;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        JSONObject jsonObject = (JSONObject) msg;
        if (jsonObject.get(JsonKeyword.TYPE).equals(JsonKeyword.APPSTORE)) {
            String devicetype = (String) jsonObject.get(JsonKeyword.DEVICETYPE);
            String page = (String) jsonObject.get(JsonKeyword.PAGE);
            JSONArray jsonArray = jedisGeturlinfo(devicetype, Integer.parseInt(page),sJedisPool);
            ctx.writeAndFlush(jsonArray.toString()+"\n");
        } else {
            ctx.fireChannelRead(msg);
            System.out.println("showapp");
        }
    }

    private JSONArray jedisGeturlinfo(String string, int a,SJedisPool sJedisPool) {
        Set set1 = null;
        Jedis jedis=null;
        int count=0;
        jedis = sJedisPool.getConnection();
        while (true) {
            try {
                int start = (a - 1) * 10;
                int end = (a - 1) * 10 + 9;
                set1 = jedis.zrevrange(string, start, end);
                sJedisPool.putbackConnection(jedis);
                break ;
            } catch (Exception e) {
                logger.error(e+"===>jedisGeturlinfo");
                if(count++>=2){
                    return null;
                }else {
                    sJedisPool.repairConnection(jedis);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e1) {
                        logger.error(e1+"thread is error in jedisGeturlinfo");
                    }
                    continue ;
                }
            }
        }
        Iterator iterator = set1.iterator();
        String json = "";
        JSONArray jsonArray = new JSONArray();
        try {
            while (iterator.hasNext()) {
                UrlAppinfo urlAppinfo = new UrlAppinfo((String) iterator.next());
                json = JSON.toJSONString(urlAppinfo);
                jsonArray.add(json);
            }
        } catch (Exception e) {
            logger.error(e+"===>jedisGeturlinfo");
        }
        System.out.println(jsonArray);
        return jsonArray;
    }
}
