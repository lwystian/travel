package org.example.springboot.util;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis宸ュ叿绫?
 */
@Component
@Slf4j
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 鎸囧畾缂撳瓨澶辨晥鏃堕棿
     *
     * @param key  閿?
     * @param time 鏃堕棿(绉?
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 鏍规嵁key鑾峰彇杩囨湡鏃堕棿
     *
     * @param key 閿?
     * @return 鏃堕棿(绉? 杩斿洖0浠ｈ〃姘镐箙鏈夋晥
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 鍒ゆ柇key鏄惁瀛樺湪
     *
     * @param key 閿?
     * @return true 瀛樺湪 false涓嶅瓨鍦?
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 鍒犻櫎缂撳瓨
     *
     * @param key 鍙互浼犱竴涓€兼垨澶氫釜
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete((Collection<String>) List.of(key));
            }
        }
    }

    /**
     * 鎸夊墠缂€鍒犻櫎缂撳瓨
     *
     * @param prefix 鍓嶇紑
     */
    public void delByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ============================String=============================

    /**
     * 鏅€氱紦瀛樿幏鍙?
     *
     * @param key 閿?
     * @return 鍊?
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 鏅€氱紦瀛樻斁鍏?
     *
     * @param key   閿?
     * @param value 鍊?
     * @return true鎴愬姛 false澶辫触
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 鏅€氱紦瀛樻斁鍏ュ苟璁剧疆鏃堕棿
     *
     * @param key   閿?
     * @param value 鍊?
     * @param time  鏃堕棿(绉? time瑕佸ぇ浜? 濡傛灉time灏忎簬绛変簬0 灏嗚缃棤闄愭湡
     * @return true鎴愬姛 false 澶辫触
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 閫掑
     *
     * @param key   閿?
     * @param delta 瑕佸鍔犲嚑(澶т簬0)
     * @return 閫掑鍚庣殑鍊?
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("閫掑鍥犲瓙蹇呴』澶т簬0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 閫掑噺
     *
     * @param key   閿?
     * @param delta 瑕佸噺灏戝嚑(灏忎簬0)
     * @return 閫掑噺鍚庣殑鍊?
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("閫掑噺鍥犲瓙蹇呴』澶т簬0");
        }
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // ================================Map=================================

    /**
     * HashGet
     *
     * @param key  閿?涓嶈兘涓簄ull
     * @param item 椤?涓嶈兘涓簄ull
     * @return 鍊?
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 鑾峰彇hashKey瀵瑰簲鐨勬墍鏈夐敭鍊?
     *
     * @param key 閿?
     * @return 瀵瑰簲鐨勫涓敭鍊?
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 閿?
     * @param map 瀵瑰簲澶氫釜閿€?
     * @return true 鎴愬姛 false 澶辫触
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * HashSet 骞惰缃椂闂?
     *
     * @param key  閿?
     * @param map  瀵瑰簲澶氫釜閿€?
     * @param time 鏃堕棿(绉?
     * @return true鎴愬姛 false澶辫触
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 鍚戜竴寮爃ash琛ㄤ腑鏀惧叆鏁版嵁,濡傛灉涓嶅瓨鍦ㄥ皢鍒涘缓
     *
     * @param key   閿?
     * @param item  椤?
     * @param value 鍊?
     * @return true 鎴愬姛 false澶辫触
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 鍚戜竴寮爃ash琛ㄤ腑鏀惧叆鏁版嵁,濡傛灉涓嶅瓨鍦ㄥ皢鍒涘缓
     *
     * @param key   閿?
     * @param item  椤?
     * @param value 鍊?
     * @param time  鏃堕棿(绉? 娉ㄦ剰:濡傛灉宸插瓨鍦ㄧ殑hash琛ㄦ湁鏃堕棿,杩欓噷灏嗕細鏇挎崲鍘熸湁鐨勬椂闂?
     * @return true 鎴愬姛 false澶辫触
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 鍒犻櫎hash琛ㄤ腑鐨勫€?
     *
     * @param key  閿?涓嶈兘涓簄ull
     * @param item 椤?鍙互浣垮涓?涓嶈兘涓簄ull
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 鍒ゆ柇hash琛ㄤ腑鏄惁鏈夎椤圭殑鍊?
     *
     * @param key  閿?涓嶈兘涓簄ull
     * @param item 椤?涓嶈兘涓簄ull
     * @return true 瀛樺湪 false涓嶅瓨鍦?
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * 鑾峰彇hash琛ㄧ殑澶у皬
     *
     * @param key 閿?涓嶈兘涓簄ull
     * @return hash澶у皬
     */
    public long hsize(String key) {
        try {
            return redisTemplate.opsForHash().size(key);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * hash閫掑 濡傛灉涓嶅瓨鍦?灏变細鍒涘缓涓€涓?骞舵妸鏂板鍚庣殑鍊艰繑鍥?
     *
     * @param key  閿?
     * @param item 椤?
     * @param by   瑕佸鍔犲嚑(澶т簬0)
     * @return 閫掑鍚庣殑鍊?
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash閫掑噺
     *
     * @param key  閿?
     * @param item 椤?
     * @param by   瑕佸噺灏戣(灏忎簬0)
     * @return 閫掑噺鍚庣殑鍊?
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    // ============================Set=============================

    /**
     * 鏍规嵁key鑾峰彇Set涓殑鎵€鏈夊€?
     *
     * @param key 閿?
     * @return Set闆嗗悎
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 灏嗘暟鎹斁鍏et缂撳瓨
     *
     * @param key    閿?
     * @param values 鍊?鍙互鏄涓?
     * @return 鎴愬姛涓暟
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 灏唖et鏁版嵁鏀惧叆缂撳瓨
     *
     * @param key    閿?
     * @param time   鏃堕棿(绉?
     * @param values 鍊?鍙互鏄涓?
     * @return 鎴愬姛涓暟
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 鑾峰彇set缂撳瓨鐨勯暱搴?
     *
     * @param key 閿?
     * @return 闀垮害
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 绉婚櫎鍊间负value鐨?
     *
     * @param key    閿?
     * @param values 鍊?鍙互鏄涓?
     * @return 绉婚櫎鐨勪釜鏁?
     */
    public long setRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ===============================List=================================

    /**
     * 鑾峰彇list缂撳瓨鐨勫唴瀹?
     *
     * @param key   閿?
     * @param start 寮€濮?
     * @param end   缁撴潫 0 鍒?-1浠ｈ〃鎵€鏈夊€?
     * @return List
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 鑾峰彇list缂撳瓨鐨勯暱搴?
     *
     * @param key 閿?
     * @return 闀垮害
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 閫氳繃绱㈠紩 鑾峰彇list涓殑鍊?
     *
     * @param key   閿?
     * @param index 绱㈠紩 index>=0鏃讹紝 0 琛ㄥご锛? 绗簩涓厓绱狅紝渚濇绫绘帹锛沬ndex<0鏃讹紝-1锛岃〃灏撅紝-2鍊掓暟绗簩涓厓绱狅紝渚濇绫绘帹
     * @return 鍊?
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 灏唋ist鏀惧叆缂撳瓨
     *
     * @param key   閿?
     * @param value 鍊?
     * @return 鎴愬姛涓庡惁
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 灏唋ist鏀惧叆缂撳瓨
     *
     * @param key   閿?
     * @param value 鍊?
     * @param time  鏃堕棿(绉?
     * @return 鎴愬姛涓庡惁
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 灏唋ist鏀惧叆缂撳瓨
     *
     * @param key   閿?
     * @param value 鍊?
     * @return 鎴愬姛涓庡惁
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 灏唋ist鏀惧叆缂撳瓨
     *
     * @param key   閿?
     * @param value 鍊?
     * @param time  鏃堕棿(绉?
     * @return 鎴愬姛涓庡惁
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 鏍规嵁绱㈠紩淇敼list涓殑鏌愭潯鏁版嵁
     *
     * @param key   閿?
     * @param index 绱㈠紩
     * @param value 鍊?
     * @return 鎴愬姛涓庡惁
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 绉婚櫎N涓€间负value
     *
     * @param key   閿?
     * @param count 绉婚櫎澶氬皯涓?
     * @param value 鍊?
     * @return 绉婚櫎鐨勪釜鏁?
     */
    public long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }
} 
