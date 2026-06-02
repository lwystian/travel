package org.example.springboot.util;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 通用工具类。
 */
@Component
@Slf4j
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public boolean expire(@NonNull String key, long time) {
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

    public long getExpire(@NonNull String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public boolean hasKey(@NonNull String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public void del(@NonNull String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(Objects.requireNonNull(key[0], "redis key must not be null"));
            } else {
                redisTemplate.delete(Objects.requireNonNull(List.of(key), "redis keys must not be null"));
            }
        }
    }

    public void delByPrefix(@NonNull String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Nullable
    public Object get(@Nullable String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    public boolean set(@NonNull String key, @NonNull Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean set(@NonNull String key, @NonNull Object value, long time) {
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

    public long incr(@NonNull String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于等于0");
        }
        Long value = redisTemplate.opsForValue().increment(key, delta);
        return value == null ? 0 : value;
    }

    public long decr(@NonNull String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于等于0");
        }
        Long value = redisTemplate.opsForValue().decrement(key, delta);
        return value == null ? 0 : value;
    }

    @Nullable
    public Object hget(@NonNull String key, @NonNull String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    public Map<Object, Object> hmget(@NonNull String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public boolean hmset(@NonNull String key, @NonNull Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean hmset(@NonNull String key, @NonNull Map<String, Object> map, long time) {
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

    public boolean hset(@NonNull String key, @NonNull String item, @NonNull Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean hset(@NonNull String key, @NonNull String item, @NonNull Object value, long time) {
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

    public void hdel(@NonNull String key, @NonNull Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    public boolean hHasKey(@NonNull String key, @NonNull String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    public long hsize(@NonNull String key) {
        try {
            return redisTemplate.opsForHash().size(key);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    public double hincr(@NonNull String key, @NonNull String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    public double hdecr(@NonNull String key, @NonNull String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    @Nullable
    public Set<Object> sGet(@NonNull String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public long sSet(@NonNull String key, @NonNull Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            return count == null ? 0 : count;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    public long sSetAndTime(@NonNull String key, long time, @NonNull Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count == null ? 0 : count;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    public long sGetSetSize(@NonNull String key) {
        try {
            Long size = redisTemplate.opsForSet().size(key);
            return size == null ? 0 : size;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    public long setRemove(@NonNull String key, @NonNull Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count == null ? 0 : count;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Nullable
    public List<Object> lGet(@NonNull String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public long lGetListSize(@NonNull String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            return size == null ? 0 : size;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Nullable
    public Object lGetIndex(@NonNull String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public boolean lSet(@NonNull String key, @NonNull Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean lSet(@NonNull String key, @NonNull Object value, long time) {
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

    public boolean lSet(@NonNull String key, @NonNull List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean lSet(@NonNull String key, @NonNull List<Object> value, long time) {
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

    public boolean lUpdateIndex(@NonNull String key, long index, @NonNull Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public long lRemove(@NonNull String key, long count, @NonNull Object value) {
        try {
            Long removed = redisTemplate.opsForList().remove(key, count, value);
            return removed == null ? 0 : removed;
        } catch (Exception e) {
            log.warn("Redis operation failed: {}", e.getMessage(), e);
            return 0;
        }
    }
}
