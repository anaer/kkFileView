package cn.keking.model;

import cn.keking.config.ConfigConstants;

/**
 * Created by kl on 2018/1/17.
 * Content :
 */
public class FileAttribute {

    private FileType type;
    private String suffix;
    /**
     * 原始文件名
     */
    private String name;
    /**
     * 根据uniqueKey生成的新文件名
     */
    private String fileName;
    private String url;
    private String fileKey;
    private String filePassword;
    private String userToken;
    private String officePreviewType = ConfigConstants.getOfficePreviewType();
    private String tifPreviewType;
    private Boolean skipDownLoad = false;
    private String uniqueKey;
    /**
     * 是否更新缓存, 即不使用缓存 重新预览
     */
    private Boolean refresh = false;

    public FileAttribute() {
    }

    public FileAttribute(FileType type, String suffix, String name, String url) {
        this.type = type;
        this.suffix = suffix;
        this.name = name;
        this.url = url;
    }

    public FileAttribute(FileType type, String suffix, String name, String url, String officePreviewType) {
        this.type = type;
        this.suffix = suffix;
        this.name = name;
        this.url = url;
        this.officePreviewType = officePreviewType;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getFilePassword() {
        return filePassword;
    }

    public void setFilePassword(String filePassword) {
        this.filePassword = filePassword;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getOfficePreviewType() {
        return officePreviewType;
    }

    public void setOfficePreviewType(String officePreviewType) {
        this.officePreviewType = officePreviewType;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getSkipDownLoad() {
        return skipDownLoad;
    }

    public void setSkipDownLoad(Boolean skipDownLoad) {
        this.skipDownLoad = skipDownLoad;
    }

    public String getTifPreviewType() {
        return tifPreviewType;
    }

    public void setTifPreviewType(String previewType) {
        this.tifPreviewType = previewType;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Boolean getRefresh() {
        return refresh;
    }

    public void setRefresh(Boolean refresh) {
        this.refresh = refresh;
    }
     
}
