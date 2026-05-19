package org.example.springboot.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.springboot.entity.SensitiveWord;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.SensitiveWordMapper;
import org.example.springboot.service.SensitiveLogService;
import org.example.springboot.service.SensitiveWordService;
import org.example.springboot.service.SiteNotificationService;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord> implements SensitiveWordService {
    private static final long CACHE_TTL_MS = 60_000L;
    private static final String DEFAULT_REPLACEMENT = "***";
    private static final String LOOSE_SEPARATOR = "[\\s\\p{Punct}·•・。，、；：？！“”‘’（）【】《》、/\\\\_-]*[个的了吧呀啊呢哦哈嘛么啦]{0,2}[\\s\\p{Punct}·•・。，、；：？！“”‘’（）【】《》、/\\\\_-]*";

    @Resource
    private SensitiveLogService sensitiveLogService;
    @Resource
    private SiteNotificationService siteNotificationService;

    private volatile List<SensitiveWord> cachedRules = Collections.emptyList();
    private volatile long cacheExpireAt = 0L;

    @Override
    public Page<SensitiveWord> getWordsByPage(String keyword, String category, String level, Integer status,
                                              Integer currentPage, Integer size) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(keyword), SensitiveWord::getWord, keyword)
                .eq(StringUtils.isNotBlank(category), SensitiveWord::getCategory, category)
                .eq(StringUtils.isNotBlank(level), SensitiveWord::getLevel, level)
                .eq(status != null, SensitiveWord::getStatus, status)
                .orderByDesc(SensitiveWord::getUpdateTime)
                .orderByDesc(SensitiveWord::getCreateTime);
        return this.page(new Page<>(currentPage, size), wrapper);
    }

    @Override
    public SensitiveCheckResult checkText(String content, String contentType, String objectId) {
        SensitiveCheckResult result = detect(content);
        if (result.isHit()) {
            recordDetection(content, contentType, objectId, result);
        }
        return result;
    }

    @Override
    public String filterContent(String content, String contentType, String objectId) {
        SensitiveCheckResult result = checkText(content, contentType, objectId);
        if (result.isRejected()) {
            siteNotificationService.sendToAdmins(
                    "内容触发敏感词拦截",
                    "用户提交的" + humanContentType(contentType) + "触发禁止发布规则，系统已自动拦截。",
                    "SECURITY",
                    contentType,
                    objectId,
                    "/back/sensitive-word"
            );
            throw new ServiceException("内容包含禁止发布的信息，请修改后再提交");
        }
        return result.getFilteredContent();
    }

    private String humanContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "内容";
        }
        return switch (contentType) {
            case "COMMENT" -> "景区评论";
            case "ACCOMMODATION_REVIEW" -> "酒店评论";
            case "GUIDE_TITLE" -> "攻略标题";
            case "GUIDE_DESTINATION" -> "攻略目的地";
            case "GUIDE" -> "旅游攻略";
            default -> "内容";
        };
    }

    @Override
    public void refreshCache() {
        cacheExpireAt = 0L;
        loadRules();
    }

    private SensitiveCheckResult detect(String content) {
        SensitiveCheckResult result = new SensitiveCheckResult();
        result.setFilteredContent(content);
        result.setHandleType("PASS");
        result.setHits(new ArrayList<>());

        if (StringUtils.isBlank(content)) {
            return result;
        }

        String filtered = content;
        boolean rejected = false;
        for (SensitiveWord rule : loadRules()) {
            String word = rule.getWord();
            if (StringUtils.isBlank(word) || !matches(content, word, rule.getMatchType())) {
                continue;
            }

            result.getHits().add(new SensitiveWordHit(word, rule.getCategory(), rule.getLevel()));
            if ("BLOCK".equalsIgnoreCase(rule.getLevel())) {
                rejected = true;
            }
            filtered = replaceIgnoreCase(filtered, word,
                    StringUtils.isBlank(rule.getReplacement()) ? DEFAULT_REPLACEMENT : rule.getReplacement());
        }

        boolean hit = !result.getHits().isEmpty();
        result.setHit(hit);
        result.setRejected(rejected);
        result.setFilteredContent(hit ? filtered : content);
        if (rejected) {
            result.setHandleType("REJECT");
        } else if (hit) {
            result.setHandleType("REPLACE");
        }
        return result;
    }

    private boolean matches(String content, String word, String matchType) {
        String normalizedWord = normalizeForMatch(word);
        if (StringUtils.isBlank(normalizedWord)) {
            return false;
        }
        String normalizedContent = normalizeForMatch(content);
        boolean exact = "EXACT".equalsIgnoreCase(matchType);
        if (exact && normalizedContent.equals(normalizedWord)) {
            return true;
        }
        if (!exact && normalizedContent.contains(normalizedWord)) {
            return true;
        }
        Pattern pattern = buildPattern(word, exact);
        return pattern.matcher(content).find();
    }

    private Pattern buildPattern(String word, boolean exact) {
        StringBuilder pattern = new StringBuilder();
        if (exact) {
            pattern.append("^\\s*");
        }
        String normalizedWord = normalizeWord(word);
        for (int i = 0; i < normalizedWord.length(); i++) {
            if (i > 0) {
                pattern.append(LOOSE_SEPARATOR);
            }
            pattern.append(Pattern.quote(String.valueOf(normalizedWord.charAt(i))));
        }
        if (exact) {
            pattern.append("\\s*$");
        }
        return Pattern.compile(pattern.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private String normalizeWord(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private String normalizeForMatch(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        normalized = normalized
                .replace('０', '0')
                .replace('１', '1')
                .replace('２', '2')
                .replace('３', '3')
                .replace('４', '4')
                .replace('５', '5')
                .replace('６', '6')
                .replace('７', '7')
                .replace('８', '8')
                .replace('９', '9')
                .replace('ｑ', 'q')
                .replace('Ｑ', 'q')
                .replace("扣扣", "qq")
                .replace("企鹅号", "qq")
                .replace("微 信", "微信")
                .replace("v信", "微信")
                .replace("vx", "微信")
                .replace("v x", "微信");
        normalized = normalized.replaceAll("[\\s\\p{Punct}·•・。，、；：？！“”‘’（）【】《》/\\\\_-]+", "");
        normalized = normalized.replaceAll("(一个|一下|我们|咱们|可以|帮忙|麻烦|请|个|我|你|您|他|她|它|了|吧|呀|啊|呢|哦|哈|嘛|么|啦)", "");
        return normalized;
    }

    private String replaceIgnoreCase(String content, String word, String replacement) {
        Pattern pattern = buildPattern(word, false);
        return pattern.matcher(content).replaceAll(Matcher.quoteReplacement(replacement));
    }

    private List<SensitiveWord> loadRules() {
        long now = System.currentTimeMillis();
        if (now < cacheExpireAt && !cachedRules.isEmpty()) {
            return cachedRules;
        }

        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensitiveWord::getStatus, 1);
        List<SensitiveWord> rules = this.list(wrapper);
        rules.sort(Comparator.comparingInt((SensitiveWord item) ->
                item.getWord() == null ? 0 : item.getWord().length()).reversed());
        cachedRules = rules;
        cacheExpireAt = now + CACHE_TTL_MS;
        return cachedRules;
    }

    private void recordDetection(String content, String contentType, String objectId, SensitiveCheckResult result) {
        User user = getCurrentUserQuietly();
        HttpServletRequest request = currentRequest();
        sensitiveLogService.recordSensitiveCheck(
                user == null ? null : user.getId(),
                user == null ? null : user.getUsername(),
                trim(content, 1000),
                contentType,
                objectId,
                JSON.toJSONString(result.getHits()),
                result.getHits().size(),
                result.getHandleType(),
                trim(result.getFilteredContent(), 1000),
                getClientIp(request),
                request == null ? null : request.getHeader("User-Agent")
        );
    }

    private User getCurrentUserQuietly() {
        try {
            return JwtTokenUtils.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip == null ? null : ip.split(",")[0].trim();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @Override
    public boolean save(SensitiveWord entity) {
        prepare(entity, true);
        boolean saved = super.save(entity);
        refreshCache();
        return saved;
    }

    @Override
    public boolean updateById(SensitiveWord entity) {
        prepare(entity, false);
        boolean updated = super.updateById(entity);
        refreshCache();
        return updated;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        boolean removed = super.removeById(id);
        refreshCache();
        return removed;
    }

    private void prepare(SensitiveWord word, boolean create) {
        LocalDateTime now = LocalDateTime.now();
        if (create) {
            word.setCreateTime(now);
            if (word.getStatus() == null) {
                word.setStatus(1);
            }
        }
        word.setUpdateTime(now);
        if (StringUtils.isBlank(word.getLevel())) {
            word.setLevel("REVIEW");
        }
        if (StringUtils.isBlank(word.getMatchType())) {
            word.setMatchType("CONTAINS");
        }
        if (StringUtils.isBlank(word.getReplacement())) {
            word.setReplacement(DEFAULT_REPLACEMENT);
        }
    }
}
