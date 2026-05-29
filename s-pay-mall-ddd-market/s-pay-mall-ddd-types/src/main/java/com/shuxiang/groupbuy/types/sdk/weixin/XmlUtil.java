package com.shuxiang.groupbuy.types.sdk.weixin;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class XmlUtil {

    /**
     * 解析微信发来的请求(xml)
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> xmlToMap(HttpServletRequest request) throws Exception {
        // 从request中取得输入流
        try (InputStream inputStream = request.getInputStream()) {
            // 将解析结果存储在HashMap中
            Map<String, String> map = new HashMap<>();
            // 读取输入流
            SAXReader reader = new SAXReader();
            // 得到xml文档
            Document document = reader.read(inputStream);
            // 得到xml根元素
            Element root = document.getRootElement();
            // 得到根元素的所有子节点
            List<Element> elementList = root.elements();
            // 遍历所有子节点
            for (Element e : elementList)
                map.put(e.getName(), e.getText());
            // 释放资源
            inputStream.close();
            return map;
        }
    }

    /**
     * 将map转化成xml响应给微信服务器
     */
    static String mapToXML(Map map) {
        StringBuffer sb = new StringBuffer();
        sb.append("<xml>");
        mapToXML2(map, sb);
        sb.append("</xml>");
        try {
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void mapToXML2(Map map, StringBuffer sb) {
        Set set = map.keySet();
        for (Object o : set) {
            String key = (String) o;
            Object value = map.get(key);
            if (null == value)
                value = "";
            if (value.getClass().getName().equals("java.util.ArrayList")) {
                ArrayList list = (ArrayList) map.get(key);
                sb.append("<").append(key).append(">");
                for (Object o1 : list) {
                    HashMap hm = (HashMap) o1;
                    mapToXML2(hm, sb);
                }
                sb.append("</").append(key).append(">");

            } else {
                if (value instanceof HashMap) {
                    sb.append("<").append(key).append(">");
                    mapToXML2((HashMap) value, sb);
                    sb.append("</").append(key).append(">");
                } else {
                    sb.append("<").append(key).append("><![CDATA[").append(value).append("]]></").append(key).append(">");
                }

            }

        }
    }

    /** 微信公众平台推送明文 XML → 实体（JDK17 下不用 XStream，避免 TreeMap 反射被模块系统拦截）。 */
    public static MessageTextEntity weixinIncomingXmlToEntity(String xml) throws Exception {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new InputStreamReader(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        Element root = document.getRootElement();
        MessageTextEntity m = new MessageTextEntity();
        m.setToUserName(childText(root, "ToUserName"));
        m.setFromUserName(childText(root, "FromUserName"));
        m.setCreateTime(childText(root, "CreateTime"));
        m.setMsgType(childText(root, "MsgType"));
        m.setEvent(childText(root, "Event"));
        m.setEventKey(childText(root, "EventKey"));
        m.setTicket(childText(root, "Ticket"));
        m.setContent(childText(root, "Content"));
        m.setMsgId(childText(root, "MsgId"));
        return m;
    }

    private static String childText(Element root, String name) {
        Element el = root.element(name);
        return el != null ? el.getText() : null;
    }

    private static String cdataSafe(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("]]>", "]]]]><![CDATA[>");
    }

    /** 明文被动回复：文本消息 XML（JDK17 下不使用 XStream）。 */
    public static String weixinPassiveTextXml(String toUserOpenId, String fromGhOriginalId, String content) {
        long ts = System.currentTimeMillis() / 1000L;
        String to = cdataSafe(toUserOpenId);
        String from = cdataSafe(fromGhOriginalId);
        String ct = cdataSafe(content);
        return "<xml>\n"
                + "<ToUserName><![CDATA[" + to + "]]></ToUserName>\n"
                + "<FromUserName><![CDATA[" + from + "]]></FromUserName>\n"
                + "<CreateTime>" + ts + "</CreateTime>\n"
                + "<MsgType><![CDATA[text]]></MsgType>\n"
                + "<Content><![CDATA[" + ct + "]]></Content>\n"
                + "</xml>";
    }

    public static XStream getMyXStream() {
        return new XStream(new XppDriver() {
            @Override
            public HierarchicalStreamWriter createWriter(Writer out) {
                return new PrettyPrintWriter(out) {
                    // 对所有xml节点都增加CDATA标记
                    boolean cdata = true;

                    @Override
                    public void startNode(String name, Class clazz) {
                        super.startNode(name, clazz);
                    }

                    @Override
                    protected void writeText(QuickWriter writer, String text) {
                        if (cdata && !StringUtils.isNumeric(text)) {
                            writer.write("<![CDATA[");
                            writer.write(text);
                            writer.write("]]>");
                        } else {
                            writer.write(text);
                        }
                    }
                };
            }
        });
    }

    /**
     * bean转成微信的xml消息格式
     */
    public static String beanToXml(Object object) {
        XStream xStream = getMyXStream();
        xStream.alias("xml", object.getClass());
        xStream.processAnnotations(object.getClass());
        String xml = xStream.toXML(object);
        if (!StringUtils.isEmpty(xml)) {
            return xml;
        } else {
            return null;
        }
    }

    /**
     * xml转成bean泛型方法
     */
    public static <T> T xmlToBean(String resultXml, Class clazz) {
        // XStream对象设置默认安全防护，同时设置允许的类
        XStream stream = new XStream(new DomDriver());
        stream.addPermission(AnyTypePermission.ANY);
        XStream.setupDefaultSecurity(stream);
        stream.allowTypes(new Class[]{clazz});
        stream.processAnnotations(new Class[]{clazz});
        stream.setMode(XStream.NO_REFERENCES);
        stream.alias("xml", clazz);
        return (T) stream.fromXML(resultXml);
    }

}