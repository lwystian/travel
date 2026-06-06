package org.example.springboot.security;

import java.util.List;

public final class AdminPermissionCatalog {
    private AdminPermissionCatalog() {
    }

    public record PermissionItem(String code, String name, String groupName, String description) {
    }

    public static final List<PermissionItem> ITEMS = List.of(
            new PermissionItem("dashboard:view", "首页概览", "工作台", "查看后台首页数据概览"),
            new PermissionItem("order:manage", "订单管理", "业务运营", "查看和处理旅游线路订单"),
            new PermissionItem("user:view", "用户管理", "账号权限", "查看用户列表和用户资料"),
            new PermissionItem("user:create", "新增用户", "账号权限", "创建后台或前台用户账号"),
            new PermissionItem("user:update", "编辑用户", "账号权限", "编辑用户基础资料和状态"),
            new PermissionItem("user:delete", "停用用户", "账号权限", "停用或删除用户账号"),
            new PermissionItem("user:reset-password", "重置密码", "账号权限", "为用户重置登录密码"),
            new PermissionItem("tour:manage", "行程管理", "内容运营", "管理旅游线路、套餐和行程详情"),
            new PermissionItem("recommend:manage", "首页推荐", "内容运营", "管理首页推荐内容"),
            new PermissionItem("scenic:manage", "景点管理", "内容运营", "管理景点资料、标签和坐标"),
            new PermissionItem("accommodation:manage", "住宿管理", "内容运营", "管理住宿资料和房源展示"),
            new PermissionItem("guide:manage", "攻略管理", "内容运营", "管理旅游攻略内容"),
            new PermissionItem("comment:manage", "评论管理", "内容风控", "管理景点评论和住宿评价"),
            new PermissionItem("collection:manage", "收藏管理", "内容运营", "查看和管理收藏数据"),
            new PermissionItem("category:manage", "分类管理", "内容运营", "管理景点分类目录"),
            new PermissionItem("carousel:manage", "轮播图管理", "内容运营", "管理前台轮播图内容"),
            new PermissionItem("review:manage", "内容审核", "内容风控", "审核攻略、评论和住宿评价"),
            new PermissionItem("notification:manage", "站内消息", "内容运营", "管理站内通知消息"),
            new PermissionItem("permission:manage", "权限控制", "账号权限", "配置管理员后台权限和内容审核策略"),
            new PermissionItem("payment:manage", "支付配置", "系统配置", "管理支付渠道和支付参数"),
            new PermissionItem("auth-config:manage", "认证配置", "系统配置", "管理登录、验证和安全认证配置"),
            new PermissionItem("log:view", "系统日志", "系统审计", "查看系统操作日志和审计信息"),
            new PermissionItem("site-footer:manage", "网站页脚", "系统配置", "管理网站页脚信息"),
            new PermissionItem("site-assets:manage", "站点素材", "系统配置", "管理站点图片和品牌素材"),
            new PermissionItem("site-settings:manage", "网站设置", "系统配置", "管理网站开关和访问终端策略")
    );

    public static List<String> allCodes() {
        return ITEMS.stream().map(PermissionItem::code).toList();
    }

    public static boolean contains(String code) {
        return allCodes().contains(code);
    }
}
