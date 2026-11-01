package com.example.domain.models

import org.json.JSONArray
import org.json.JSONObject

enum class SourceStatus(val labelAr: String) {
    SUPPORTED("مدعوم بالكامل (SUPPORTED)"),
    PARTIALLY_SUPPORTED("مدعوم جزئياً (PARTIALLY_SUPPORTED)"),
    WEBVIEW_ONLY("يعمل عبر WebView فقط (WEBVIEW_ONLY)"),
    NEEDS_CONFIGURATION("يحتاج إعداد محددات (NEEDS_CONFIGURATION)"),
    NEEDS_VERIFICATION("يتطلب تحققاً أمنياً (NEEDS_VERIFICATION)"),
    UNSUPPORTED("غير مدعوم (UNSUPPORTED)"),
    NETWORK_ERROR("خطأ في الشبكة (NETWORK_ERROR)"),
    NEEDS_ADAPTER("يحتاج إلى موصل (NEEDS_ADAPTER)"),
    PARSING_ERROR("خطأ في الاستخراج (PARSING_ERROR)"),
    NEEDS_UPDATE("يحتاج إلى تحديث"),
    REQUIRES_VERIFICATION("يتطلب تحققاً أمنياً"),
    DISABLED("معطل"),
    ERROR("خطأ في المصدر");

    val isUsable: Boolean
        get() = this in setOf(
            SUPPORTED,
            PARTIALLY_SUPPORTED,
            WEBVIEW_ONLY,
            NEEDS_CONFIGURATION,
            NEEDS_VERIFICATION,
            REQUIRES_VERIFICATION
        )
}

enum class ContentRating(
    val labelAr: String,
    val badgeText: String,
    val badgeColorHex: Long,
    val isSensitive: Boolean
) {
    GENERAL("عام (جميع الأعمار)", "عام", 0xFF4CAF50, false),
    TEEN("مراهقين (+13)", "+13", 0xFF2196F3, false),
    MATURE("ناضجين (+16)", "+16", 0xFFFF9800, true),
    ADULT_18_PLUS("للبالغين فقط (+18)", "+18", 0xFFE53935, true)
}

/**
 * Classifies manga/source content rating and blocks any illegal/prohibited content.
 */
object ContentSafetyClassifier {
    private val ILLEGAL_PROHIBITED_TERMS = listOf(
        "csam", "child abuse", "pedophil", "underage", "loli", "shota", "minor explicit"
    )

    private val ADULT_18_KEYWORDS = listOf(
        "18+", "+18", "adult", "hentai", "smut", "erotica", "nsfw", "uncensored",
        "mature 18", "للبالغين", "بالغين", "إيتشي صريح", "راشدين", "بدون حجب"
    )

    private val MATURE_16_KEYWORDS = listOf(
        "mature", "ecchi", "gore", "sexual violence", "ناضجين", "إيتشي", "دموي", "+16", "16+"
    )

    private val TEEN_13_KEYWORDS = listOf(
        "action", "fantasy", "martial arts", "shounen", "seinen", "أكشن", "اكشن", "فانتازيا", "فنون قتالية", "شونين", "سينين"
    )

    fun isProhibitedIllegalContent(
        title: String,
        description: String = "",
        genres: List<String> = emptyList()
    ): Boolean {
        val combined = buildString {
            append(title).append(' ')
            append(description).append(' ')
            append(genres.joinToString(" "))
        }.lowercase()
        return ILLEGAL_PROHIBITED_TERMS.any { combined.contains(it) }
    }

    fun classify(
        title: String,
        description: String = "",
        genres: List<String> = emptyList(),
        domain: String = "",
        sourceDefaultRating: ContentRating = ContentRating.GENERAL
    ): ContentRating {
        val combined = buildString {
            append(title).append(' ')
            append(description).append(' ')
            append(genres.joinToString(" ")).append(' ')
            append(domain)
        }.lowercase()

        if (ADULT_18_KEYWORDS.any { combined.contains(it) }) {
            return ContentRating.ADULT_18_PLUS
        }
        if (sourceDefaultRating == ContentRating.ADULT_18_PLUS) {
            return ContentRating.ADULT_18_PLUS
        }
        if (MATURE_16_KEYWORDS.any { combined.contains(it) }) {
            return ContentRating.MATURE
        }
        if (sourceDefaultRating == ContentRating.MATURE) {
            return ContentRating.MATURE
        }
        if (TEEN_13_KEYWORDS.any { combined.contains(it) }) {
            return ContentRating.TEEN
        }
        return sourceDefaultRating
    }

