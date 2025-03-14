package cn.keking.service;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.service.cache.CacheService;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.WebUtils;
import com.aspose.cad.Color;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import cn.keking.icepdf.Document;
import org.icepdf.core.util.GraphicsRenderingHints;

/**
 * @author yudian-it
 * @date 2017/11/13
 */
@Component
public class FileHandlerService {

    private final Logger logger = LoggerFactory.getLogger(FileHandlerService.class);

    private static final String DEFAULT_CONVERTER_CHARSET = System.getProperty("sun.jnu.encoding");
    private final String fileDir = ConfigConstants.getFileDir();
    private final CacheService cacheService;

    @Value("${server.tomcat.uri-encoding:UTF-8}")
    private String uriEncoding;

    public FileHandlerService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * @return 已转换过的文件集合(缓存)
     */
    public Map<String, String> listConvertedFiles() {
        return cacheService.getPDFCache();
    }

    /**
     * 是否是已转换过的文件
     * @param key
     * @return
     */
    private boolean isConvertedFile(String key){
        return cacheService.getPDFCache().containsKey(key);
    }

    /**
     * 判断是否使用缓存
     * @param uniqueKey 唯一key
     * @param refresh 是否强制刷新
     * @return 是否使用缓存
     */
    public boolean isUseCache(String uniqueKey, Boolean refresh) {
        boolean useCache = ConfigConstants.isCacheEnabled() && (!BooleanUtil.isTrue(refresh) && isConvertedFile(uniqueKey));
        if(useCache) {
            logger.info("converted file cache hit:{}", uniqueKey);
        }

        return useCache;
    }

    /**
     * @return 已转换过的文件
     */
    public String getConvertedFile(String key) {
        String path = cacheService.getPDFCache(key);
        // 如果是压缩包, 缓存的是文件树, 内容有点长 所以设置限制长度100
        logger.info("cache file:{} {}", key, StrUtil.maxLength(path, 100));
        return path;
    }

    /**
     * @param key pdf本地路径
     * @return pdf图片数量
     */
    public Integer getConvertedPdfImage(String key) {
        return cacheService.getPdfImageCache(key);
    }


    /**
     * 从路径中获取文件负
     *
     * @param path 类似这种：C:\Users\yudian-it\Downloads
     * @return 文件名
     */
    public String getFileNameFromPath(String path) {
        return path.substring(path.lastIndexOf(File.separator) + 1);
    }

    /**
     * 获取相对路径
     *
     * @param absolutePath 绝对路径
     * @return 相对路径
     */
    public String getRelativePath(String absolutePath) {
        String relativePath = absolutePath.substring(fileDir.length());
        while(StrUtil.startWith(relativePath, File.separator)) {
            relativePath = StrUtil.removePrefix(relativePath, File.separator);
        }
        return relativePath;
    }

    /**
     * 添加转换后PDF缓存
     *
     * @param uniqueKey 唯一key
     * @param value    缓存相对路径
     */
    public void addConvertedFile(String uniqueKey, String value) {
        cacheService.putPDFCache(uniqueKey, value);
    }

    /**
     * 添加转换后图片数缓存
     *
     * @param uniqueKey 唯一key
     * @param num         图片张数
     */
    public void addConvertedPdfImage(String uniqueKey, int num) {
        cacheService.putPdfImageCache(uniqueKey, num);
    }

    /**
     * 获取redis中压缩包内图片文件
     *
     * @param uniqueKey 唯一key
     * @return 图片文件访问url列表
     */
    public List<String> getImgCache(String uniqueKey) {
        return cacheService.getImgCache(uniqueKey);
    }

    /**
     * 设置redis中压缩包内图片文件
     *
     * @param uniqueKey 唯一key
     * @param imgs    图片文件访问url列表
     */
    public void putImgCache(String uniqueKey, List<String> imgs) {
        cacheService.putImgCache(uniqueKey, imgs);
    }

