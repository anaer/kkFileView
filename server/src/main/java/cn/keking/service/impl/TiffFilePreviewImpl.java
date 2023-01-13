package cn.keking.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.utils.ConvertPicUtil;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.WebUtils;
import cn.keking.web.filter.BaseUrlFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * tiff 图片文件处理
 *
 * @author kl (http://kailing.pub)
 * @since 2021/2/8
 */
@Service
public class TiffFilePreviewImpl implements FilePreview {

    private final static Logger logger = LoggerFactory.getLogger(TiffFilePreviewImpl.class);

    private final FileHandlerService fileHandlerService;
    private final PictureFilePreviewImpl pictureFilePreview;
    private final OtherFilePreviewImpl otherFilePreview;

    private static final String INITIALIZE_MEMORY_SIZE = "initializeMemorySize";
    //默认初始化 50MB 内存
    private static final long INITIALIZE_MEMORY_SIZE_VALUE_DEFAULT = 1024L * 1024 * 50;

    public TiffFilePreviewImpl(FileHandlerService fileHandlerService,
            PictureFilePreviewImpl pictureFilePreview, OtherFilePreviewImpl otherFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.pictureFilePreview = pictureFilePreview;
        this.otherFilePreview = otherFilePreview;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        String tifPreviewType = ConfigConstants.getTifPreviewType();
        String tifOnLinePreviewType = fileAttribute.getTifPreviewType();
        String name = fileAttribute.getName();

        if (StringUtils.hasText(tifOnLinePreviewType)) {
            tifPreviewType = tifOnLinePreviewType;
        }

        // 因tif支持多种预览类型, 所以调整uniqueKey取值
        String uniqueKey = fileAttribute.getUniqueKey() + "_" + tifPreviewType;

        if ("tif".equalsIgnoreCase(tifPreviewType)) {
            pictureFilePreview.filePreviewHandle(url, model, fileAttribute);
            String fileSize = WebUtils.getUrlParameterReg(url, INITIALIZE_MEMORY_SIZE);
            if (StringUtils.hasText(fileSize)) {
                model.addAttribute(INITIALIZE_MEMORY_SIZE, fileSize);
            } else {
                model.addAttribute(INITIALIZE_MEMORY_SIZE,
                        Long.toString(INITIALIZE_MEMORY_SIZE_VALUE_DEFAULT));
            }
            return TIFF_FILE_PREVIEW_PAGE;

        } else if ("jpg".equalsIgnoreCase(tifPreviewType)) {
            // 将返回页面的图片url的list对象
            List<String> imagePaths = new ArrayList<>();
            if (ConfigConstants.isCacheEnabled() && fileHandlerService.isConvertedFile(uniqueKey)) {
                String cacheStr = fileHandlerService.getConvertedFile(uniqueKey);
                imagePaths = JSONUtil.toList(cacheStr, String.class);
            } else {
                ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, name);
                if (response.isFailure()) {
                    return otherFilePreview.notSupportedFile(model, fileAttribute,
                            response.getMsg());
                }
                String tifFilePath = response.getContent();

                // 修改扩展名 生成转换后的文件路径
                String jpgFilePath = tifFilePath.substring(0, tifFilePath.lastIndexOf("."))
                        + ".jpg";
                // 将tif转换为jpg，返回转换后的文件路径、文件名的list
                imagePaths = ConvertPicUtil.convertTif2Jpg(tifFilePath, jpgFilePath);

                if (ConfigConstants.isCacheEnabled() && CollUtil.isNotEmpty(imagePaths)) {
                    fileHandlerService.addConvertedFile(uniqueKey, JSONUtil.toJsonStr(imagePaths));
                }
            }

            String baseUrl = BaseUrlFilter.getBaseUrl();
            List<String> imageUrls = new ArrayList<>();
            // 循环，拼装url的list对象
            for (String strJpg : imagePaths) {
                imageUrls.add(baseUrl + StrUtil.replace(strJpg, "\\", "/"));

                model.addAttribute("imgUrls", imageUrls);
                model.addAttribute("currentUrl", imageUrls.get(0));

                return PICTURE_FILE_PREVIEW_PAGE;
            }
        } else if ("pdf".equalsIgnoreCase(tifPreviewType)) {
            String relativePath = null;
            if (ConfigConstants.isCacheEnabled() && fileHandlerService.isConvertedFile(uniqueKey)) {
                relativePath = fileHandlerService.getConvertedFile(uniqueKey);
            } else {
                ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, name);
                if (response.isFailure()) {
                    return otherFilePreview.notSupportedFile(model, fileAttribute,
                            response.getMsg());
                }
                String tifFilePath = response.getContent();

                // 以PDF模式预览的过程
                String pdfFilePath = tifFilePath.substring(0, tifFilePath.lastIndexOf("."))
                        + ".pdf";
                File filePdf = ConvertPicUtil.convertTif2Pdf(tifFilePath, pdfFilePath);

                // 如果pdf已经存在，则将url路径加入到对象中，返回给页面
                if (filePdf != null && filePdf.exists()) {
                    relativePath = fileHandlerService.getRelativePath(pdfFilePath);

                    if(ConfigConstants.isCacheEnabled()) {
                        fileHandlerService.addConvertedFile(uniqueKey, relativePath);
                    }
                }
            }

            if (StrUtil.isNotBlank(relativePath)) {
                String baseUrl = BaseUrlFilter.getBaseUrl();
                String pdfUrl = baseUrl + StrUtil.replace(relativePath, "\\", "/");
                model.addAttribute("pdfUrl", pdfUrl);
                return PDF_FILE_PREVIEW_PAGE;
            }
        }

        return otherFilePreview.notSupportedFile(model, fileAttribute, "暂不支持");
    }
}
