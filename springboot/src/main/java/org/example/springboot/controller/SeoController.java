package org.example.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.entity.Accommodation;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.Tour;
import org.example.springboot.entity.TravelGuide;
import org.example.springboot.mapper.AccommodationMapper;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.example.springboot.mapper.TourMapper;
import org.example.springboot.mapper.TravelGuideMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping
public class SeoController {
    private static final String SITE_NAME = "侠客行国旅";
    private static final int SITEMAP_LIMIT = 5000;
    private static final int FEED_LIMIT = 40;
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter XML_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${app.seo.public-origin:}")
    private String configuredPublicOrigin;

    @Resource
    private ScenicSpotMapper scenicSpotMapper;
    @Resource
    private TourMapper tourMapper;
    @Resource
    private TravelGuideMapper travelGuideMapper;
    @Resource
    private AccommodationMapper accommodationMapper;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots(HttpServletRequest request) {
        String origin = publicOrigin(request);
        return """
                User-agent: *
                Allow: /
                Allow: /img/
                Allow: /videos/
                Allow: /api/img/
                Allow: /api/videos/
                Disallow: /back
                Disallow: /login
                Disallow: /register
                Disallow: /profile
                Disallow: /orders
                Disallow: /collection
                Disallow: /my-guide
                Disallow: /guide/edit
                Disallow: /ticket/booking
                Disallow: /tour-order-confirm
                Disallow: /tour-order-pay
                Disallow: /payment
                Disallow: /payment-failed
                Disallow: /api/

                Sitemap: %s/sitemap.xml
                Sitemap: %s/sitemap.txt
                """.formatted(origin, origin);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap(HttpServletRequest request) {
        String origin = publicOrigin(request);
        List<SitemapUrl> urls = sitemapUrls(origin);

        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        for (SitemapUrl url : urls) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(escapeXml(url.loc())).append("</loc>\n");
            if (url.lastmod() != null) {
                xml.append("    <lastmod>").append(url.lastmod().format(XML_DATE)).append("</lastmod>\n");
            }
            xml.append("    <changefreq>").append(url.changefreq()).append("</changefreq>\n");
            xml.append("    <priority>").append(url.priority()).append("</priority>\n");
            xml.append("  </url>\n");
        }
        xml.append("</urlset>");
        return xml.toString();
    }

    @GetMapping(value = "/sitemap.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String sitemapText(HttpServletRequest request) {
        String origin = publicOrigin(request);
        StringBuilder text = new StringBuilder();
        for (SitemapUrl url : sitemapUrls(origin)) {
            text.append(url.loc()).append('\n');
        }
        return text.toString();
    }

