package com.ahut.servlet;

import com.ahut.service.RobotService;
import com.ahut.service.WeatherService;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.common.util.StringUtils;
import me.chanjar.weixin.mp.api.*;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutTextMessage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Created by 胡成超 on 2016/8/6.
 */
public class WxServlet extends HttpServlet {



    protected WxMpInMemoryConfigStorage config;

    protected WxMpService wxMpService;

    protected WxMpMessageRouter wxMpMessageRouter;


    WxMpMessageHandler weatherHandler = new WeatherService();//天气处理
    RobotService robotService = new RobotService();//机器人聊天

    String subscirbeContent = "欢迎关注最懂我(*＾-＾*)\n" +
            "输入以下回复功能：\n" +
            "城市名+天气：查询对应城市天气\n" +
            "笑话：来个笑话\n" +
            "新闻：新闻头条\n" +
            "微信精选：微信精选内容\n" +
            "星座：查询星座运势\n" +
            "其他：与我对话聊天\n";

    @Override public void init() throws ServletException {
        super.init();

        config = new WxMpInMemoryConfigStorage();
        config.setAppId("wxad58b4c185c2ad90"); // 设置微信公众号的appid
        config.setSecret("ab0404797755d4242cc762be9281f995"); // 设置微信公众号的app corpSecret
        config.setToken("huchengchao"); // 设置微信公众号的token
        config.setAesKey("qyn43W1ANKVQWnQOqP31v42XbFub0u7HlWRFByDBhKp"); // 设置微信公众号的EncodingAESKey

        wxMpService = new WxMpServiceImpl();
        wxMpService.setWxMpConfigStorage(config);

        WxMpMessageHandler subscribeHandler = new WxMpMessageHandler() {

            public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
                WxMpXmlOutTextMessage m
                        = WxMpXmlOutMessage.TEXT().content(subscirbeContent).fromUser(wxMpXmlMessage.getToUserName())
                        .toUser(wxMpXmlMessage.getFromUserName()).build();
                return m;
            }
        };

        wxMpMessageRouter = new WxMpMessageRouter(wxMpService);
        wxMpMessageRouter
                .rule()
                .msgType(WxConsts.XML_MSG_EVENT)
                .event(WxConsts.EVT_SUBSCRIBE)
                .async(false)
                .handler(subscribeHandler)
                .end()

                .rule()
                .msgType(WxConsts.XML_MSG_TEXT)
                .rContent(".+天气")
                .async(false)
                .handler(weatherHandler)
                .end()

                .rule()
                .msgType(WxConsts.XML_MSG_TEXT)
                .async(false)
                .handler(robotService)
                .end();

    }


    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{

        response.setCharacterEncoding("UTF-8");//设置编码为UTF-8

        WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(request.getInputStream());

        WxMpXmlOutMessage reMsg = wxMpMessageRouter.route(inMessage);

        if (reMsg != null) {
            // 说明是同步回复的消息
            // 将xml写入HttpServletResponse
            response.getWriter().write(reMsg.toXml());
        } else {
            // 说明是异步回复的消息，直接将空字符串写入HttpServletResponse
            response.getWriter().write("success");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        String signature = request.getParameter("signature");//获取签名
        String nonce = request.getParameter("nonce");//获取随机数
        String timestamp = request.getParameter("timestamp");//获取时间戳
        String echostr = request.getParameter("echostr");//获取随机字符串

        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            // 消息签名不正确，说明不是公众平台发过来的消息
            response.getWriter().println("非法请求");
            return;
        }
        if (StringUtils.isNotBlank(echostr)) {
            // 说明是一个仅仅用来验证的请求，回显echostr
            response.getWriter().println(echostr);
            return;
        }
    }


}
