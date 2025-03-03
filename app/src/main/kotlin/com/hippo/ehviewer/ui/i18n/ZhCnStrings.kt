package com.hippo.ehviewer.ui.i18n

object ZhCnStrings : Strings by EnStrings {
    override val homepage = "主页"
    override val subscription = "订阅"
    override val whatsHot = "热门"
    override val favourite = "收藏"
    override val history = "历史"
    override val downloads = "下载"
    override val settings = "设置"
    override val username = "用户名"
    override val password = "密码"
    override val signIn = "登录"
    override val register = "注册"
    override val signInViaWebview = "通过网页登录"
    override val signInFirst = "请先登录"
    override val textIsEmpty = "文本为空"
    override val waring = "警告"
    override val invalidDownloadLocation = "似乎下载路径不可用。请到重新设置下载路径。"
    override val clipboardGalleryUrlSnackMessage = "剪切板里有画廊链接"
    override val clipboardGalleryUrlSnackAction = "查看"
    override val errorTimeout = "超时"
    override val errorUnknownHost = "未知主机"
    override val errorRedirection = "太多重定向"
    override val errorSocket = "网络错误"
    override val errorUnknown = "奇怪的错误"
    override val errorCantFindActivity = "找不到相应的应用"
    override val errorCannotParseTheUrl = "无法解析链接"
    override val errorDecodingFailed = "解码失败"
    override val errorReadingFailed = "读取失败"
    override val errorOutOfRange = "越界"
    override val errorParseError = "解析失败"
    override val error509 = "509"
    override val errorInvalidUrl = "无效链接"
    override val errorGetPtokenError = "获取 pToken 错误"
    override val errorCantSaveImage = "无法保存图片"
    override val errorInvalidNumber = "非法数字"
    override val appWaring = "本应用中内容来自互联网，部分内容可能对您的生理及心理造成难以恢复的伤害。本应用作者不会对由本应用造成的任何后果负责。未成年人应在监护人指导下使用本应用。请遵守所在地的法律法规。"
    override val appWaring2 = "继续使用即代表您同意上述条款。"
    override val errorUsernameCannotEmpty = "用户名不可为空"
    override val errorPasswordCannotEmpty = "密码不可为空"
    override val guestMode = "游客模式"
    override val signInFailed = "登录失败"
    override val signInFailedTip = { a: String -> "若持续出错，请尝试“$a”。" }
    override val getIt = "知道了"
    override val galleryListSearchBarHintExhentai = "搜索 ExHentai"
    override val galleryListSearchBarHintEHentai = "搜索 E-Hentai"
    override val galleryListSearchBarOpenGallery = "打开画廊"
    override val galleryListEmptyHit = "什么都没有找到"
    override val galleryListEmptyHitSubscription = "请至 设置->EH->我的标签 订阅标签"
    override val keywordSearch = "关键字搜索"
    override val imageSearch = "图片搜索"
    override val searchImage = "图片搜索"
    override val searchSh = "已删除"
    override val searchSto = "有种子"
    override val searchSr = "最低评分"
    override val searchSpTo = "到"
    override val searchSpErr1 = "页数最大值至少为 10"
    override val searchSpErr2 = "页数范围过窄"
    override val searchSf = "禁用排除项："
    override val searchSfl = "语言"
    override val searchSfu = "上传者"
    override val searchSft = "标签"
    override val selectImage = "选择图片"
    override val selectImageFirst = "请先选择图片"
    override val addToFavourites = "收藏"
    override val removeFromFavourites = "移除收藏"
    override val deleteDownloads = "删除下载"
    override val quickSearch = "快速搜索"
    override val quickSearchTip = "点击“+”来添加快速搜索"
    override val addQuickSearchDialogTitle = "添加快速搜索"
    override val nameIsEmpty = "名称为空"
    override val translateTagForTagger = "使用翻译标签"
    override val delete = "删除"
    override val addQuickSearchTip = "画廊列表的状态将被保存为快速搜索。如果你想保存搜索面板的状态，请先执行搜索。"
    override val readme = "读我"
    override val imageSearchNotQuickSearch = "无法添加图片搜索为快速搜索"
    override val duplicateQuickSearch = { a: String -> "已存在相同的快速搜索，名称为“$a”。" }
    override val duplicateName = "已存在相同名称"
    override val saveProgress = "保存进度"
    override val deleteQuickSearch = { a: String -> "删除快速搜索“$a”？" }
    override val goToHint = { a: Int, b: Int -> "第 $a 页，共 $b 页" }
    override val any = "不限"
    override val star2 = "2 星"
    override val star3 = "3 星"
    override val star4 = "4 星"
    override val star5 = "5 星"
    override val download = "下载"
    override val read = "阅读"
    override val favoredTimes = { a: Int -> "\u2665 $a" }
    override val ratingText = { a: String, b: Float, c: Int -> "%s (%.2f, %d)".format(a, b, c) }
    override val torrentCount = { a: Int -> "种子 ($a)" }
    override val share = "分享"
    override val rate = "评分"
    override val similarGallery = "相似画廊"
    override val searchCover = "搜索封面"
    override val noTags = "暂无标签"
    override val noComments = "暂无评论"
    override val noMoreComments = "已显示所有评论"
    override val moreComment = "查看更多评论"
    override val refresh = "刷新"
    override val viewOriginal = "查看原图"
    override val openInOtherApp = "在其他应用中打开"
    override val clearImageCache = "清除图片缓存"
    override val clearImageCacheConfirm = "清除该画廊的所有图片缓存？"
    override val imageCacheCleared = "已清除图片缓存"
    override val rateSuccessfully = "评分成功"
    override val rateFailed = "评分失败"
    override val noTorrents = "没有种子"
    override val torrents = "种子"
    override val notFavorited = "未收藏"
    override val addFavoritesDialogTitle = "添加收藏"
    override val addToFavoriteSuccess = "已添加至收藏"
    override val removeFromFavoriteSuccess = "移除收藏"
    override val addToFavoriteFailure = "添加收藏失败"
    override val removeFromFavoriteFailure = "移除收藏失败"
    override val filterTheUploader = { a: String -> "屏蔽上传者“$a”？" }
    override val filterTheTag = { a: String -> "屏蔽标签“$a”？" }
    override val filterAdded = "已添加屏蔽项"
    override val newerVersionAvailable = "此画廊有新版本可用。"
    override val newerVersionTitle = { a: String, b: String -> "$a, 添加于 $b" }
    override val rating10 = "根本把持不住"
    override val rating9 = "好极了"
    override val rating8 = "很棒"
    override val rating7 = "不错"
    override val rating6 = "还行"
    override val rating5 = "一般般"
    override val rating4 = "不行"
    override val rating3 = "糟糕"
    override val rating2 = "瞎眼"
    override val rating1 = "快要窒息了"
    override val rating0 = "扑街了"
    override val galleryInfo = "画廊信息"
    override val copiedToClipboard = "已复制到剪切板"
    override val keyGid = "GID"
    override val keyToken = "Token"
    override val keyUrl = "链接"
    override val keyTitle = "标题"
    override val keyTitleJpn = "日文标题"
    override val keyThumb = "缩略图"
    override val keyCategory = "分类"
    override val keyUploader = "上传者"
    override val keyPosted = "上传时间"
    override val keyParent = "父画廊"
    override val keyVisible = "可见性"
    override val keyLanguage = "语言"
    override val keyPages = "页数"
    override val keySize = "大小"
    override val keyFavoriteCount = "收藏次数"
    override val keyFavorited = "收藏"
    override val keyRatingCount = "评价次数"
    override val keyRating = "评分"
    override val keyTorrents = "种子个数"
    override val keyTorrentUrl = "种子链接"
    override val galleryComments = "画廊评论"
    override val commentSuccessfully = "评论成功"
    override val commentFailed = "评论失败"
    override val copyCommentText = "复制评论文字"
    override val blockCommenter = "屏蔽评论者"
    override val filterTheCommenter = { a: String -> "屏蔽评论者“$a”？" }
    override val editComment = "修改评论"
    override val editCommentSuccessfully = "评论已被修改"
    override val editCommentFailed = "修改评论失败"
    override val voteUp = "深表赞同"
    override val cancelVoteUp = "不再深表赞同"
    override val voteDown = "垃圾评论"
    override val cancelVoteDown = "不是垃圾评论"
    override val voteUpSuccessfully = "已深表赞同"
    override val cancelVoteUpSuccessfully = "已不再深表赞同"
    override val voteDownSuccessfully = "已钦定为垃圾评论"
    override val cancelVoteDownSuccessfully = "已不再钦定为垃圾评论"
    override val voteFailed = "投票失败"
    override val checkVoteStatus = "查看投票情况"
    override val clickMoreComments = "点击加载更多评论"
    override val lastEdited = { a: String -> "上次修改时间：$a" }
    override val goTo = "跳页"
    override val sceneDownloadTitle = { a: String -> "下载 - $a" }
    override val noDownloadInfo = "这里是下载项目"
    override val downloadStateNone = "未启动"
    override val downloadStateWait = "等待中"
    override val downloadStateDownloading = "下载中"
    override val downloadStateDownloaded = "已下载"
    override val downloadStateFailed = "失败"
    override val downloadStateFailed2 = { a: Int -> "$a 未完成" }
    override val downloadStateFinish = "已完成"
    override val stat509AlertTitle = "509 警告"
    override val stat509AlertText = "图片配额已用尽。请停止下载，休息一下。"
    override val statDownloadDoneTitle = "下载结束"
    override val statDownloadDoneTextSucceeded = { a: Int -> "$a 项下载成功" }
    override val statDownloadDoneTextFailed = { a: Int -> "$a 项下载失败" }
    override val statDownloadDoneTextMix = { a: Int, b: Int -> "$a 项下载成功，$b 项下载失败" }
    override val statDownloadDoneLineSucceeded = { a: String -> "下载成功：$a" }
    override val statDownloadDoneLineFailed = { a: String -> "下载失败：$a" }
    override val downloadRemoveDialogTitle = "移除下载项"
    override val downloadRemoveDialogMessage = { a: String -> "从下载列表移除 $a ？" }
    override val downloadRemoveDialogMessage2 = { a: Int -> "从下载列表移除 $a 项？" }
    override val downloadRemoveDialogCheckText = "删除图片文件"
    override val statDownloadActionStopAll = "全部停止"
    override val defaultDownloadLabelName = "默认"
    override val downloadMoveDialogTitle = "移动"
    override val downloadLabels = "下载标签"
    override val downloadStartAll = "全部开始"
    override val downloadStopAll = "全部停止"
    override val downloadResetReadingProgress = "重置阅读进度"
    override val resetReadingProgressMessage = "重置所有已下载画廊的阅读进度？"
    override val downloadServiceLabel = "EhViewer 下载服务"
    override val downloadSpeedText = { a: String -> a }
    override val downloadSpeedText2 = { a: String, b: String -> "$a，剩余 $b" }
    override val rememberDownloadLabel = "记住下载标签"
    override val defaultDownloadLabel = "默认下载标签"
    override val addedToDownloadList = "已添加至下载列表"
    override val selectGroupingMode = "选择分组依据"
    override val selectGroupingModeCustom = "自定义标签"
    override val selectGroupingModeArtist = "作者"
    override val unknownArtists = "未知"
    override val add = "添加"
    override val newLabelTitle = "新标签"
    override val labelTextIsEmpty = "标签文本为空"
    override val labelTextIsInvalid = "“默认”是无效标签"
    override val labelTextExist = "标签已存在"
    override val renameLabelTitle = "重命名标签"
    override val deleteLabel = { a: String -> "删除标签“$a”？" }
    override val noHistory = "这里是阅读历史"
    override val clearAll = "全部清除"
    override val clearAllHistory = "清除所有阅读历史？"
    override val filter = "屏蔽列表"
    override val filterTitle = "标题"
    override val filterUploader = "上传者"
    override val filterTag = "标签"
    override val filterTagNamespace = "标签组"
    override val filterCommenter = "评论者"
    override val filterComment = "评论"
    override val deleteFilter = { a: String -> "删除屏蔽项“$a”？" }
    override val addFilter = "添加屏蔽项"
    override val showDefinition = "查看定义"
    override val filterText = "屏蔽项文本"
    override val filterTip = "该屏蔽系统会在 EHentai 网站屏蔽系统的基础上继续屏蔽画廊。\n\n标题屏蔽项：排除标题含有该关键字的画廊。\n\n上传者屏蔽项：排除该上传者上传的画廊。\n\n标签屏蔽项：排除包含该标签的画廊，这会使获取画廊列表花费更多时间。\n\n标签组屏蔽项：排除包含该标签组的画廊，这会使获取画廊列表花费更多时间。\n\n评论者屏蔽项：排除该评论者发布的评论。\n\n评论屏蔽项：排除匹配该正则表达式的评论。"
    override val uConfig = "EHentai 设置"
    override val applyTip = "点击右上角的对勾来保存设置"
    override val myTags = "我的标签"
    override val shareImage = "分享图片"
    override val imageSaved = { a: String -> "图片已保存至 $a" }
    override val settingsEh = "EH"
    override val settingsEhSignOut = "退出登录"
    override val settingsEhIdentityCookiesSigned = "身份 Cookie 可用于登录该账号。<br><b>注意数据安全</b>"
    override val settingsEhIdentityCookiesGuest = "游客模式"
    override val settingsEhClearIgneous = "清除 igneous"
    override val settingsUConfig = "EHentai 设置"
    override val settingsUConfigSummary = "EHentai 网站上的设置"
    override val settingsMyTags = "我的标签"
    override val settingsMyTagsSummary = "EHentai 网站上的我的标签"
    override val settingsEhGallerySite = "画廊站点"
    override val settingsEhLaunchPage = "启动页"
    override val settingsEhListMode = "列表模式"
    override val settingsEhListModeDetail = "详情"
    override val settingsEhListModeThumb = "缩略图"
    override val settingsEhDetailSize = "详情宽度"
    override val settingsEhDetailSizeLong = "长"
    override val settingsEhDetailSizeShort = "短"
    override val settingsEhThumbColumns = "缩略图列数"
    override val settingsEhForceEhThumb = "使用 e-hentai 缩略图服务器"
    override val settingsEhForceEhThumbSummary = "若缩略图加载失败可尝试关闭此项"
    override val settingsEhShowJpnTitle = "显示日文标题"
    override val settingsEhShowJpnTitleSummary = "需同时在 EHentai 网站设置中启用 Japanese Title"
    override val settingsEhShowGalleryPages = "显示画廊页数"
    override val settingsEhShowGalleryPagesSummary = "在画廊列表中显示页数"
    override val settingsEhShowVoteStatus = "显示标签投票状态"
    override val settingsEhShowGalleryComments = "显示画廊评论"
    override val settingsEhShowGalleryCommentsSummary = "在画廊详情页中显示评论"
    override val settingsEhShowGalleryCommentThreshold = "评论分数阈值"
    override val settingsEhShowGalleryCommentThresholdSummary = "隐藏低于或等于此分数的评论（-101 禁用）"
    override val settingsEhShowTagTranslations = "显示标签翻译"
    override val settingsEhShowTagTranslationsSummary = "显示翻译后的标签而非原始文字（需花费时间来下载数据文件）"
    override val settingsEhTagTranslationsSource = "补充翻译（由 EhTagTranslation 提供）"
    override val settingsEhTagTranslationsSourceUrl = "https://github.com/EhTagTranslation/Editor/wiki"
    override val settingsEhFilter = "屏蔽列表"
    override val settingsEhFilterSummary = "根据标题、上传者、标签、评论者屏蔽画廊或评论"
    override val settingsReadReverseControls = "反转物理按键控制"
    override val settingsBlockExtraneousAds = "[实验性] 屏蔽外部广告"
    override val settingsAdsPlaceholder = "[可选] 选择替换外部广告的图片"
    override val settingsDownload = "下载"
    override val settingsDownloadDownloadLocation = "下载路径"
    override val settingsDownloadCantGetDownloadLocation = "无法获取下载路径"
    override val settingsDownloadConcurrency = "并发下载数"
    override val settingsDownloadConcurrencySummary = { a: Int -> "最多同时下载 $a 张图片" }
    override val settingsDownloadPreloadImage = "预载图片"
    override val settingsDownloadPreloadImageSummary = { a: Int -> "向后预载 $a 张图片" }
    override val settingsDownloadDownloadOriginImage = "下载原图"
    override val settingsDownloadDownloadOriginImageSummary = "警告！可能需要 GP"
    override val settingsDownloadSaveAsCbz = "保存为 CBZ 压缩包"
    override val settingsDownloadArchiveMetadata = "压缩包元数据"
    override val settingsDownloadArchiveMetadataSummary = "下载压缩包时生成 ComicInfo.xml"
    override val settingsDownloadReloadMetadata = "重新加载元数据"
    override val settingsDownloadReloadMetadataSummary = "为标签可能发生变动的下载项更新 ComicInfo.xml"
    override val settingsDownloadReloadMetadataSuccessfully = { a: Int -> "成功加载 $a 项" }
    override val settingsDownloadReloadMetadataFailed = { a: String -> "加载元数据失败: $a" }
    override val settingsDownloadMediaScan = "允许媒体扫描"
    override val settingsDownloadMediaScanSummaryOn = "请避免他人翻看你的图库应用"
    override val settingsDownloadMediaScanSummaryOff = "大多数图库应用将不会显示下载目录中的图片"
    override val settingsDownloadRestoreDownloadItems = "恢复下载项"
    override val settingsDownloadRestoreDownloadItemsSummary = "恢复下载目录里的所有下载项"
    override val settingsDownloadRestoreNotFound = "未找到可恢复下载项"
    override val settingsDownloadRestoreFailed = "恢复失败"
    override val settingsDownloadRestoreSuccessfully = { a: Int -> "成功恢复 $a 项" }
    override val settingsDownloadCleanRedundancy = "清理下载冗余"
    override val settingsDownloadCleanRedundancySummary = "清理下载目录中不在下载列表里的图片文件"
    override val settingsDownloadCleanRedundancyNoRedundancy = "未发现冗余"
    override val settingsDownloadCleanRedundancyDone = { a: Int -> "完成冗余清理，共清理 $a 项" }
    override val settingsAdvanced = "高级"
    override val settingsAdvancedSaveParseErrorBody = "解析失败时保存页面内容"
    override val settingsAdvancedSaveParseErrorBodySummary = "页面内容可能含有隐私敏感信息"
    override val settingsAdvancedSaveCrashLog = "应用崩溃时保存错误日志"
    override val settingsAdvancedSaveCrashLogSummary = "错误日志可以帮助开发者修正问题"
    override val settingsAdvancedDumpLogcat = "导出日志"
    override val settingsAdvancedDumpLogcatSummary = "保存日志至外置存储器"
    override val settingsAdvancedDumpLogcatFailed = "导出日志失败"
    override val settingsAdvancedDumpLogcatTo = { a: String -> "已保存日志至 $a" }
    override val settingsAdvancedReadCacheSize = "阅读缓存大小"
    override val settingsAdvancedAppLanguageTitle = "App 界面语言"
    override val settingsAdvancedHardwareBitmapThreshold = "硬件位图（性能更好）阈值"
    override val settingsAdvancedHardwareBitmapThresholdSummary = "若长图无法加载请尝试减小此项"
    override val settingsAdvancedExportData = "导出数据"
    override val settingsAdvancedExportDataSummary = "保存数据至外置存储器，例如下载列表，快速搜索列表"
    override val settingsAdvancedExportDataTo = { a: String -> "已导出数据至 $a" }
    override val settingsAdvancedExportDataFailed = "导出数据失败"
    override val settingsAdvancedImportData = "导入数据"
    override val settingsAdvancedImportDataSummary = "从外置存储器导入数据"
    override val settingsAdvancedImportDataSuccessfully = "导入数据成功"
    override val settingsAdvancedBackupFavorite = "备份收藏列表"
    override val settingsAdvancedBackupFavoriteSummary = "备份云端收藏列表到本地"
    override val settingsAdvancedBackupFavoriteStart = { a: String -> "正在备份收藏列表 $a" }
    override val settingsAdvancedBackupFavoriteNothing = "没有可以备份的收藏列表"
    override val settingsAdvancedBackupFavoriteSuccess = "备份收藏列表成功"
    override val settingsAdvancedBackupFavoriteFailed = "备份收藏列表失败"
    override val settingsAbout = "关于"
    override val settingsAboutDeclarationSummary = "EhViewer 与 E-Hentai.org 无任何联系"
    override val settingsAboutAuthor = "作者"
    override val settingsAboutLatestRelease = "最新版本"
    override val settingsAboutSource = "源码"
    override val license = "许可证"
    override val settingsAboutVersion = "版本号"
    override val settingsAboutCommitTime = { a: String -> "于 $a 提交" }
    override val settingsAboutCheckForUpdates = "检查更新"
    override val pleaseWait = "请稍候"
    override val cantReadTheFile = "无法读取文件"
    override val appLanguageSystem = "系统语言（默认）"
    override val cloudFavorites = "云端收藏"
    override val localFavorites = "本地收藏"
    override val searchBarHint = { a: String -> "搜索 $a" }
    override val favoritesTitle = { a: String -> a }
    override val favoritesTitle2 = { a: String, b: String -> "$a - $b" }
    override val deleteFavoritesDialogTitle = "删除收藏"
    override val deleteFavoritesDialogMessage = { a: Int -> "删除 $a 项收藏？" }
    override val moveFavoritesDialogTitle = "移动收藏"
    override val defaultFavoritesCollection = "默认收藏夹"
    override val defaultFavoritesWarning = "启用此项将无法添加收藏备注"
    override val letMeSelect = "让我选择"
    override val favoriteNote = "收藏备注"
    override val collections = "收藏夹"
    override val errorSomethingWrongHappened = "被玩坏了"
    override val fromTheFuture = "来自未来"
    override val justNow = "刚刚"
    override val yesterday = "昨天"
    override val someDaysAgo = { a: Int -> "$a 天前" }
    override val archive = "压缩包"
    override val archiveFree = "免费"
    override val archiveOriginal = "原始"
    override val archiveResample = "重采样"
    override val noArchives = "没有压缩包"
    override val downloadArchiveStarted = "开始下载压缩包"
    override val downloadArchiveFailure = "无法下载压缩包"
    override val downloadArchiveFailureNoHath = "下载压缩包需要 H@H 客户端"
    override val currentFunds = "当前资金："
    override val insufficientFunds = "余额不足"
    override val imageLimits = "图片配额"
    override val imageLimitsSummary = "已用："
    override val imageLimitsNormal = "未受限制"
    override val imageLimitsRestricted = "图片分辨率限制为 1280x"
    override val resetCost = { a: Int -> "花费 $a GP 重置" }
    override val reset = "重置"
    override val settingsPrivacy = "隐私"
    override val settingsPrivacySecure = "不允许屏幕抓取"
    override val settingsPrivacySecureSummary = "启用后，将不能截取该应用的屏幕截图，同时，将不会在系统任务切换器中显示该应用的内容预览，重新加载应用以生效此更改"
    override val clearSearchHistory = "清除设备上的搜索记录"
    override val clearSearchHistorySummary = "移除您在此设备上执行过的搜索查询的记录"
    override val clearSearchHistoryConfirm = "要清除搜索记录吗？"
    override val searchHistoryCleared = "已清除搜索记录"
    override val downloadService = "下载服务"
    override val keyFavoriteName = "收藏画廊"
    override val blackDarkTheme = "纯黑深色主题"
    override val harmonizeCategoryColor = "类别颜色适配动态配色"
    override val backupBeforeUpdate = "更新前备份数据"
    override val useCiUpdateChannel = "使用持续集成(CI)构建频道更新"
    override val sortBy = "排序依据"
    override val addedTimeDesc = "添加时间（降序）"
    override val addedTimeAsc = "添加时间（升序）"
    override val uploadedTimeDesc = "上传时间（降序）"
    override val uploadedTimeAsc = "上传时间（升序）"
    override val titleAsc = "标题（升序）"
    override val titleDesc = "标题（降序）"
    override val pageCountAsc = "页数（升序）"
    override val pageCountDesc = "页数（降序）"
    override val groupByDownloadLabel = "按下载标签分组"
    override val downloadFilter = "过滤"
    override val downloadAll = "全部"
    override val downloadStartAllReversed = "全部开始（倒序）"
    override val settingsDownloadDownloadDelay = "下载延时"
    override val settingsDownloadDownloadDelaySummary = { a: Int -> "每次下载延时 $a 毫秒" }
    override val settingsDownloadConnectionTimeout = "连接超时（秒）"
    override val settingsDownloadTimeoutSpeed = "最低响应速度（KB/s）"
    override val noBrowserInstalled = "请安装一个浏览器。"
    override val toplistAlltime = "从始至终"
    override val toplistPastyear = "过去一年"
    override val toplistPastmonth = "过去一个月"
    override val toplistYesterday = "昨日"
    override val toplist = "排行"
    override val tagVoteDown = "不，这不是"
    override val tagVoteUp = "是的，这很正确"
    override val tagVoteWithdraw = "撤回投票"
    override val tagVoteSuccessfully = "投票成功"
    override val deleteSearchHistory = { a: String -> "从搜索记录中删除“$a”？" }
    override val actionAddTag = "添加标签"
    override val actionAddTagTip = "添加新标签"
    override val commentUserUploader = { a: String -> "$a （上传者）" }
    override val noNetwork = "无网络"
    override val settingsEhMeteredNetworkWarning = "流量计费网络警告"
    override val meteredNetworkWarning = "正在使用流量计费网络"
    override val readFrom = { a: Int -> "从第 $a 页阅读" }
    override val settingsEhRequestNews = "定时请求新闻页面"
    override val settingsEhHideHvEvents = "隐藏 HV 事件通知"
    override val copyTrans = "复制翻译"
    override val defaultDownloadDirNotEmpty = "默认下载目录非空！"
    override val resetDownloadLocation = "恢复默认路径"
    override val pickNewDownloadLocation = "选择新路径"
    override val dontShowAgain = "不再显示"
    override val openSettings = "打开设置"
    override val appLinkNotVerifiedMessage = "对于 Android 12 及更新的版本，您需要手动添加链接到已验证链接才能在 EhViewer 中打开 E-Hentai 链接。"
    override val appLinkNotVerifiedTitle = "应用链接未验证"
    override val openByDefault = "默认打开"
    override val settingsPrivacyRequireUnlock = "需要解锁"
    override val settingsPrivacyRequireUnlockDelay = "锁定延迟"
    override val settingsPrivacyRequireUnlockDelaySummary = { a: Int -> "离开应用并在 $a 分钟内返回时不需要解锁" }
    override val settingsPrivacyRequireUnlockDelaySummaryImmediately = "无论何时回到应用均要求解锁"
    override val filterLabel = "屏蔽项类型"
    override val archivePasswd = "归档文件密码"
    override val archiveNeedPasswd = "归档文件需要密码"
    override val passwdWrong = "密码错误"
    override val passwdCannotBeEmpty = "密码不能为空"
    override val listTileThumbSize = "详情模式下缩略图大小"
    override val accountName = "账户"
    override val preloadThumbAggressively = "激进地预载缩略图"
    override val animateItems = "列表项目动画"
    override val animateItemsSummary = "如遇崩溃/掉帧请尝试禁用此项"
    override val autoUpdates = "自动检查更新"
    override val updateFrequencyNever = "从不"
    override val updateFrequencyDaily = "每天"
    override val updateFrequency3days = "每三天"
    override val updateFrequencyWeekly = "每周"
    override val updateFrequencyBiweekly = "每两周"
    override val updateFrequencyMonthly = "每月"
    override val updateFailed = { a: String -> "更新失败：$a" }
    override val newVersionAvailable = "有新版本！"
    override val alreadyLatestVersion = "已是最新版本"
    override val permissionDenied = "权限被拒"
    override val downloadGalleryFirst = "请先下载画廊！"
    override val exportAsArchive = "导出为归档文件"
    override val exportAsArchiveSuccess = "导出成功"
    override val exportAsArchiveFailed = "导出失败"
    override val actionSettings = "设置"
    override val actionRetry = "重试"
    override val actionShare = "分享"
    override val actionCopy = "复制"
    override val actionSave = "保存"
    override val actionSaveTo = "保存到…"
    override val prefCategoryGeneral = "通用"
    override val prefFullscreen = "全屏"
    override val prefPageTransitions = "页面过渡动画"
    override val prefShowPageNumber = "显示页码"
    override val prefShowReaderSeekbar = "显示跳页拖动条"
    override val prefDoubleTapToZoom = "双击缩放"
    override val prefCustomBrightness = "自定义亮度"
    override val prefCustomColorFilter = "自定义滤镜"
    override val prefKeepScreenOn = "保持屏幕常亮"
    override val prefReaderTheme = "背景颜色"
    override val whiteBackground = "白色"
    override val blackBackground = "黑色"
    override val leftToRightViewer = "从左到右"
    override val rightToLeftViewer = "从右到左"
    override val verticalViewer = "从上到下"
    override val webtoonViewer = "条漫"
    override val pagerViewer = "单页式"
    override val prefImageScaleType = "缩放模式"
    override val scaleTypeFitScreen = "填充屏幕"
    override val scaleTypeStretch = "拉伸"
    override val scaleTypeFitWidth = "适应宽度"
    override val scaleTypeFitHeight = "适应高度"
    override val scaleTypeOriginalSize = "原始大小"
    override val scaleTypeSmartFit = "智能填充"
    override val prefZoomStart = "缩放位置"
    override val zoomStartAutomatic = "自动"
    override val zoomStartLeft = "左边"
    override val zoomStartRight = "右边"
    override val zoomStartCenter = "中间"
    override val prefRotationType = "默认屏幕方向"
    override val rotationFree = "跟随系统"
    override val rotationForcePortrait = "锁定竖屏"
    override val rotationForceLandscape = "锁定横屏"
    override val customFilter = "滤镜"
    override val decodeImageError = "无法加载该图片"
    override val prefReadWithLongTap = "长按显示操作菜单"
    override val prefColorFilterMode = "滤镜调和模式"
    override val filterModeMultiply = "色彩增值"
    override val filterModeScreen = "滤色"
    override val filterModeOverlay = "覆盖"
    override val filterModeLighten = "减淡/变亮"
    override val filterModeDarken = "加深/变暗"
    override val prefCutoutShort = "在刘海屏区域显示内容"
    override val actionMenu = "菜单"
    override val webtoonSidePadding25 = "25%"
    override val webtoonSidePadding20 = "20%"
    override val webtoonSidePadding15 = "15%"
    override val webtoonSidePadding10 = "10%"
    override val webtoonSidePadding0 = "无"
    override val prefWebtoonSidePadding = "侧边填充"
    override val verticalPlusViewer = "从上到下（连续）"
    override val grayBackground = "灰色"
    override val viewer = "阅读模式"
    override val tappingInvertedBoth = "水平 + 垂直"
    override val tappingInvertedVertical = "垂直"
    override val tappingInvertedHorizontal = "水平"
    override val tappingInvertedNone = "无"
    override val prefCategoryReadingMode = "阅读模式"
    override val prefReadWithTappingInverted = "反转点按区域"
    override val prefViewerNav = "点按区域"
    override val edgeNav = "边缘样式"
    override val kindlishNav = "Kindle 样式"
    override val lNav = "L 样式"
    override val rightAndLeftNav = "左右样式"
    override val navZoneRight = "右"
    override val navZoneLeft = "左"
    override val navZoneNext = "下页"
    override val navZonePrev = "上页"
    override val rotationLandscape = "横屏"
    override val rotationPortrait = "竖屏"
    override val rotationType = "屏幕方向"
    override val prefGrayscale = "灰度"
    override val automaticBackground = "自动"
    override val prefInvertedColors = "反色"
    override val labelDefault = "默认"
    override val webtoonSidePadding5 = "5%"
    override val prefLandscapeZoom = "放大横向图片"
    override val prefNavigatePan = "图片放大时先平移再翻页"
    override val rotationReversePortrait = "反向竖屏"
    override val disabledNav = "关闭"
    override val wideColorGamut = "使用Display P3色彩空间"
    override val settingsEhRequestNewsTimepicker = "设置请求新闻页面的时间"
    override val darkTheme = "深色主题"
    override val darkThemeFollowSystem = "跟随系统"
    override val darkThemeOff = "总是关闭"
    override val darkThemeOn = "总是开启"
    override val prefCropBorders = "裁剪边缘"
    override val blockedImage = "已屏蔽的图片"
    override val showBlockedImage = "显示已屏蔽的图片"
    override val pageCount = { a: Int -> "$a 页" }
    override val someMinutesAgo = { a: Int -> "$a 分钟前" }
    override val someHoursAgo = { a: Int -> "$a 小时前" }
    override val second = { _: Int -> "秒" }
    override val minute = { _: Int -> "分钟" }
    override val hour = { _: Int -> "小时" }
    override val day = { _: Int -> "天" }
    override val year = { _: Int -> "年" }
}