    @GetMapping(value = {"/feed.xml", "/rss.xml"}, produces = "application/rss+xml;charset=UTF-8")
    public String feed(HttpServletRequest request) {
        String origin = publicOrigin(request);
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <title>%s最新旅游内容</title>
                    <link>%s/</link>
                    <description>%s提供最新旅游线路、景点、住宿和攻略内容。</description>
                    <language>zh-CN</language>
                    <lastBuildDate>%s</lastBuildDate>
                    <ttl>60</ttl>
                """.formatted(SITE_NAME, origin, SITE_NAME, rssDate(LocalDateTime.now())));
        for (FeedItem item : publicFeedItems(origin)) {
            xml.append("    <item>\n");
            xml.append("      <title>").append(escapeXml(limit(stripHtml(item.title()), 80))).append("</title>\n");
            xml.append("      <link>").append(escapeXml(item.link())).append("</link>\n");
            xml.append("      <guid isPermaLink=\"true\">").append(escapeXml(item.link())).append("</guid>\n");
            xml.append("      <description>").append(escapeXml(limit(stripHtml(item.summary()), 240))).append("</description>\n");
            xml.append("      <pubDate>").append(rssDate(item.updateTime())).append("</pubDate>\n");
            xml.append("    </item>\n");
        }
        xml.append("  </channel>\n</rss>");
        return xml.toString();
    }

    @GetMapping(value = "/sitemap.html", produces = MediaType.TEXT_HTML_VALUE)
    public String sitemapHtml(HttpServletRequest request) {
        return listingHtml(request, "侠客行国旅站点地图", "侠客行国旅公开页面索引，包含景点、旅游线路、住宿和旅游攻略。", "/", "WebSite");
    }

    @GetMapping(value = "/seo/home", produces = MediaType.TEXT_HTML_VALUE)
    public String homeSnapshot(HttpServletRequest request) {
        return listingHtml(request, "侠客行国旅", "侠客行国旅提供精品旅游线路、热门旅游景点、景区住宿推荐和目的地旅游攻略。", "/", "TravelAgency");
    }

    @GetMapping(value = "/seo/scenic", produces = MediaType.TEXT_HTML_VALUE)
    public String scenicListSnapshot(HttpServletRequest request) {
        return listingHtml(request, "热门旅游景点", "侠客行国旅热门旅游景点索引，包含景点介绍、地理位置、开放时间和周边线路。", "/scenic", "CollectionPage");
    }

    @GetMapping(value = "/seo/tickets", produces = MediaType.TEXT_HTML_VALUE)
    public String ticketListSnapshot(HttpServletRequest request) {
        return listingHtml(request, "旅游线路预订", "侠客行国旅精品旅游线路索引，包含目的地、行程天数、线路特色和在线预订入口。", "/tickets", "CollectionPage");
    }

    @GetMapping(value = "/seo/accommodation", produces = MediaType.TEXT_HTML_VALUE)
    public String accommodationListSnapshot(HttpServletRequest request) {
        return listingHtml(request, "景区住宿推荐", "侠客行国旅景区住宿索引，包含酒店、民宿、特色住宿、地址和价格区间。", "/accommodation", "CollectionPage");
    }

    @GetMapping(value = "/seo/guide", produces = MediaType.TEXT_HTML_VALUE)
    public String guideListSnapshot(HttpServletRequest request) {
        return listingHtml(request, "旅游攻略", "侠客行国旅旅游攻略索引，包含目的地玩法、路线安排和出行建议。", "/guide", "CollectionPage");
    }

    @GetMapping(value = "/seo/about", produces = MediaType.TEXT_HTML_VALUE)
    public String aboutSnapshot(HttpServletRequest request) {
        return html(request,
                "关于侠客行",
                "了解侠客行国旅的旅行服务、企业资质、目的地资源与客户服务承诺。",
                "/about",
                "",
                "AboutPage",
                SITE_NAME,
                "",
                "侠客行国旅提供旅游线路、热门景点、景区住宿和目的地攻略服务，帮助游客完成行程咨询、预订和出行安排。");
    }

    @GetMapping(value = "/seo/scenic/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String scenicSnapshot(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        ScenicSpot spot = scenicSpotMapper.selectById(id);
        if (spot == null) {
            return notFound(request, response);
        }
        String title = spot.getName() + "旅游攻略、门票与路线";
        String description = firstText(spot.getDescription(), spot.getLocation(), "侠客行国旅为您提供景点介绍、周边住宿和精品行程推荐。");
        String body = joinText(
                spot.getDescription(),
                labelText("景点位置：", spot.getLocation()),
                labelText("开放时间：", spot.getOpeningHours()),
                labelText("景点标签：", spot.getTags())
        );
        return html(request, title, description, "/scenic/" + id, spot.getImageUrl(), "TouristAttraction",
                spot.getName(), spot.getLocation(), body);
    }

    @GetMapping(value = "/seo/tour/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String tourSnapshot(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        Tour tour = tourMapper.selectById(id);
        if (tour == null || !Integer.valueOf(1).equals(tour.getStatus())) {
            return notFound(request, response);
        }
        String title = tour.getTitle() + "预订";
        String description = firstText(tour.getSubtitle(), tour.getFeature(), tour.getDetailContent(), "侠客行国旅精品旅游线路，支持在线咨询与预订。");
        String body = joinText(
                tour.getSubtitle(),
                labelText("目的地：", tour.getDestination()),
                tour.getDays() == null ? "" : "行程天数：" + tour.getDays() + "天",
                tour.getMinPrice() == null ? "" : "参考价格：¥" + tour.getMinPrice() + "起",
                tour.getFeature(),
                tour.getDetailContent(),
                tour.getNotice()
        );
        return html(request, title, description, "/tour/" + id, tour.getMainImage(), "Trip",
                tour.getTitle(), tour.getDestination(), body);
    }

    @GetMapping(value = "/seo/accommodation/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String accommodationSnapshot(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        Accommodation item = accommodationMapper.selectById(id);
        if (item == null) {
            return notFound(request, response);
        }
        String title = item.getName() + "住宿推荐";
        String description = firstText(item.getDescription(), item.getFeatures(), item.getAddress(), "侠客行国旅为您推荐景区周边优质住宿。");
        String body = joinText(
                item.getDescription(),
                item.getFeatures(),
                labelText("住宿地址：", item.getAddress()),
                labelText("住宿类型：", item.getType()),
                labelText("价格区间：", item.getPriceRange()),
                labelText("距景点距离：", item.getDistance())
        );
        return html(request, title, description, "/accommodation/" + id, item.getImageUrl(), "LodgingBusiness",
                item.getName(), item.getAddress(), body);
    }

    @GetMapping(value = "/seo/guide/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String guideSnapshot(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        TravelGuide guide = travelGuideMapper.selectById(id);
        if (guide == null || !Integer.valueOf(1).equals(guide.getReviewStatus())) {
            return notFound(request, response);
        }
        String title = guide.getTitle() + "旅游攻略";
        String description = firstText(guide.getContent(), guide.getDestination(), "侠客行国旅旅游攻略，分享目的地玩法、路线和出行建议。");
        String body = joinText(
                labelText("目的地：", guide.getDestination()),
                guide.getContent()
        );
        return html(request, title, description, "/guide/detail/" + id, guide.getCoverImage(), "Article",
                guide.getTitle(), guide.getDestination(), body);
    }

    private String html(HttpServletRequest request, String title, String description, String path, String image,
                        String schemaType, String name, String location, String body) {
        return html(request, title, description, path, image, schemaType, name, location, body, true);
    }

    private String html(HttpServletRequest request, String title, String description, String path, String image,
                        String schemaType, String name, String location, String body, boolean indexable) {
        String origin = publicOrigin(request);
        String canonical = origin + path;
        String pageTitle = limit(title, 70);
        String pageDescription = limit(stripHtml(description), 155);
        String bodyText = firstText(body, pageDescription);
        String safeTitle = escapeHtml(pageTitle);
        String safeDescription = escapeHtml(pageDescription);
        String safeBody = escapeHtml(limit(stripHtml(bodyText), 1600));
        String safeImage = imageUrl(origin, image);
        String robots = indexable ? "index,follow,max-image-preview:large" : "noindex,nofollow";
        String ogType = "Article".equals(schemaType) ? "article" : "website";
        String jsonLd = """
                {
                  "@context": "https://schema.org",
                  "@type": "%s",
                  "name": "%s",
                  "description": "%s",
                  "url": "%s"%s%s
                }
                """.formatted(schemaType, escapeJson(name), escapeJson(pageDescription), escapeJson(canonical),
                safeImage.isBlank() ? "" : ",\n  \"image\": \"" + escapeJson(safeImage) + "\"",
                location == null || location.isBlank() ? "" : ",\n  \"address\": \"" + escapeJson(location) + "\"");
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>%s - %s</title>
                  <meta name="description" content="%s">
                  <meta name="robots" content="%s">
                  <link rel="canonical" href="%s">
                  <link rel="alternate" type="application/rss+xml" title="%s最新内容" href="%s/feed.xml">
                  <meta property="og:type" content="%s">
                  <meta property="og:site_name" content="%s">
                  <meta property="og:title" content="%s">
                  <meta property="og:description" content="%s">
                  <meta property="og:url" content="%s">
                  %s
                  <meta name="twitter:card" content="%s">
                  <meta name="twitter:title" content="%s">
                  <meta name="twitter:description" content="%s">
                  <script type="application/ld+json">%s</script>
                </head>
                <body>
                  <main>
                    <h1>%s</h1>
                    <p>%s</p>
                    <p><a href="%s">查看完整页面</a></p>
                  </main>
                </body>
                </html>
                """.formatted(safeTitle, SITE_NAME, safeDescription, robots, canonical, SITE_NAME, origin, ogType,
                SITE_NAME, safeTitle,
                safeDescription, canonical,
                safeImage.isBlank() ? "" : "<meta property=\"og:image\" content=\"" + escapeHtml(safeImage) + "\">",
                safeImage.isBlank() ? "summary" : "summary_large_image", safeTitle, safeDescription,
                jsonLd, safeTitle, safeBody, canonical);
    }

