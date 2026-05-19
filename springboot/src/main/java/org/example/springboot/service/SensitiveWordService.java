package org.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.springboot.entity.SensitiveWord;

import java.util.List;

public interface SensitiveWordService extends IService<SensitiveWord> {
    Page<SensitiveWord> getWordsByPage(String keyword, String category, String level, Integer status,
                                       Integer currentPage, Integer size);

    SensitiveCheckResult checkText(String content, String contentType, String objectId);

    String filterContent(String content, String contentType, String objectId);

    void refreshCache();

    class SensitiveCheckResult {
        private boolean hit;
        private boolean rejected;
        private String handleType;
        private String filteredContent;
        private List<SensitiveWordHit> hits;

        public boolean isHit() {
            return hit;
        }

        public void setHit(boolean hit) {
            this.hit = hit;
        }

        public boolean isRejected() {
            return rejected;
        }

        public void setRejected(boolean rejected) {
            this.rejected = rejected;
        }

        public String getHandleType() {
            return handleType;
        }

        public void setHandleType(String handleType) {
            this.handleType = handleType;
        }

        public String getFilteredContent() {
            return filteredContent;
        }

        public void setFilteredContent(String filteredContent) {
            this.filteredContent = filteredContent;
        }

        public List<SensitiveWordHit> getHits() {
            return hits;
        }

        public void setHits(List<SensitiveWordHit> hits) {
            this.hits = hits;
        }
    }

    class SensitiveWordHit {
        private String word;
        private String category;
        private String level;

        public SensitiveWordHit(String word, String category, String level) {
            this.word = word;
            this.category = category;
            this.level = level;
        }

        public String getWord() {
            return word;
        }

        public String getCategory() {
            return category;
        }

        public String getLevel() {
            return level;
        }
    }
}
