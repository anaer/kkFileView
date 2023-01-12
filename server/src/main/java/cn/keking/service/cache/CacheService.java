package cn.keking.service.cache;

import java.util.List;
import java.util.Map;

/**
 * @author: chenjh
 * @since: 2019/4/2 16:45
 */
public interface CacheService {

    /**
     * pdf文件相对路径缓存.
     * k: uniqueKey
     * v: 文件相对路径
     */
    String FILE_PREVIEW_PDF_KEY = "converted-preview-pdf-file";

    /**
     * pdf文件 图片数量.
     * k: uniqueKey
     * v: 图片数量
     */
    String FILE_PREVIEW_PDF_IMGS_KEY = "converted-preview-pdfimgs-file";
    /**
     * 压缩包内图片文件集合
     * k: uniqueKey
     * v: 图片url列表
     */
    String FILE_PREVIEW_IMGS_KEY = "converted-preview-imgs-file";
    /**
     * 视频转码 链接映射关系
     * k: 原视频url
     * v: 转码视频url
     */
    String FILE_PREVIEW_MEDIA_CONVERT_KEY = "converted-preview-media-file";
    /**
     * 转码队列.
     * v: 待转码url
     */
    String TASK_QUEUE_NAME = "convert-task";

    /**
     * JDK缓存 设置缓存容量
     */
    Integer DEFAULT_PDF_CAPACITY = 500000;
    Integer DEFAULT_PDFIMG_CAPACITY = 500000;
    Integer DEFAULT_IMG_CAPACITY = 500000;
    Integer DEFAULT_MEDIACONVERT_CAPACITY = 500000;

    /**
     * JDK缓存 初始化
     * @param capacity
     */
    void initPDFCachePool(Integer capacity);
    void initIMGCachePool(Integer capacity);
    void initPdfImagesCachePool(Integer capacity);
    void initMediaConvertCachePool(Integer capacity);

    /**
     * pdf相对路径缓存.
     */
    void putPDFCache(String key, String value);
    Map<String, String> getPDFCache();
    String getPDFCache(String key);

    /**
     * pdf图片数量缓存
     */
    void putPdfImageCache(String pdfFilePath, int num);
    Integer getPdfImageCache(String key);

    /**
     * 压缩包中图片列表缓存.
     */
    void putImgCache(String key, List<String> value);
    Map<String, List<String>> getImgCache();
    List<String> getImgCache(String key);

    /**
     * 视频转码链接映射缓存.
     */
    void putMediaConvertCache(String key, String value);
    Map<String, String> getMediaConvertCache();
    String getMediaConvertCache(String key);

    /**
     * 转码队列
     */
    void addQueueTask(String url);
    String takeQueueTask() throws InterruptedException;

    /**
     * 清空缓存
     */
    void cleanCache();
}
