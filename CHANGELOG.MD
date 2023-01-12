# TODO
1. 同一文件 不同链接 处理
2. 配置文件刷新调整

## 2023.1.12
1. 提取PPT浏览方式配置:`ppt.preview.page`, 取值:ppt-PPT方式,左侧有列表, officePicture-图片浏览模式 默认图片浏览模式
2. 配置刷新间隔调整为5秒, 原为1秒
3. 缓存调整, 根据md5(url)生成uniqueKey, 部分缓存存储时基于该uniqueKey进行存储.
   PS: 因原逻辑默认文件都在/file/一级目录下, 处理方式较为简单, 本次修改后支持自定义多级目录, 未全面测试
4. 下载文件使用上述的uniqueKey作为文件名, 解决同名文件覆盖问题
5. 压缩包下载时使用上述规则, 但是预览解压时解压到同名目录中, 目录结构与压缩包内结构一致, 保留压缩包内的文件命名
6. 对于压缩包内文件浏览, 根据fileKey判断不重新下载, 从url中剥离出文件路径进行处理. PS: 当前仅测试了文本与图片文件

## 2023.1.11
1. 代码库删除/server/libreoffice目录, 减少仓库体积, 因为本机有安装libreoffice(D:\LibreOffice), 有需要的话 添加软链接
```
mklink /D "D:\Git\kkFileView\server\libreoffice" "D:\LibreOffice"
```
2. 添加application.example.properties, 为原默认配置文件, 
   application.properties 针对本机环境 做些调整
3. 本地日志中文乱码, 修改配置applicatioin.properties, 添加日志编码配置
```conf
# 日志配置
logging.charset.console = GBK
```
4. 修改转换目录为日期目录, 便于文件管理 如: /file/2023/01/11/fileName.pdf
5. 添加levigo-jbig2-imageio依赖, icepdf有用到, 不加会提示: `Levigo JBIG2 image library was not found on classpath`
6. pdf2img调整, 取缓存时, 不判断具体文件是否存在, 因为生成过程中可能存在异常导致某一页生成失败, 如果判断是否存在, 则会每次重新生成, 影响效率
   因此需要保证缓存数据与/file/目录下文件 需要一致, 如果缓存存在, 而文件已删除, 则会导致预览页面破图
7. image图片保存时 异常捕获处理, 本机因安全工具的防护规则出现过以下异常, 因此捕获异常, 不因某一页的失败, 导致后续页面未生成 
    `java.io.FileNotFoundException: ...\360.jpg (拒绝访问。)`

8. 测试CAD文件预览时, icepdf如下异常:
    文件: `C:\Program Files (x86)\Microsoft Office\Office14\Visio Content\2052\BLOCKS.DWG`
    异常: `java.lang.IllegalArgumentException: Width (0) and height (3) cannot be <= 0`

    自定义Document继承org.icepdf.core.pobjects.Document. 重写getPageImage方法, 修改pageWidth赋值逻辑: 
   `int pageWidth = Math.max((int) sz.getWidth(), 1);`

   pdfbox貌似做了相应处理, 没这个问题.

9. 预览临时文件日期路径调整, CAD, SimText, Picture, Tiff, Media, Compress
10. TrustDirFilter中allowPreview预览 处理file:\\\三斜杠的情况 PS: 只测试了Windows的情况
11. WebUtils.decodeUrl Base64解码链接后, 进行URL解码
12. ConvertPicUtil.convertTif2Jpg 返回的图片url链接调整
13. 视频转码 文件下载逻辑调整
14. Tiff跳转fileNotSupprot页面逻辑调整

## 2023.1.10

1. /addTask 添加判断是否授信host, 非授信host不接受转码任务
2. .gitignore过滤server/libreoffice/, Linux下不需要该目录
3. server/pom.xml注释make assembly, 需要时可临时开启, 另dist-win32.xml将libreoffice一起打包时, 本机会打包失败, 使用时注释掉.

目录结构:
```
kkFileView-4.2.0
├── bin                             应用目录
│   ├── kkFileView-4.2.0.jar        应用包
│   ├── install.sh                  libreoffice安装脚本(首次执行安装)
│   ├── showlog.sh                  打印日志
│   ├── shutdown.sh                 停止程序脚本
│   └── startup.sh                  启动程序脚本
├── config                          配置目录
│   ├── application.properties      应用配置
│   └── freemarker_implicit.ftl
├── file                            文件目录(文件上传, 转码生成文件等)
└── log                             日志目录
    └── README.txt
```

4. 添加hutool依赖, 反正现在包已经289M+, 也不差这一个

## 2023.1.9

1. pdf转img调整使用icepdf扩展, pdfbox针对部分pdf文件会转换失败, 卡住不动, 类似问题: https://stackoom.com/question/2YHxZ
2. 对于ppt/pptx, 直接使用officePicture浏览方式
3. 去掉部分页面的知识星球提示
4. 首页和版本发布页 去掉相关交流链接, 有问题上官网
5. 演示首页的上传文件按更新时间降序, 新上传文件在前面