    private String notFound(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return html(request, "页面不存在", "该内容不存在或暂不可访问。", "/", "", "WebPage", SITE_NAME, "", "该内容不存在或暂不可访问。", false);
    }

    private String listingHtml(HttpServletRequest request, String title, String description, String path, String schemaType) {
        String origin = publicOrigin(request);
        String canonical = origin + path;
        List<SeoLink> links = publicLinks(origin);
        StringBuilder linkHtml = new StringBuilder();
        linkHtml.append("<li><a href=\"").append(origin).append("/\">侠客行国旅首页</a></li>\n");
        linkHtml.append("<li><a href=\"").append(origin).append("/scenic\">热门旅游景点</a></li>\n");
        linkHtml.append("<li><a href=\"").append(origin).append("/tickets\">旅游线路预订</a></li>\n");
        linkHtml.append("<li><a href=\"").append(origin).append("/accommodation\">景区住宿推荐</a></li>\n");
        linkHtml.append("<li><a href=\"").append(origin).append("/guide\">旅游攻略</a></li>\n");
        linkHtml.append("<li><a href=\"").append(origin).append("/about\">关于侠客行</a></li>\n");
        for (SeoLink link : links) {
            linkHtml.append("<li><a href=\"")
                    .append(escapeHtml(link.url()))
                    .append("\">")
                    .append(escapeHtml(link.title()))
                    .append("</a>");
            if (!link.summary().isBlank()) {
                linkHtml.append("<p>").append(escapeHtml(limit(stripHtml(link.summary()), 120))).append("</p>");
            }
            linkHtml.append("</li>\n");
        }
        String safeTitle = escapeHtml(limit(title, 70));
        String safeDescription = escapeHtml(limit(stripHtml(description), 155));
        String jsonLd = """
                {
                  "@context": "https://schema.org",
                  "@type": "%s",
                  "name": "%s",
                  "description": "%s",
                  "url": "%s"
                }
                """.formatted(schemaType, escapeJson(title), escapeJson(description), escapeJson(canonical));
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>%s - %s</title>
                  <meta name="description" content="%s">
                  <meta name="robots" content="index,follow,max-image-preview:large">
                  <link rel="canonical" href="%s">
                  <link rel="sitemap" type="application/xml" href="%s/sitemap.xml">
                  <link rel="alternate" type="application/rss+xml" title="%s最新内容" href="%s/feed.xml">
                  <meta property="og:type" content="website">
                  <meta property="og:site_name" content="%s">
                  <meta property="og:title" content="%s">
                  <meta property="og:description" content="%s">
                  <meta property="og:url" content="%s">
                  <meta name="twitter:card" content="summary">
                  <meta name="twitter:title" content="%s">
                  <meta name="twitter:description" content="%s">
                  <script type="application/ld+json">%s</script>
                </head>
                <body>
                  <header>
                    <a href="%s/">%s</a>
                    <nav>
                      <a href="%s/scenic">景点</a>
                      <a href="%s/tickets">线路</a>
                      <a href="%s/accommodation">住宿</a>
                      <a href="%s/guide">攻略</a>
                    </nav>
                  </header>
                  <main>
                    <h1>%s</h1>
                    <p>%s</p>
                    <section>
                      <h2>站内公开链接</h2>
                      <ul>
                        %s
                      </ul>
                    </section>
                  </main>
                </body>
                </html>
                """.formatted(safeTitle, SITE_NAME, safeDescription, canonical, origin, SITE_NAME, origin,
                SITE_NAME, safeTitle,
                safeDescription, canonical, safeTitle, safeDescription, jsonLd, origin, SITE_NAME, origin, origin, origin, origin,
                safeTitle, safeDescription, linkHtml);
    }

    private List<SitemapUrl> sitemapUrls(String origin) {
        List<SitemapUrl> urls = new ArrayList<>();
        addUrl(urls, origin + "/", null, "daily", "1.0");
        addUrl(urls, origin + "/scenic", null, "daily", "0.9");
        addUrl(urls, origin + "/tickets", null, "daily", "0.9");
        addUrl(urls, origin + "/accommodation", null, "daily", "0.8");
        addUrl(urls, origin + "/guide", null, "daily", "0.8");
        addUrl(urls, origin + "/about", null, "monthly", "0.5");
        addUrl(urls, origin + "/sitemap.html", null, "daily", "0.6");

        scenicSpotMapper.selectPage(new Page<>(1, SITEMAP_LIMIT), new LambdaQueryWrapper<ScenicSpot>()
                        .orderByDesc(ScenicSpot::getUpdateTime)).getRecords()
                .forEach(item -> addUrl(urls, origin + "/scenic/" + item.getId(), item.getUpdateTime(), "weekly", "0.8"));
        tourMapper.selectPage(new Page<>(1, SITEMAP_LIMIT), new LambdaQueryWrapper<Tour>()
                        .eq(Tour::getStatus, 1).orderByDesc(Tour::getUpdateTime)).getRecords()
                .forEach(item -> addUrl(urls, origin + "/tour/" + item.getId(), item.getUpdateTime(), "weekly", "0.8"));
        accommodationMapper.selectPage(new Page<>(1, SITEMAP_LIMIT), new LambdaQueryWrapper<Accommodation>()
                        .orderByDesc(Accommodation::getUpdateTime)).getRecords()
                .forEach(item -> addUrl(urls, origin + "/accommodation/" + item.getId(), item.getUpdateTime(), "weekly", "0.7"));
        travelGuideMapper.selectPage(new Page<>(1, SITEMAP_LIMIT), new LambdaQueryWrapper<TravelGuide>()
                        .eq(TravelGuide::getReviewStatus, 1).orderByDesc(TravelGuide::getUpdateTime)).getRecords()
                .forEach(item -> addUrl(urls, origin + "/guide/detail/" + item.getId(), item.getUpdateTime(), "weekly", "0.7"));
        return urls;
    }

    private List<SeoLink> publicLinks(String origin) {
        List<SeoLink> links = new ArrayList<>();
        scenicSpotMapper.selectPage(new Page<>(1, 100), new LambdaQueryWrapper<ScenicSpot>()
                .orderByDesc(ScenicSpot::getUpdateTime)).getRecords()
                .forEach(item -> links.add(new SeoLink(origin + "/scenic/" + item.getId(),
                        Objects.toString(item.getName(), "景点详情"),
                        firstText(item.getDescription(), item.getLocation()))));
        tourMapper.selectPage(new Page<>(1, 100), new LambdaQueryWrapper<Tour>()
                .eq(Tour::getStatus, 1).orderByDesc(Tour::getUpdateTime)).getRecords()
                .forEach(item -> links.add(new SeoLink(origin + "/tour/" + item.getId(),
                        Objects.toString(item.getTitle(), "旅游线路"),
                        firstText(item.getSubtitle(), item.getFeature(), item.getDestination()))));
        accommodationMapper.selectPage(new Page<>(1, 100), new LambdaQueryWrapper<Accommodation>()
                .orderByDesc(Accommodation::getUpdateTime)).getRecords()
                .forEach(item -> links.add(new SeoLink(origin + "/accommodation/" + item.getId(),
                        Objects.toString(item.getName(), "住宿推荐"),
                        firstText(item.getDescription(), item.getAddress(), item.getFeatures()))));
        travelGuideMapper.selectPage(new Page<>(1, 100), new LambdaQueryWrapper<TravelGuide>()
                .eq(TravelGuide::getReviewStatus, 1).orderByDesc(TravelGuide::getUpdateTime)).getRecords()
                .forEach(item -> links.add(new SeoLink(origin + "/guide/detail/" + item.getId(),
                        Objects.toString(item.getTitle(), "旅游攻略"),
                        firstText(item.getContent(), item.getDestination()))));
        return links;
    }

    private List<FeedItem> publicFeedItems(String origin) {
        List<FeedItem> items = new ArrayList<>();
        scenicSpotMapper.selectPage(new Page<>(1, FEED_LIMIT), new LambdaQueryWrapper<ScenicSpot>()
                        .orderByDesc(ScenicSpot::getUpdateTime)).getRecords()
                .forEach(item -> items.add(new FeedItem(
                        Objects.toString(item.getName(), "景点详情"),
                        origin + "/scenic/" + item.getId(),
                        firstText(item.getDescription(), item.getLocation()),
                        item.getUpdateTime())));
        tourMapper.selectPage(new Page<>(1, FEED_LIMIT), new LambdaQueryWrapper<Tour>()
                        .eq(Tour::getStatus, 1).orderByDesc(Tour::getUpdateTime)).getRecords()
                .forEach(item -> items.add(new FeedItem(
                        Objects.toString(item.getTitle(), "旅游线路"),
                        origin + "/tour/" + item.getId(),
                        firstText(item.getSubtitle(), item.getFeature(), item.getDestination()),
                        item.getUpdateTime())));
        accommodationMapper.selectPage(new Page<>(1, FEED_LIMIT), new LambdaQueryWrapper<Accommodation>()
                        .orderByDesc(Accommodation::getUpdateTime)).getRecords()
                .forEach(item -> items.add(new FeedItem(
                        Objects.toString(item.getName(), "住宿推荐"),
                        origin + "/accommodation/" + item.getId(),
                        firstText(item.getDescription(), item.getAddress(), item.getFeatures()),
                        item.getUpdateTime())));
        travelGuideMapper.selectPage(new Page<>(1, FEED_LIMIT), new LambdaQueryWrapper<TravelGuide>()
                        .eq(TravelGuide::getReviewStatus, 1).orderByDesc(TravelGuide::getUpdateTime)).getRecords()
                .forEach(item -> items.add(new FeedItem(
                        Objects.toString(item.getTitle(), "旅游攻略"),
                        origin + "/guide/detail/" + item.getId(),
                        firstText(item.getContent(), item.getDestination()),
                        item.getUpdateTime())));
        items.sort((left, right) -> {
            LocalDateTime leftTime = left.updateTime() == null ? LocalDateTime.MIN : left.updateTime();
            LocalDateTime rightTime = right.updateTime() == null ? LocalDateTime.MIN : right.updateTime();
            return rightTime.compareTo(leftTime);
        });
        return items.size() > FEED_LIMIT ? items.subList(0, FEED_LIMIT) : items;
    }

    private void addUrl(List<SitemapUrl> urls, String loc, LocalDateTime lastmod, String changefreq, String priority) {
        urls.add(new SitemapUrl(loc, lastmod, changefreq, priority));
    }

    private String publicOrigin(HttpServletRequest request) {
        String configuredOrigin = normalizeOrigin(configuredPublicOrigin);
        if (!configuredOrigin.isBlank()) {
            return configuredOrigin;
        }
        String proto = headerOrDefault(request, "X-Forwarded-Proto", request.getScheme());
        String host = headerOrDefault(request, "X-Forwarded-Host", request.getHeader("Host"));
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        return proto.toLowerCase(Locale.ROOT) + "://" + host;
    }

    private String normalizeOrigin(String origin) {
        String value = Objects.toString(origin, "").trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String headerOrDefault(HttpServletRequest request, String header, String fallback) {
        String value = request.getHeader(header);
        return value == null || value.isBlank() ? fallback : value.split(",", 2)[0].trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !stripHtml(value).isBlank()) {
                return value;
            }
        }
        return SITE_NAME;
    }

    private String joinText(String... values) {
        StringBuilder text = new StringBuilder();
        for (String value : values) {
            String clean = stripHtml(value);
            if (clean.isBlank()) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(clean);
        }
        return text.isEmpty() ? SITE_NAME : text.toString();
    }

    private String labelText(String label, Object value) {
        String text = stripHtml(Objects.toString(value, ""));
        return text.isBlank() ? "" : label + text;
    }

    private String rssDate(LocalDateTime dateTime) {
        LocalDateTime value = dateTime == null ? LocalDateTime.now() : dateTime;
        return value.atZone(CHINA_ZONE).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private String imageUrl(String origin, String image) {
        if (image == null || image.isBlank()) {
            return "";
        }
        if (image.startsWith("http://") || image.startsWith("https://")) {
            return image;
        }
        return image.startsWith("/") ? origin + image : origin + "/" + image;
    }

    private String limit(String value, int maxLength) {
        String normalized = Objects.toString(value, "").replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String stripHtml(String value) {
        return Objects.toString(value, "")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&");
    }

    private String escapeHtml(String value) {
        return Objects.toString(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeXml(String value) {
        return escapeHtml(value);
    }

    private String escapeJson(String value) {
        return Objects.toString(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private record SitemapUrl(String loc, LocalDateTime lastmod, String changefreq, String priority) {
    }

    private record SeoLink(String url, String title, String summary) {
    }

    private record FeedItem(String title, String link, String summary, LocalDateTime updateTime) {
    }
}
