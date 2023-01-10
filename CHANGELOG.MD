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

## 2023.1.9

1. pdf转img调整使用icepdf扩展, pdfbox针对部分pdf文件会转换失败, 卡住不动, 类似问题: https://stackoom.com/question/2YHxZ
2. 对于ppt/pptx, 直接使用officePicture浏览方式
3. 去掉部分页面的知识星球提示
4. 首页和版本发布页 去掉相关交流链接, 有问题上官网
5. 演示首页的上传文件按更新时间降序, 新上传文件在前面