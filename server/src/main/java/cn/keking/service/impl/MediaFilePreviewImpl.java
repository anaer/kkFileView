package cn.keking.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.KkFileUtils;
import cn.keking.service.FileHandlerService;
import cn.keking.web.filter.BaseUrlFilter;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import java.io.File;

/**
 * @author : kl
 * @authorboke : kailing.pub
 * @create : 2018-03-25 上午11:58
 * @description:
 **/
@Service
public class MediaFilePreviewImpl implements FilePreview {

    private final FileHandlerService fileHandlerService;
    private final OtherFilePreviewImpl otherFilePreview;

    private static Object LOCK = new Object();

    public MediaFilePreviewImpl(FileHandlerService fileHandlerService,
            OtherFilePreviewImpl otherFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.otherFilePreview = otherFilePreview;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        String uniqueKey = fileAttribute.getUniqueKey();
        String suffix = fileAttribute.getSuffix();
        Boolean refresh = fileAttribute.getRefresh();

        boolean isHttp = StrUtil.startWithIgnoreCase(url, "http");

        // 1. 链接是http开头, 而且视频格式 浏览器支持直接播放的, 直接返回
        if (isHttp && checkMediaSupport(suffix)) {
            model.addAttribute("mediaUrl", url);
            return MEDIA_FILE_PREVIEW_PAGE;
        }

        // 是否使用缓存, 配置开启缓存 且 未传强制刷新参数 同时 文件预览过
        boolean useCache = fileHandlerService.isUseCache(uniqueKey, refresh);

        String mediaPath = null;
        if (useCache) {
            mediaPath = fileHandlerService.getConvertedFile(uniqueKey);
        } else {
            boolean needConvert = checkNeedConvert(fileAttribute.getSuffix());
            // 下载视频文件 1.链接不是http开头， 浏览器不能直接访问 2. 是http开头 但是需要转码
            if (!isHttp || needConvert) {
                ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute,
                        fileAttribute.getFileName());
                if (response.isFailure()) {
                    return otherFilePreview.notSupportedFile(model, fileAttribute,
                            response.getMsg());
                }
                String srcMediaPath = response.getContent();

                if (needConvert) {
                    mediaPath = convertUrl(fileAttribute, srcMediaPath);
                    if (ConfigConstants.isCacheEnabled() && StrUtil.isNotBlank(mediaPath)) {
                        fileHandlerService.addConvertedFile(uniqueKey, KkFileUtils.getUrlRelativePath(mediaPath));
                    }
                }
            }

        }

        if (StrUtil.isNotBlank(mediaPath)) {
            model.addAttribute("mediaUrl", BaseUrlFilter.getBaseUrl() + StrUtil.replace(mediaPath, "\\", "/"));
            return MEDIA_FILE_PREVIEW_PAGE;
        }

        return otherFilePreview.notSupportedFile(model, fileAttribute, "暂不支持");
    }

    /**
     * 检查视频文件处理逻辑
     * @return 转码后视频绝对路径
     */
    private String convertUrl(FileAttribute fileAttribute, String mediaPath) {
        String convertedPath = null;
        synchronized (LOCK) {
            convertedPath = convertToMp4(fileAttribute, mediaPath);
        }
        return convertedPath;
    }

    /**
     * 检查视频文件转换是否已开启，以及当前文件是否需要转换
     * @return
     */
    private boolean checkNeedConvert(String suffix) {
        //1.检查开关是否开启
        if ("false".equals(ConfigConstants.getMediaConvertDisable())) {
            return false;
        }
        //2.检查当前文件是否需要转换
        String[] mediaTypesConvert = FileType.MEDIA_TYPES_CONVERT;
        String type = suffix;
        for (String temp : mediaTypesConvert) {
            if (type.equals(temp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查视频文件是否支持播放.
     * @return
     */
    private boolean checkMediaSupport(String suffix) {
        String[] mediaTypes = ConfigConstants.getMedia();
        String type = suffix;
        for (String temp : mediaTypes) {
            if (type.equals(temp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将浏览器不兼容视频格式转换成MP4
     * @param fileAttribute
     * @param mediaPath 源视频绝对路径
     * @return 转码后视频绝对路径
     */
    private static String convertToMp4(FileAttribute fileAttribute, String mediaPath) {
        // 源文件
        String srcFilePath = mediaPath;
        // 目标文件
        String tgtFilePath = srcFilePath.substring(0, srcFilePath.lastIndexOf(".")) + ".mp4";

        File file = new File(srcFilePath);
        File desFile = new File(tgtFilePath);
        //判断一下防止穿透缓存
        if (desFile.exists()) {
            return tgtFilePath;
        }
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(file);
        Frame captured_frame = null;
        FFmpegFrameRecorder recorder = null;
        try {

            frameGrabber.start();
            recorder = new FFmpegFrameRecorder(tgtFilePath, frameGrabber.getImageWidth(),
                    frameGrabber.getImageHeight(), frameGrabber.getAudioChannels());
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); //avcodec.AV_CODEC_ID_H264  //AV_CODEC_ID_MPEG4
            recorder.setFormat("mp4");
            recorder.setFrameRate(frameGrabber.getFrameRate());
            //recorder.setSampleFormat(frameGrabber.getSampleFormat()); //
            recorder.setSampleRate(frameGrabber.getSampleRate());

            recorder.setAudioChannels(frameGrabber.getAudioChannels());
            recorder.setFrameRate(frameGrabber.getFrameRate());
            recorder.start();
            while ((captured_frame = frameGrabber.grabFrame()) != null) {
                try {
                    recorder.setTimestamp(frameGrabber.getTimestamp());
                    recorder.record(captured_frame);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (recorder != null) {
                try {
                    recorder.close();
                } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }

            if (frameGrabber != null) {
                try {
                    frameGrabber.close();
                } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return desFile.exists() ? tgtFilePath : null;
    }
}