    /**
     * 对转换后的文件进行操作(改变编码方式)
     *
     * @param outFilePath 文件绝对路径
     */
    public void doActionConvertedFile(String outFilePath) {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(outFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CONVERTER_CHARSET))) {
            String line;
            while (null != (line = reader.readLine())) {
                if (line.contains("charset=gb2312")) {
                    line = line.replace("charset=gb2312", "charset=utf-8");
                }
                sb.append(line);
            }
            // 添加sheet控制头
            sb.append("<script src=\"js/jquery-3.6.1.min.js\" type=\"text/javascript\"></script>");
            sb.append("<script src=\"js/excel.header.js\" type=\"text/javascript\"></script>");
            sb.append("<link rel=\"stylesheet\" href=\"bootstrap/css/xlsx.css\">");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 重新写入文件
        try (FileOutputStream fos = new FileOutputStream(outFilePath);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  pdf文件转换成jpg图片集
     * @param pdfFilePath pdf文件路径
     * @param pdfName pdf文件名称
     * @param baseUrl 基础访问地址
     * @return 图片访问集合
     */
    public List<String> pdf2jpg(String pdfFilePath, String uniqueKey, String baseUrl, boolean useCache) {
        List<String> imageUrls = new ArrayList<>();
        String imageFileSuffix = ".jpg";
        String urlPrefix = baseUrl;

        int index = pdfFilePath.lastIndexOf(".");
        String folder = pdfFilePath.substring(0, index);

        File path = new File(folder);

        if(useCache) {
            Integer imageCount = this.getConvertedPdfImage(uniqueKey);
            logger.info("pdf:{} imageCount:{}", pdfFilePath, imageCount);
            if (imageCount != null && imageCount > 0) {
                String imageFilePath;
                for (int i = 0; i < imageCount; i++) {
                    imageFilePath = folder + File.separator + i + imageFileSuffix;
                    String relativePath = KkFileUtils.getUrlRelativePath(imageFilePath);
                    imageUrls.add(urlPrefix + relativePath);
                }
                return imageUrls;
            }
        }

        float scale = 1f;//缩放比例
        float rotation = 0f;//旋转角度
        Document doc = new Document();
        try {
            doc.setFile(pdfFilePath);
            int pageCount = doc.getNumberOfPages();
            logger.info("pdf:{} pageCount:{}", pdfFilePath, pageCount);

            if (!path.exists() && !path.mkdirs()) {
                logger.error("创建转换文件【{}】目录失败，请检查目录权限！", folder);
            }

            String imageFilePath;
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                imageFilePath = folder + File.separator + pageIndex + imageFileSuffix;
                try {
                    BufferedImage image = (BufferedImage) doc.getPageImage(pageIndex, GraphicsRenderingHints.SCREEN, org.icepdf.core.pobjects.Page.BOUNDARY_CROPBOX, rotation, scale);
                    ImageIOUtil.writeImage(image, imageFilePath, 105);
                    imageUrls.add(urlPrefix + KkFileUtils.getUrlRelativePath(imageFilePath));
                    image.flush();
                } catch (Exception e){
                    // 本机因安全工具的防护规则出现过以下异常, 因此对此类异常 跳过继续处理
                    // java.io.FileNotFoundException: ...\360.jpg (拒绝访问。)
                    logger.error("生成图片异常:{}", imageFilePath, e);
                }
            }
            this.addConvertedPdfImage(uniqueKey, pageCount);
        } catch (IOException | PDFException | PDFSecurityException e) {
            logger.error("Convert pdf to jpg exception, pdfFilePath：{}", pdfFilePath, e);
        } finally {
            if(doc != null){
                doc.dispose();
            }
        }

        // logger.info("pdf img urls:{}", imageUrls);

        return imageUrls;
    }

    /**
     * cad文件转pdf
     * @param inputFilePath cad文件路径
     * @param outputFilePath pdf输出文件路径
     * @return 转换是否成功
     */
    public boolean cadToPdf(String inputFilePath, String outputFilePath)  {
        com.aspose.cad.Image cadImage = com.aspose.cad.Image.load(inputFilePath);
        CadRasterizationOptions cadRasterizationOptions = new CadRasterizationOptions();
        cadRasterizationOptions.setLayouts(new String[]{"Model"});
        cadRasterizationOptions.setNoScaling(true);
        cadRasterizationOptions.setBackgroundColor(Color.getWhite());
        cadRasterizationOptions.setPageWidth(cadImage.getWidth());
        cadRasterizationOptions.setPageHeight(cadImage.getHeight());
        cadRasterizationOptions.setPdfProductLocation("center");
        cadRasterizationOptions.setAutomaticLayoutsScaling(true);
        cadRasterizationOptions.setDrawType(CadDrawTypeMode.UseObjectColor);
        PdfOptions pdfOptions = new PdfOptions();
        pdfOptions.setVectorRasterizationOptions(cadRasterizationOptions);
        File outputFile = new File(outputFilePath);
        OutputStream stream;
        try {
            stream = new FileOutputStream(outputFile);
            cadImage.save(stream, pdfOptions);
            cadImage.close();
            return true;
        } catch (FileNotFoundException e) {
            logger.error("PDFFileNotFoundException，inputFilePath：{}", inputFilePath, e);
            return false;
        }
    }

    /**
     * 获取文件属性
     *
     * @param url url
     * @return 文件属性
     */
    public FileAttribute getFileAttribute(String url, HttpServletRequest req) {
        FileAttribute attribute = new FileAttribute();
        String suffix;
        FileType type;
        String name;
        String fullFileName = WebUtils.getUrlParameterReg(url, "fullfilename");
        if (StringUtils.hasText(fullFileName)) {
            name = fullFileName;
        } else {
            name = WebUtils.getFileNameFromURL(url);
        }
        type = FileType.typeFromFileName(name);
        suffix = KkFileUtils.suffixFromFileName(name);

        boolean isCompress = url.contains("?fileKey=");
        if (isCompress) {
            attribute.setSkipDownLoad(true);
        }
        String  urlStrr = url.toLowerCase();  //转换为小写对比
        boolean wjl = WebUtils.kuayu("&fullfilename=", urlStrr);  //判断是否启用文件流
        if(wjl){
            url =  url.substring(0,url.lastIndexOf("&"));  //删除添加的文件流内容
        }
        url = WebUtils.encodeUrlFileName(url);
        name =  KkFileUtils.htmlEscape(name);  //文件名处理
        String uniqueKey = SecureUtil.md5(url);
        String fileName = StrUtil.isNotBlank(suffix) ? uniqueKey + "." + suffix : uniqueKey;
        attribute.setType(type);
        attribute.setName(name);
        attribute.setFileName(fileName);

        // 如果是压缩包文件 则不修改文件名
        if(isCompress) {
            attribute.setFileName(name);
        }

        attribute.setSuffix(suffix);
        attribute.setUrl(url);
        attribute.setUniqueKey(uniqueKey);
        if (req != null) {
            String officePreviewType = req.getParameter("officePreviewType");
            String fileKey = WebUtils.getUrlParameterReg(url,"fileKey");
            if (StringUtils.hasText(officePreviewType)) {
                attribute.setOfficePreviewType(officePreviewType);
            }
            if (StringUtils.hasText(fileKey)) {
                attribute.setFileKey(fileKey);
            }

            String tifPreviewType = req.getParameter("tifPreviewType");
            if (StringUtils.hasText(tifPreviewType)) {
                attribute.setTifPreviewType(tifPreviewType);
            }

            String filePassword = req.getParameter("filePassword");
            if (StringUtils.hasText(filePassword)) {
                attribute.setFilePassword(filePassword);
            }

            String userToken = req.getParameter("userToken");
            if (StringUtils.hasText(userToken)) {
                attribute.setUserToken(userToken);
            }

            String refresh = req.getParameter("refresh");
            if (StringUtils.hasText(refresh)) {
                attribute.setRefresh(BooleanUtil.toBoolean(refresh));
            }
        }

        return attribute;
    }

    /**
     * @return 已转换过的视频文件集合(缓存)
     */
    public Map<String, String> listConvertedMedias() {
        return cacheService.getMediaConvertCache();
    }

    /**
     * 添加转换后的视频文件缓存
     * @param fileName
     * @param value
     */
    public void addConvertedMedias(String fileName, String value) {
        cacheService.putMediaConvertCache(fileName, value);
    }

    /**
     * @return 已转换视频文件缓存，根据文件名获取
     */
    public String getConvertedMedias(String key) {
        return cacheService.getMediaConvertCache(key);
    }

}