    fun classifyFromGenresAndText(
        genres: List<String> = emptyList(),
        title: String = "",
        description: String = "",
        domain: String = "",
        explicitFlag: Boolean = false
    ): ContentRating {
        if (explicitFlag) return ContentRating.ADULT_18_PLUS
        return classify(
            title = title,
            description = description,
            genres = genres,
            domain = domain
        )
    }

    fun classifySource(domain: String, name: String = ""): ContentRating {
        return classify(title = name, domain = domain)
    }

    fun classifyManga(
        title: String,
        description: String = "",
        genres: List<String> = emptyList(),
        sourceRating: ContentRating = ContentRating.GENERAL
    ): ContentRating {
        return classify(
            title = title,
            description = description,
            genres = genres,
            sourceDefaultRating = sourceRating
        )
    }
}

enum class SourceEngineType(val labelAr: String) {
    MADARA("محرك WordPress Madara"),
    MANGASTREAM("محرك MangaStream / Themesia"),
    MANGAREADER("محرك WP-MangaReader"),
    CUSTOM_HTML("محرك HTML مخصص (Custom Selectors)"),
    GENERIC_HTML("محرك HTML ديناميكي عام"),
    JSON_API("محرك JSON API"),
    REST_API("محرك REST API رسمي"),
    RSS_FEED("محرك خلاصات RSS / Atom"),
    JS_DOM("محرك JavaScript DOM"),
    WEBVIEW_ONLY("محرك WebView التفاعلي فقط")
}

enum class SourceCapability(val labelAr: String) {
    POPULAR_MANGA("قائمة الأعمال الشائعة"),
    LATEST_UPDATES("آخر التحديثات"),
    SEARCH_MANGA("البحث في الأعمال"),
    MANGA_DETAILS("تفاصيل العمل"),
    MANGA_CHAPTERS("قائمة الفصول"),
    CHAPTER_PAGES("صفحات الفصل"),
    PAGINATION("تعدد الصفحات"),
    CATEGORIES("التصنيفات والفئات"),
    FILTERS("الفرز والتصفية")
}

/**
 * Serializable custom connector configuration for any manga/manhwa website.
 * Can be auto-generated by [com.example.sources.SourceDiscoveryEngine], customized manually by the user,
 * or imported/exported as JSON.
 */
