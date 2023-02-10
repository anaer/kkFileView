package cn.keking.service.impl;

import cn.hutool.core.codec.Base64Encoder;
import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.EncodingDetects;
import cn.keking.utils.KkFileUtils;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.util.HtmlUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by kl on 2018/1/17.
 * Content :处理文本文件
 */
@Service
public class SimTextFilePreviewImpl implements FilePreview {

    private final FileHandlerService fileHandlerService;
    private final OtherFilePreviewImpl otherFilePreview;

    public SimTextFilePreviewImpl(FileHandlerService fileHandlerService,OtherFilePreviewImpl otherFilePreview) {
        this.fileHandlerService = fileHandlerService;
        this.otherFilePreview = otherFilePreview;
    }
    private static final String FILE_DIR = ConfigConstants.getFileDir();
    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        String fileName = fileAttribute.getFileName();
        String uniqueKey = fileAttribute.getUniqueKey();
        String filePath = null;
        Boolean refresh = fileAttribute.getRefresh();

        // 是否使用缓存, 配置开启缓存 且 未传强制刷新参数 同时 文件预览过
        boolean useCache = fileHandlerService.isUseCache(uniqueKey, refresh);

        if(useCache) {
            filePath = fileHandlerService.getConvertedFile(uniqueKey);
        } else {
            ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute, fileName);
            if (response.isFailure()) {
                return otherFilePreview.notSupportedFile(model, fileAttribute, response.getMsg());
            }
            filePath = fileHandlerService.getRelativePath(response.getContent());
            if (ConfigConstants.isCacheEnabled()) {
                fileHandlerService.addConvertedFile(uniqueKey, filePath);  //加入缓存
            }
        }

        try {
            String fileData = HtmlUtils.htmlEscape(textData(filePath, fileName));
            String textData = Base64Encoder.encode(fileData, Charset.forName("utf-8"));
            model.addAttribute("textData", textData);
        } catch (IOException e) {
            return otherFilePreview.notSupportedFile(model, fileAttribute, e.getLocalizedMessage());
        }
        return TXT_FILE_PREVIEW_PAGE;
    }

    private String textData(String filePath,String fileName) throws IOException {
        String path = FILE_DIR + filePath;
        File file = new File(path);
        if (KkFileUtils.isIllegalFileName(fileName)) {
            return null;
        }
        if (!file.exists() || file.length() == 0) {
            return "";
        } else {
            String charset = EncodingDetects.getJavaEncode(path);
            if ("ASCII".equals(charset)) {
                charset = StandardCharsets.US_ASCII.name();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), charset));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line).append("\r\n");
            }
            br.close();
            return result.toString();
        }
    }
}
