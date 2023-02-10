package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.KkFileUtils;
import cn.keking.service.FileHandlerService;
import cn.keking.web.filter.BaseUrlFilter;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import static cn.keking.service.impl.OfficeFilePreviewImpl.getPreviewType;

/**
 * @author chenjh
 * @since 2019/11/21 14:28
 */
@Service
public class CadFilePreviewImpl implements FilePreview {

    private static final String OFFICE_PREVIEW_TYPE_IMAGE = "image";
    private static final String OFFICE_PREVIEW_TYPE_ALL_IMAGES = "allImages";
    private static final String FILE_DIR = ConfigConstants.getFileDir();

    private final FileHandlerService fileHandlerService;
    private final OtherFilePreviewImpl otherFilePreview;

    public CadFilePreviewImpl(FileHandlerService fileHandlerService, OtherFilePreviewImpl otherFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.otherFilePreview = otherFilePreview;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        // 预览Type，参数传了就取参数的，没传取系统默认
        String officePreviewType = fileAttribute.getOfficePreviewType() == null ? ConfigConstants.getOfficePreviewType() : fileAttribute.getOfficePreviewType();
        String baseUrl = BaseUrlFilter.getBaseUrl();
        String uniqueKey = fileAttribute.getUniqueKey();
        // 下载时 使用uniqueKey生成文件名
        String fileName = uniqueKey + "." + fileAttribute.getSuffix();
        String pdfName = uniqueKey + ".pdf";
        String outFilePath = null;

        Boolean refresh = fileAttribute.getRefresh();

        // 是否使用缓存, 配置开启缓存 且 未传强制刷新参数 同时 文件预览过
        boolean useCache = fileHandlerService.isUseCache(uniqueKey, refresh);

        // 判断之前是否已转换过，如果转换过，直接返回，否则执行转换
        if(useCache) {
            outFilePath = fileHandlerService.getConvertedFile(uniqueKey);
        } else {
            ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
            if (response.isFailure()) {
                return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
            }
            String filePath = response.getContent();
            outFilePath = KkFileUtils.getDateDir(FILE_DIR, pdfName);
            boolean convertResult = fileHandlerService.cadToPdf(filePath, outFilePath);
            if (!convertResult) {
                return otherFilePreview.notSupportedFile(model, fileAttribute, "cad文件转换异常，请联系管理员");
            }
            if (ConfigConstants.isCacheEnabled()) {
                fileHandlerService.addConvertedFile(uniqueKey, fileHandlerService.getRelativePath(outFilePath));
            }
        }
        if (baseUrl != null && (OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) || OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType))) {
            return getPreviewType(model, fileAttribute, officePreviewType, baseUrl, outFilePath, fileHandlerService, OFFICE_PREVIEW_TYPE_IMAGE,otherFilePreview);
        }
        model.addAttribute("pdfUrl", KkFileUtils.getUrlRelativePath(outFilePath));
        return PDF_FILE_PREVIEW_PAGE;
    }


}