data class SourceConfiguration(
    val domain: String,
    val sourceId: String = "site_${domain.lowercase().replace('.', '_').replace('-', '_')}",
    val sourceName: String = domain.substringBefore('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
    val baseUrl: String = "https://$domain",
    val language: String = "ar",
    val engineType: SourceEngineType = SourceEngineType.MADARA,
    val contentRating: ContentRating = ContentRating.GENERAL,
    val popularPath: String = "/manga/",
    val latestPath: String = "/manga/?m_orderby=latest",
    val searchPath: String = "/?s={query}&post_type=wp-manga",
    val paginationPattern: String = "page/{page}/",
    val popularUrlTemplate: String = "{baseUrl}/manga/?page={page}",
    val latestUrlTemplate: String = "{baseUrl}/manga/?m_orderby=latest&page={page}",
    val searchUrlTemplate: String = "{baseUrl}/?s={query}&post_type=wp-manga",
    val mangaCardSelector: String = "div.page-item-detail, div.c-tabs-item__content, div.bsx, div.utao, div.uta, div.box, div.manga-item, article.item",
    val mangaItemSelector: String = mangaCardSelector,
    val mangaTitleSelector: String = ".post-title h3 a, .post-title a, .luf a h4, .luf a, .tt, h3 a, h4 a",
    val titleSelector: String = mangaTitleSelector,
    val mangaUrlSelector: String = ".post-title a[href], .luf a[href], h3 a[href], .item-thumb a[href], a[href*=/manga/], a[href*=/series/], a[href]",
    val coverImageSelector: String = ".item-thumb img, .tab-thumb img, .limit img, .imgu img, .manga-poster img, img",
    val coverSelector: String = coverImageSelector,
    val coverImageAttributes: String = "data-src,data-lazy-src,data-original,data-bg,srcset,src",
    val latestChapterSelector: String = ".list-chapter .chapter-item:first-child .chapter a, .latest-chap .chapter a, .luf ul li a, .epxs, .chapter a",
    val nextPageSelector: String = "a.next, a.nextpostslink, .nav-previous a, .pagination .next, .hpage a.r",
    val detailsTitleSelector: String = ".post-title h1, h1.entry-title, .author-info-title h1, h1",
    val detailsDescriptionSelector: String = ".summary__content p, .description-summary p, .entry-content p, .review-content p, .summary__content",
    val descriptionSelector: String = detailsDescriptionSelector,
    val detailsCoverSelector: String = ".summary_image img, .thumb img, .manga-poster img, .seriestucontl img, img.wp-post-image",
    val detailsAuthorSelector: String = ".author-content a, .fmed b:contains(المؤلف) + span",
    val authorSelector: String = detailsAuthorSelector,
    val detailsStatusSelector: String = ".post-status .summary-content, .tsinfo .imptdt:contains(الحالة) i",
    val statusSelector: String = detailsStatusSelector,
    val detailsGenresSelector: String = ".genres-content a, .mgen a, .seriestugenre a, .review-author-info a",
    val genreSelector: String = detailsGenresSelector,
    val chapterListSelector: String = "li.wp-manga-chapter, #chapterlist li, .eplister ul li, .listing-chapters_wrap li",
    val chapterTitleSelector: String = ".chapternum, .epl-num, .epl-title, a",
    val chapterUrlSelector: String = "a[href]",
    val chapterDateSelector: String = ".chapter-release-date, .chapterdate, .epl-date, span.date",
    val pageImageSelector: String = ".reading-content img, .page-break img, #readerarea img, img.wp-manga-chapter-img, img.manga-chapter-img",
    val pageImageAttributes: String = "data-src,data-lazy-src,data-original,src",
    val customUserAgent: String = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36",
    val userAgent: String = customUserAgent,
    val customReferer: String = "https://$domain/",
    val customHeadersJson: String = "{}",
    val headers: Map<String, String> = emptyMap(),
    val requiresWebViewVerification: Boolean = false,
    val requiresVerification: Boolean = requiresWebViewVerification,
    val useWebViewOnlyMode: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun hasCustomSelectors(): Boolean {
        return engineType == SourceEngineType.CUSTOM_HTML ||
            mangaItemSelector.isNotBlank() ||
            coverSelector.isNotBlank() ||
            chapterListSelector.isNotBlank() ||
            pageImageSelector.isNotBlank()
    }

    fun toJsonString(): String {
        fun esc(s: String): String = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return buildString {
            append("{\n")
            append("  \"sourceId\": \"${esc(sourceId)}\",\n")
            append("  \"sourceName\": \"${esc(sourceName)}\",\n")
            append("  \"domain\": \"${esc(domain)}\",\n")
            append("  \"baseUrl\": \"${esc(baseUrl)}\",\n")
            append("  \"language\": \"${esc(language)}\",\n")
            append("  \"engineType\": \"${esc(engineType.name)}\",\n")
            append("  \"contentRating\": \"${esc(contentRating.name)}\",\n")
            append("  \"popularPath\": \"${esc(popularPath)}\",\n")
            append("  \"latestPath\": \"${esc(latestPath)}\",\n")
            append("  \"searchPath\": \"${esc(searchPath)}\",\n")
            append("  \"paginationPattern\": \"${esc(paginationPattern)}\",\n")
            append("  \"popularUrlTemplate\": \"${esc(popularUrlTemplate)}\",\n")
            append("  \"latestUrlTemplate\": \"${esc(latestUrlTemplate)}\",\n")
            append("  \"searchUrlTemplate\": \"${esc(searchUrlTemplate)}\",\n")
            append("  \"mangaCardSelector\": \"${esc(mangaCardSelector)}\",\n")
            append("  \"mangaItemSelector\": \"${esc(mangaItemSelector)}\",\n")
            append("  \"mangaTitleSelector\": \"${esc(mangaTitleSelector)}\",\n")
            append("  \"titleSelector\": \"${esc(titleSelector)}\",\n")
            append("  \"mangaUrlSelector\": \"${esc(mangaUrlSelector)}\",\n")
            append("  \"coverImageSelector\": \"${esc(coverImageSelector)}\",\n")
            append("  \"coverSelector\": \"${esc(coverSelector)}\",\n")
            append("  \"coverImageAttributes\": \"${esc(coverImageAttributes)}\",\n")
            append("  \"latestChapterSelector\": \"${esc(latestChapterSelector)}\",\n")
            append("  \"nextPageSelector\": \"${esc(nextPageSelector)}\",\n")
            append("  \"detailsTitleSelector\": \"${esc(detailsTitleSelector)}\",\n")
            append("  \"detailsDescriptionSelector\": \"${esc(detailsDescriptionSelector)}\",\n")
            append("  \"descriptionSelector\": \"${esc(descriptionSelector)}\",\n")
            append("  \"detailsCoverSelector\": \"${esc(detailsCoverSelector)}\",\n")
            append("  \"detailsAuthorSelector\": \"${esc(detailsAuthorSelector)}\",\n")
            append("  \"authorSelector\": \"${esc(authorSelector)}\",\n")
            append("  \"detailsStatusSelector\": \"${esc(detailsStatusSelector)}\",\n")
            append("  \"statusSelector\": \"${esc(statusSelector)}\",\n")
            append("  \"detailsGenresSelector\": \"${esc(detailsGenresSelector)}\",\n")
            append("  \"genreSelector\": \"${esc(genreSelector)}\",\n")
            append("  \"chapterListSelector\": \"${esc(chapterListSelector)}\",\n")
            append("  \"chapterTitleSelector\": \"${esc(chapterTitleSelector)}\",\n")
            append("  \"chapterUrlSelector\": \"${esc(chapterUrlSelector)}\",\n")
            append("  \"chapterDateSelector\": \"${esc(chapterDateSelector)}\",\n")
            append("  \"pageImageSelector\": \"${esc(pageImageSelector)}\",\n")
            append("  \"pageImageAttributes\": \"${esc(pageImageAttributes)}\",\n")
            append("  \"customUserAgent\": \"${esc(userAgent)}\",\n")
            append("  \"customReferer\": \"${esc(customReferer)}\",\n")
            append("  \"customHeadersJson\": \"${esc(customHeadersJson)}\",\n")
            append("  \"requiresWebViewVerification\": $requiresVerification,\n")
            append("  \"requiresVerification\": $requiresVerification,\n")
            append("  \"useWebViewOnlyMode\": $useWebViewOnlyMode,\n")
            append("  \"updatedAt\": $updatedAt\n")
            append("}")
        }
    }

    companion object {
        fun toJsonString(config: SourceConfiguration): String = config.toJsonString()

        fun fromJsonString(json: String): Result<SourceConfiguration> = runCatching {
            parseJsonOrThrow(json)
        }

        fun parseJsonOrThrow(json: String): SourceConfiguration {
            val trimmed = json.trim()
            require(trimmed.startsWith("{") && trimmed.endsWith("}")) { "صيغة JSON غير صالحة" }

            fun extractString(key: String, default: String = ""): String {
                val pattern = Regex(""""${Regex.escape(key)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
                val match = pattern.find(trimmed) ?: return default
                return match.groupValues[1]
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\\\", "\\")
            }

            fun extractBoolean(key: String, default: Boolean = false): Boolean {
                val pattern = Regex(""""${Regex.escape(key)}"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE)
                val match = pattern.find(trimmed) ?: return default
                return match.groupValues[1].equals("true", ignoreCase = true)
            }

            val domain = extractString("domain", "").trim().lowercase()
            require(domain.isNotBlank()) { "حقل domain مفقود في ملف إعدادات JSON" }
            val id = extractString("sourceId", "site_${domain.replace('.', '_')}").trim()
            val name = extractString("sourceName", domain.substringBefore('.')).trim()
            val baseUrl = extractString("baseUrl", "https://$domain").trim()
            val engine = runCatching {
                SourceEngineType.valueOf(extractString("engineType", SourceEngineType.MADARA.name))
            }.getOrDefault(SourceEngineType.MADARA)
            val rating = runCatching {
                ContentRating.valueOf(extractString("contentRating", ContentRating.GENERAL.name))
            }.getOrDefault(ContentRating.GENERAL)

            val cardSel = extractString(
                "mangaCardSelector",
                extractString(
                    "mangaItemSelector",
                    "div.page-item-detail, div.c-tabs-item__content, div.bsx, div.utao, div.uta, div.box, div.manga-item, article.item"
                )
            )
            val titleSel = extractString(
                "mangaTitleSelector",
                extractString(
                    "titleSelector",
                    ".post-title h3 a, .post-title a, .luf a h4, .luf a, .tt, h3 a, h4 a"
                )
            )
            val coverSel = extractString(
                "coverImageSelector",
                extractString(
                    "coverSelector",
                    ".item-thumb img, .tab-thumb img, .limit img, .imgu img, .manga-poster img, img"
                )
            )
            val descSel = extractString(
                "detailsDescriptionSelector",
                extractString(
                    "descriptionSelector",
                    ".summary__content p, .description-summary p, .entry-content p, .review-content p, .summary__content"
                )
            )
            val genreSel = extractString(
                "detailsGenresSelector",
                extractString(
                    "genreSelector",
                    ".genres-content a, .mgen a, .seriestugenre a, .review-author-info a"
                )
            )
            val authorSel = extractString(
                "detailsAuthorSelector",
                extractString(
                    "authorSelector",
                    ".author-content a, .fmed b:contains(المؤلف) + span"
                )
            )
            val statusSel = extractString(
                "detailsStatusSelector",
                extractString(
                    "statusSelector",
                    ".post-status .summary-content, .tsinfo .imptdt:contains(الحالة) i"
                )
            )
            val reqVer = extractBoolean(
                "requiresVerification",
                extractBoolean("requiresWebViewVerification", false)
            )

            return SourceConfiguration(
                sourceId = id,
                sourceName = name,
                domain = domain,
                baseUrl = baseUrl,
                language = extractString("language", "ar"),
                engineType = engine,
                contentRating = rating,
                popularPath = extractString("popularPath", "/manga/"),
                latestPath = extractString("latestPath", "/manga/?m_orderby=latest"),
                searchPath = extractString("searchPath", "/?s={query}&post_type=wp-manga"),
                paginationPattern = extractString("paginationPattern", "page/{page}/"),
                popularUrlTemplate = extractString("popularUrlTemplate", "{baseUrl}/manga/?page={page}"),
                latestUrlTemplate = extractString("latestUrlTemplate", "{baseUrl}/manga/?m_orderby=latest&page={page}"),
                searchUrlTemplate = extractString("searchUrlTemplate", "{baseUrl}/?s={query}&post_type=wp-manga"),
                mangaCardSelector = cardSel,
                mangaItemSelector = cardSel,
                mangaTitleSelector = titleSel,
                titleSelector = titleSel,
                mangaUrlSelector = extractString(
                    "mangaUrlSelector",
                    ".post-title a[href], .luf a[href], h3 a[href], .item-thumb a[href], a[href*=/manga/], a[href*=/series/], a[href]"
                ),
                coverImageSelector = coverSel,
                coverSelector = coverSel,
                coverImageAttributes = extractString(
                    "coverImageAttributes",
                    "data-src,data-lazy-src,data-original,data-bg,srcset,src"
                ),
                latestChapterSelector = extractString(
                    "latestChapterSelector",
                    ".list-chapter .chapter-item:first-child .chapter a, .latest-chap .chapter a, .luf ul li a, .epxs, .chapter a"
                ),
                nextPageSelector = extractString(
                    "nextPageSelector",
                    "a.next, a.nextpostslink, .nav-previous a, .pagination .next, .hpage a.r"
                ),
                detailsTitleSelector = extractString(
                    "detailsTitleSelector",
                    ".post-title h1, h1.entry-title, .author-info-title h1, h1"
                ),
                detailsDescriptionSelector = descSel,
                descriptionSelector = descSel,
                detailsCoverSelector = extractString(
                    "detailsCoverSelector",
                    ".summary_image img, .thumb img, .manga-poster img, .seriestucontl img, img.wp-post-image"
                ),
                detailsAuthorSelector = authorSel,
                authorSelector = authorSel,
                detailsStatusSelector = statusSel,
                statusSelector = statusSel,
                detailsGenresSelector = genreSel,
                genreSelector = genreSel,
                chapterListSelector = extractString(
                    "chapterListSelector",
                    "li.wp-manga-chapter, #chapterlist li, .eplister ul li, .listing-chapters_wrap li"
                ),
                chapterTitleSelector = extractString("chapterTitleSelector", ".chapternum, .epl-num, .epl-title, a"),
                chapterUrlSelector = extractString("chapterUrlSelector", "a[href]"),
                chapterDateSelector = extractString("chapterDateSelector", ".chapter-release-date, .chapterdate, .epl-date, span.date"),
                pageImageSelector = extractString(
                    "pageImageSelector",
                    ".reading-content img, .page-break img, #readerarea img, img.wp-manga-chapter-img, img.manga-chapter-img"
                ),
                pageImageAttributes = extractString("pageImageAttributes", "data-src,data-lazy-src,data-original,src"),
                customUserAgent = extractString(
                    "customUserAgent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                ),
                customReferer = extractString("customReferer", "$baseUrl/"),
                customHeadersJson = extractString("customHeadersJson", "{}"),
                requiresWebViewVerification = reqVer,
                requiresVerification = reqVer,
                useWebViewOnlyMode = extractBoolean("useWebViewOnlyMode", false),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Section 3: 14-point Site Inspection Report produced by [com.example.sources.SourceDiscoveryEngine].
 */
data class SiteInspectionReport(
    val inputUrl: String,
    val normalizedDomain: String,
    val baseUrl: String,
    // 1. صحة النطاق
    val isValidDomain: Boolean = true,
    // 2. دعم HTTPS
    val usesHttps: Boolean = true,
    // 3. إمكانية الوصول للموقع
    val isReachable: Boolean = false,
    val httpStatusCode: Int = 0,
    val responseTimeMs: Long = 0L,
    // 4. وجود تحقق أمني
    val requiresVerification: Boolean = false,
    val verificationChallengeType: String = "NONE",
    // 5. نوع الموقع / الموصل
    val detectedArchitecture: SourceEngineType = SourceEngineType.CUSTOM_HTML,
    // 6. وجود قائمة أعمال
    val canExtractWorksList: Boolean = false,
    val extractedWorksCount: Int = 0,
    val sampleMangaTitles: List<String> = emptyList(),
    // 7. وجود صور الأغلفة
    val canExtractCovers: Boolean = false,
    val sampleCoverUrl: String? = null,
    // 8. وجود صفحات التفاصيل
    val canExtractDetails: Boolean = false,
    // 9. وجود قائمة الفصول
    val canExtractChapters: Boolean = false,
    val extractedChaptersCount: Int = 0,
    // 10. وجود صفحات القراءة داخل الفصل
    val canExtractPages: Boolean = false,
    val extractedPagesCount: Int = 0,
    // 11. دعم البحث
    val supportsSearch: Boolean = false,
    // 12. دعم التصفح متعدد الصفحات Pagination
    val supportsPagination: Boolean = false,
    // 13. تصنيف المحتوى
    val detectedContentRating: ContentRating = ContentRating.GENERAL,
    val detectedAdultIndicators: List<String> = emptyList(),
    // 14. حالة الدعم المقترحة وإعدادات الموصل
    val recommendedStatus: SourceStatus = SourceStatus.UNSUPPORTED,
    val suggestedName: String = "",
    val diagnosticSummaryAr: String = "",
    val recommendedConfig: SourceConfiguration? = null
) {
    val supportsHttps: Boolean get() = usesHttps
    val detectedEngineType: SourceEngineType get() = detectedArchitecture
    val canExtractMangaList: Boolean get() = canExtractWorksList
    val extractedMangaCount: Int get() = extractedWorksCount
    val detectedMangaCount: Int get() = extractedWorksCount
    val detectedChaptersCount: Int get() = extractedChaptersCount
    val detectedPagesCount: Int get() = extractedPagesCount
    val summaryAr: String get() = diagnosticSummaryAr
    val errorMessageAr: String?
        get() = if (!isReachable || recommendedStatus == SourceStatus.UNSUPPORTED) diagnosticSummaryAr else null
}

/**
 * Section 8: Single stage result in the 11-stage [SourceFullTestReport].
 */
data class SourceStageTestResult(
    val stageNameAr: String,
    val passed: Boolean,
    val itemCount: Int = 0,
    val detailsAr: String = "",
    val durationMs: Long = 0L,
    val stageNumber: Int = 0,
    val errorAr: String? = null
) {
    val count: Int get() = itemCount
}

/**
 * Section 8: Complete 11-stage test report for a manga source.
 */
data class SourceFullTestReport(
    val sourceId: String,
    val sourceName: String,
    val domain: String,
    val finalStatus: SourceStatus,
    val stages: List<SourceStageTestResult>,
    val engineType: SourceEngineType = SourceEngineType.MADARA,
    val contentRating: ContentRating = ContentRating.GENERAL,
    val extractedMangaCount: Int = stages.firstOrNull { it.stageNameAr.contains("الشائعة") }?.itemCount ?: 0,
    val extractedCoversCount: Int = stages.firstOrNull { it.stageNameAr.contains("الأغلفة") }?.itemCount ?: 0,
    val extractedChaptersCount: Int = stages.firstOrNull { it.stageNameAr.contains("الفصول") }?.itemCount ?: 0,
    val extractedPagesCount: Int = stages.firstOrNull { it.stageNameAr.contains("صفحات الفصل") }?.itemCount ?: 0,
    val sampleCoverUrl: String? = null,
    val lastError: String? = stages.firstOrNull { !it.passed }?.detailsAr,
    val testedAt: Long = System.currentTimeMillis()
) {
    val passedCount: Int
        get() = stages.count { it.passed }

    val passedStagesCount: Int
        get() = passedCount

    val summaryAr: String?
        get() = if (finalStatus == SourceStatus.SUPPORTED) null else lastError
}

/**
 * Section 11: Comprehensive 12-point Source Diagnostic Report.
 */
data class SourceDiagnosticReport(
    val sourceId: String,
    val sourceName: String,
    val domain: String,
    val baseUrl: String = "https://$domain",
    val contentRating: ContentRating = ContentRating.GENERAL,
    val engineType: SourceEngineType = SourceEngineType.MADARA,
    val connectionStatusAr: String,
    val verificationStatusAr: String,
    val adapterStatus: SourceStatus,
    val siteArchitecture: String = "HTML",
    val extractedMangaCount: Int = 0,
    val sampleCoverUrl: String? = null,
    val imagesStatusOk: Boolean = false,
    val chaptersStatusOk: Boolean = false,
    val extractedChaptersCount: Int = 0,
    val pagesStatusOk: Boolean = false,
    val extractedPagesCount: Int = 0,
    val capabilities: Set<SourceCapability> = emptySet(),
    val stageResults: List<SourceStageTestResult> = emptyList(),
    val lastError: String? = null,
    val lastTestedAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus(val labelAr: String) {
    WAITING("في الانتظار"),
    DOWNLOADING("جار التنزيل..."),
    PAUSED("موقوف مؤقتاً"),
    COMPLETED("اكتمل التنزيل"),
    FAILED("فشل التنزيل"),
    CANCELLED("ألغيت")
}

enum class ErrorType(val titleAr: String, val messageAr: String) {
    NETWORK_ERROR("خطأ في الشبكة", "تعذّر الاتصال بالإنترنت. يرجى التحقق من الشبكة والمحاولة مجدداً."),
    HTTP_ERROR("خطأ في استجابة الخادم", "أعاد الموقع رمز استجابة غير متوقع. حاول مرة أخرى لاحقاً."),
    SOURCE_UNSUPPORTED("الموقع غير مدعوم", "هذا النطاق لا يمتلك موصلاً متوافقاً مع التطبيق حالياً."),
    SOURCE_PARSE_ERROR("خطأ في تحليل الصفحة", "تغيرت بنية HTML الخاصة بالموقع أو تعذر استخراج البيانات."),
    SOURCE_UPDATE_REQUIRED("يتطلب تحديث الموصل", "بنية الموقع تغيرت ويحتاج موصل المصدر إلى تحديث."),
    SECURITY_VERIFICATION_REQUIRED("عليك فتح الصفحة وإكمال التحقق", "يطلب هذا الموقع إتمام التحقق البشري (Cloudflare) للمتابعة."),
    IMAGE_LOAD_ERROR("خطأ في التحميل", "تعذّر تحميل صورة الصفحة. حاول مجدداً!"),
    CHAPTER_EMPTY("لم يتم العثور على فصول", "لا توجد صفحات أو فصول متاحة لهذا العمل حالياً."),
    DOWNLOAD_FAILED("فشل التنزيل", "تعذّر إكمال تنزيل الفصل بسبب انقطاع الاتصال أو رفض الطلب."),
    STORAGE_FULL("مساحة التخزين ممتلئة", "لا توجد مساحة كافية على الجهاز لحفظ صفحات الفصل."),
    DATABASE_ERROR("خطأ في قاعدة البيانات", "حدث خطأ أثناء حفظ أو استرجاع البيانات المحلية.")
}

data class AppError(
    val type: ErrorType,
    val customMessage: String? = null,
    val sourceId: String? = null,
    val targetUrl: String? = null
)

data class SourceInfo(
    val id: String,
    val name: String,
    val domain: String,
    val baseUrl: String,
    val language: String = "ar",
    val iconText: String = "M",
    val iconColorHex: Long = 0xFF3F51B5,
    val isEnabled: Boolean = true,
    val status: SourceStatus = SourceStatus.SUPPORTED,
    val engineType: SourceEngineType = SourceEngineType.MADARA,
    val contentRating: ContentRating = ContentRating.GENERAL,
    val extractedMangaCount: Int = 0,
    val extractedChaptersCount: Int = 0,
    val extractedPagesCount: Int = 0,
    val lastError: String? = null,
    val hasCustomConfig: Boolean = false,
    val isCustomConfigured: Boolean = hasCustomConfig,
    val version: Int = 1,
    val lastCheckedAt: Long = System.currentTimeMillis()
)

data class MangaSummary(
    val id: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String?,
    val mangaUrl: String,
    val latestChapter: String? = null,
    val genres: List<String> = emptyList(),
    val rating: Float? = null,
    val status: String? = null,
    val contentRating: ContentRating = ContentRating.GENERAL
)

data class MangaChapter(
    val id: String,
    val mangaId: String,
    val sourceId: String,
    val remoteId: String,
    val title: String,
    val chapterNumber: Float?,
    val uploadDate: Long?,
    val dateLabel: String = "",
    val isRead: Boolean = false,
    val isDownloaded: Boolean = false,
    val lastReadPage: Int = 0
)

data class MangaDetails(
    val id: String,
    val sourceId: String,
    val title: String,
    val alternativeTitles: List<String>,
    val coverUrl: String?,
    val description: String,
    val author: String?,
    val artist: String?,
    val status: String?,
    val genres: List<String>,
    val rating: Float?,
    val chapters: List<MangaChapter>,
    val contentRating: ContentRating = ContentRating.GENERAL
)

data class MangaPage(
    val index: Int,
    val imageUrl: String,
    val localPath: String? = null,
    val chapterTitle: String = "",
    val dialoguePreviewAr: String = ""
)

data class ReadingProgress(
    val mangaId: String,
    val chapterId: String,
    val pageIndex: Int,
    val scrollOffset: Int,
    val updatedAt: Long
)

data class DownloadTask(
    val id: String,
    val chapterId: String,
    val mangaId: String,
    val mangaTitle: String,
    val chapterTitle: String,
    val coverUrl: String?,
    val totalPages: Int,
    val downloadedPages: Int,
    val failedPages: Int,
    val status: DownloadStatus,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progress: Int
        get() = if (totalPages > 0) (downloadedPages * 100) / totalPages else 0

    val progressFloat: Float
        get() = if (totalPages > 0) downloadedPages.toFloat() / totalPages.toFloat() else 0f

    val remainingSecondsEstimate: Int
        get() = ((totalPages - downloadedPages).coerceAtLeast(0)) * 2
}

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val hasNextPage: Boolean
)

enum class AppThemeStyle(
    val titleAr: String,
    val primaryPreviewColor: Long,
    val secondaryPreviewColor: Long,
    val bgPreviewColor: Long
) {
    DEFAULT("افتراضي", 0xFF96B0F8, 0xFFCE93D8, 0xFF14161F),
    DYNAMIC("ديناميك", 0xFFA5B4FC, 0xFF93C5FD, 0xFF14161F),
    GREEN("أخضر", 0xFF81C784, 0xFFA5D6A7, 0xFF121A14),
    PURPLE("أرجواني", 0xFFB39DDB, 0xFFCE93D8, 0xFF181322),
    MIDNIGHT("منتصف الليل", 0xFFCE93D8, 0xFFF48FB1, 0xFF140F1D),
    RED("أحمر", 0xFFE57373, 0xFFEF9A9A, 0xFF1F1212),
    TAKO("تاكو", 0xFFFFAB91, 0xFFFFCC80, 0xFF1C1514),
    TEAL("أزرق مخضر", 0xFF4DB6AC, 0xFF80CBC4, 0xFF101D1B),
    TIDAL_WAVE("موجة مد و جزر", 0xFF64B5F6, 0xFF4DD0E1, 0xFF0E1924),
    YIN_YANG("ين & يانغ", 0xFFE0E0E0, 0xFF9E9E9E, 0xFF121212),
    YOTSUBA("يوتسوبا", 0xFFFFB74D, 0xFFA1887F, 0xFF1E1610)
}

enum class ReaderMode(val labelAr: String) {
    VERTICAL_WEBTOON("تمرير رأسي (ويبتون)"),
    HORIZONTAL_RTL("أفقي (من اليمين إلى اليسار)"),
    HORIZONTAL_LTR("أفقي (من اليسار إلى اليمين)"),
    SINGLE_PAGE("صفحة واحدة")
}

enum class GridDisplayMode(val labelAr: String, val minCellWidthDp: Int) {
    LIST("القائمة", 320),
    SMALL("صغير", 105),
    MEDIUM("متوسط", 130),
    LARGE("كبير", 165)
}

enum class SortOption(val labelAr: String) {
    LATEST_ADDED("آخر الاضافات"),
    MOST_VIEWED("الاكثر مشاهدة"),
    NEWEST("جديد"),
    ALPHABETICAL("أبجدي"),
    RATING("التقييم"),
    POPULAR("الشائع")
}

enum class StatusFilter(val labelAr: String) {
    ALL("الكل"),
    COMPLETED("مكتمل"),
    ONGOING("مستمر"),
    CANCELLED("ملغي"),
    HIATUS("متوقف")
}
