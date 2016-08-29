package com.ahut.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutTextMessage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 天气服务
 */
public class WeatherService implements WxMpMessageHandler{

    public static final String DEF_CHATSET = "UTF-8";
    public static final int DEF_CONN_TIMEOUT = 30000;
    public static final int DEF_READ_TIMEOUT = 30000;
    public static String userAgent =  "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.66 Safari/537.36";

    //配置您申请的KEY
    public static final String APPKEY ="25860136d54247e75b69421ba696f7bc";

    //1.根据城市查询天气
    public static String getRequest(String city){
        String result =null;
        String url ="http://op.juhe.cn/onebox/weather/query";//请求接口地址
        Map params = new HashMap();//请求参数
        params.put("cityname",city);//要查询的城市，如：温州、上海、北京
        params.put("key",APPKEY);//应用APPKEY(应用详细页查询)
        params.put("dtype","json");//返回数据的格式,xml或json，默认json

        try {
            result =net(url, params, "GET");
            System.out.println(result);
            JSONObject object = JSON.parseObject(result);
            if(object.getInteger("error_code") == 0){

                StringBuilder weatherInfo = new StringBuilder();
                weatherInfo.append(city).append("天气如下\n");
                JSONObject resultData = object.getJSONObject("result");
                JSONObject data = resultData.getJSONObject("data");
                JSONArray weathers = data.getJSONArray("weather");

                for(int i = 0;i<3;i++){
                    JSONObject w = weathers.getJSONObject(i);
                    weatherInfo.append("-----\n").append(w.getString("date") + "\n");
                    JSONArray day = w.getJSONObject("info").getJSONArray("day");
                    JSONArray night = w.getJSONObject("info").getJSONArray("night");
                    weatherInfo.append(night.getString(2) + "~" + day.getString(2) + "度\n");
                    weatherInfo.append("白天 : " + day.getString(1) + " "  + " " + day.getString(3) + " " + day.getString(4)+"\n");
                    weatherInfo.append("夜晚 : " + night.getString(1) + " " + night.getString(3) + " " + night.getString(4) + "\n");
                }

                String chuanyi = data.getJSONObject("life").getJSONObject("info").getJSONArray("chuanyi").getString(1);
                String ganmao = data.getJSONObject("life").getJSONObject("info").getJSONArray("chuanyi").getString(1);
                weatherInfo.append("-----\n").append("温馨提示 : " + chuanyi + ganmao);

                return weatherInfo.toString();
            }else {
                return "error";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }



    public static void main(String[] args) {
        WeatherService weatherService = new WeatherService();
        System.out.println(weatherService.getRequest("杭州"));
    }

    /**
     *
     * @param strUrl 请求地址
     * @param params 请求参数
     * @param method 请求方法
     * @return  网络请求字符串
     * @throws Exception
     */
    public static String net(String strUrl, Map params,String method) throws Exception {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        String rs = null;
        try {
            StringBuffer sb = new StringBuffer();
            if(method==null || method.equals("GET")){
                strUrl = strUrl+"?"+urlencode(params);
            }
            URL url = new URL(strUrl);
            conn = (HttpURLConnection) url.openConnection();
            if(method==null || method.equals("GET")){
                conn.setRequestMethod("GET");
            }else{
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
            }
            conn.setRequestProperty("User-agent", userAgent);
            conn.setUseCaches(false);
            conn.setConnectTimeout(DEF_CONN_TIMEOUT);
            conn.setReadTimeout(DEF_READ_TIMEOUT);
            conn.setInstanceFollowRedirects(false);
            conn.connect();
            if (params!= null && method.equals("POST")) {
                try {
                    DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                    out.writeBytes(urlencode(params));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            InputStream is = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, DEF_CHATSET));
            String strRead = null;
            while ((strRead = reader.readLine()) != null) {
                sb.append(strRead);
            }
            rs = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return rs;
    }

    //将map型转为请求参数型
    public static String urlencode(Map<String,String> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry i : data.entrySet()) {
            try {
                sb.append(i.getKey()).append("=").append(URLEncoder.encode(i.getValue() + "", "UTF-8")).append("&");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
        String content = wxMpXmlMessage.getContent();
        String city = content.substring(0, content.length() - 2);
        String result  = getRequest(city);
        WxMpXmlOutTextMessage m
                = WxMpXmlOutMessage.TEXT().content(result).fromUser(wxMpXmlMessage.getToUserName())
                .toUser(wxMpXmlMessage.getFromUserName()).build();
        return m;
    }
}
