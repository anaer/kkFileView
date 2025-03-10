package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.DownloadUtils;
import cn.keking.service.FileHandlerService;
import cn.keking.service.CompressFileReader;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

/**
 * Created by kl on 2018/1/17.
 * Content :处理压缩包文件
 */
@Service
public class CompressFilePreviewImpl implements FilePreview {

    private final FileHandlerService fileHandlerService;
    private final CompressFileReader compressFileReader;
    private final OtherFilePreviewImpl otherFilePreview;

    public CompressFilePreviewImpl(FileHandlerService fileHandlerService, CompressFileReader compressFileReader, OtherFilePreviewImpl otherFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.compressFileReader = compressFileReader;
        this.otherFilePreview = otherFilePreview;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        String fileName = fileAttribute.getFileName();
        String uniqueKey = fileAttribute.getUniqueKey();
        Boolean refresh = fileAttribute.getRefresh();

        String fileTree;

        // 是否使用缓存, 配置开启缓存 且 未传强制刷新参数 同时 文件预览过
        boolean useCache = fileHandlerService.isUseCache(uniqueKey, refresh);

        if(useCache) {
            fileTree = fileHandlerService.getConvertedFile(uniqueKey);
        } else {
            ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
            if (response.isFailure()) {
                return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
            }
            String filePath = response.getContent();
            fileTree = compressFileReader.unRar(filePath, uniqueKey);

            if(ConfigConstants.isCacheEnabled()) {
                fileHandlerService.addConvertedFile(uniqueKey, fileTree);
            }
        }
        if (fileTree != null && !"null".equals(fileTree)) {
            model.addAttribute("fileTree", fileTree);
            return COMPRESS_FILE_PREVIEW_PAGE;
        } else {
            return otherFilePreview.notSupportedFile(model, fileAttribute, "压缩文件类型不受支持");
        }
    }
}
