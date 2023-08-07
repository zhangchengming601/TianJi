package com.tianji.learning.utils;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.StringUtils;

import com.tianji.learning.entity.LearningLesson;
import com.tianji.learning.entity.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LearningRecordDelayTaskHandler {

    private final StringRedisTemplate redisTemplate;
    private final DelayQueue<DelayTask<RecordTaskData>> queue = new DelayQueue<>();
    private final static String RECORD_KEY_TEMPLATE = "learning:record:{}";
    private final LearningRecordMapper recordMapper;
    private final ILearningLessonService lessonService;
    private static volatile boolean begin = true;

    static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16,
            18,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10)
    );


    // @PostConstruct注解：
    @PostConstruct
    public void init(){
        CompletableFuture.runAsync(this::handleDelayTask);
    }
    @PreDestroy
    public void destroy(){
        log.debug("关闭学习记录处理的延迟任务");
        begin = false;
    }

    /**
     * 处理延迟队列中的延迟任务
     * */
    private void handleDelayTask(){
        while (begin){
            try {
                // 1.尝试获取任务
                DelayTask<RecordTaskData> task = queue.take();  // 尝试获取队列中的元素的时候，如果队列中没有元素，则会阻塞
                log.debug("获取到要处理的播放记录任务");

                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        RecordTaskData data = task.getData();
                        // 2.读取Redis缓存
                        LearningRecord record = readRecordCache(data.getLessonId(), data.getSectionId());
                        log.debug("获取到要处理的播放记录任务，  任务数据{}，  缓存中的数据{}  ",data,record);
                        if (record == null) {
                            return;
                        }
                        // 3.比较数据
                        if(!Objects.equals(data.getMoment(), record.getMoment())){
                            // 4.如果不一致，播放进度在变化，无需持久化
                            return;
                        }
                        // 5.如果一致，证明用户离开了视频，需要持久化
                        // 5.1.更新学习记录
                        record.setFinished(null);
                        recordMapper.updateById(record);
                        // 5.2.更新课表
                        LearningLesson lesson = new LearningLesson();
                        lesson.setId(data.getLessonId());
                        lesson.setLatestSectionId(data.getSectionId());
                        lesson.setLatestLearnTime(LocalDateTime.now());
                        lessonService.updateById(lesson);

                        log.debug("准备持久化学习记录信息");
                    }
                });
            } catch (Exception e) {
                log.error("处理播放记录任务发生异常", e);
            }
        }
    }


    /**
     * 向redis中添加缓存，向延迟队列中添加延迟任务
     * */
    public void addLearningRecordTask(LearningRecord record){
        // 1.添加数据到Redis缓存
        writeRecordCache(record);
        // 2.提交延迟任务到延迟队列 DelayQueue
        queue.add(new DelayTask<>(new RecordTaskData(record), Duration.ofSeconds(20)));
    }

    public void writeRecordCache(LearningRecord record) {
        log.debug("更新学习记录的缓存数据");
        try {
            // 1.数据转换
            String json = JsonUtils.toJsonStr(new RecordCacheData(record));
            // 2.写入Redis    learning:record:{}
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, record.getLessonId());
            redisTemplate.opsForHash().put(key, record.getSectionId().toString(), json);
            // 3.添加缓存过期时间
            redisTemplate.expire(key, Duration.ofMinutes(1));
        } catch (Exception e) {
            log.error("更新学习记录缓存异常", e);
        }
    }

    /**
     * 从redis中读取缓存(学习记录)
     * */
    public LearningRecord readRecordCache(Long lessonId, Long sectionId){
        try {
            // 1.读取Redis数据
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
            Object cacheData = redisTemplate.opsForHash().get(key, sectionId.toString());
            if (cacheData == null) {
                return null;
            }
            // 2.数据检查和转换
            return JsonUtils.toBean(cacheData.toString(), LearningRecord.class);
        } catch (Exception e) {
            log.error("缓存读取异常", e);
            return null;
        }
    }


    /**
     * 删除缓存，删除的是lessonid下的sectionid
     * */
    public void cleanRecordCache(Long lessonId, Long sectionId){
        // 删除数据
        String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
        redisTemplate.opsForHash().delete(key, sectionId.toString());
    }

    @Data
    @NoArgsConstructor
    private static class RecordCacheData{
        private Long id;
        private Integer moment;
        private Boolean finished;

        public RecordCacheData(LearningRecord record) {
            this.id = record.getId();
            this.moment = record.getMoment();
            this.finished = record.getFinished();
        }
    }
    @Data
    @NoArgsConstructor
    private static class RecordTaskData{
        private Long lessonId;
        private Long sectionId;
        private Integer moment;

        public RecordTaskData(LearningRecord record) {
            this.lessonId = record.getLessonId();
            this.sectionId = record.getSectionId();
            this.moment = record.getMoment();
        }
    }
}
