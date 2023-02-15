package net.lainiao.dicom.utils;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

/**
 * @author wangdong
 * @date 2022/10/25
 * @Description email工具类
 */

public class EmailUtils {

    public EmailUtils(String hostname, String port, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    private String port;
    private String hostname;
    private String username;
    private String password;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void sendMailCode(String receiveEmail, String code) {

        // 配置发送邮件的环境属性
        final Properties props = new Properties();
        // 表示SMTP发送邮件，需要进行身份验证
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", hostname);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.port", port);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.from", username);    //mailfrom 参数  "发信地址"
        props.put("mail.user", username);// 发件人的账号(在控制台创建的发信地址)
        props.put("mail.password", password);// 发信地址的smtp密码(在控制台选择发信地址进行设置) "SMTP密码"
        props.put("mail.smtp.starttls.enable", "true");

        // 构建授权信息，用于进行SMTP进行身份验证
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // 用户名、密码
                String userName = props.getProperty("mail.user");
                String password = props.getProperty("mail.password");
                return new PasswordAuthentication(userName, password);
            }
        };

        //使用环境属性和授权信息，创建邮件会话
        Session mailSession = Session.getInstance(props, authenticator);
        //mailSession.setDebug(true);//开启debug模式

        //UUID uuid = UUID.randomUUID();
        //final String messageIDValue = "<" + uuid.toString() + ">";
        //创建邮件消息
        MimeMessage message = new MimeMessage(mailSession) {
            //@Override
            //protected void updateMessageID() throws MessagingException {
            //设置自定义Message-ID值
            //setHeader("Message-ID", messageIDValue);//创建Message-ID
            //}
        };

        try {
            // 设置发件人邮件地址和名称。填写控制台配置的发信地址。和上面的mail.user保持一致。名称用户可以自定义填写。
            InternetAddress from = new InternetAddress(username, "MOUSE AI");//from 参数,可实现代发，注意：代发容易被收信方拒信或进入垃圾箱。
            message.setFrom(from);

            //可选。设置回信地址
            Address[] a = new Address[1];
            a[0] = new InternetAddress(receiveEmail);
            message.setReplyTo(a);

            // 设置收件人邮件地址
            InternetAddress to = new InternetAddress(receiveEmail);
            message.setRecipient(MimeMessage.RecipientType.TO, to);
            //如果同时发给多人，才将上面两行替换为如下（因为部分收信系统的一些限制，尽量每次投递给一个人；同时我们限制单次允许发送的人数是60人）：
//            InternetAddress[] adds = new InternetAddress[2];
//            adds[0] = new InternetAddress(receiveEmail);
//            adds[1] = new InternetAddress("收信地址");
//            message.setRecipients(Message.RecipientType.TO, adds);

            message.setSentDate(new Date()); //设置时间

            message.setSubject("Verify the information!");
            message.setContent("【MOUSE AI】Welcome to use the email verification system! The verification code is【" + code + "】", "text/html;charset=UTF-8");//html超文本；// "text/plain;charset=UTF-8" //纯文本。

            // 发送邮件
            Transport.send(message);

        }
        catch (MessagingException e) {
            String err = e.getMessage();
            // 在这里处理message内容， 格式是固定的
            System.out.println(err);
        }
        catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
//        EmailUtils emailUtils = new EmailUtils("smtp.qiye.aliyun.com", "465", "cloud.service@dexhin.com", "rYdegP1Pl1AYIybR");
        EmailUtils emailUtils = new EmailUtils("smtp.office365.com", "587", "dicom-ai@outlook.com", "Fact2018");
        emailUtils.sendMailCode("yang.fei@dexhin.com", "123123");
    }
}
