package org.example.springboot.dto;

import lombok.Data;

@Data
public class SiteAccessConfigDTO {
    private Boolean siteEnabled = true;
    private Boolean rejectMobile = false;
    private String closedTitle = "网站维护中";
    private String closedMessage = "我们正在进行系统维护与服务升级，完成后将第一时间恢复访问。";
    private String closedContact = "如有紧急订单或出行问题，请联系官方客服处理。";
    private String mobileTitle = "请使用电脑访问";
    private String mobileMessage = "当前官网桌面版正在服务中，移动端 H5 模板正在制作，为保证浏览和下单体验，请使用电脑访问。";
    private String mobileContact = "如需咨询行程，可通过官方客服渠道联系我们。";
    private String supportButtonText = "联系官方客服";
    private String supportUrl = "";
    private String supportCredential = "";
    private String supportQrImageUrl = "";
}
