package eu.kanade.tachiyomi.extension.all.striputopija

import android.util.Log
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.lang.Exception
import kotlin.collections.ArrayList

class StripUtopija : HttpSource() {

    override val name = "StripUtopija"

    override val baseUrl = "https://striputopija.blogspot.com"

    override val lang = "en"

    override val supportsLatest = true

    private fun defaultRequestBuilder(url: String): Request.Builder {
        val rb = Request.Builder()
        rb.url(url)
        rb.header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
        rb.header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
        return rb
    }

    private fun defaultRequest(url: String): Request {
        return defaultRequestBuilder(url).build()
    }

    private fun parseMangaList(response: Response): List<SManga> {
        Log.v("StripUtopija", "parseMangaList")
        val mangas = ArrayList<SManga>()
        if (response.body == null) {
            Log.v("StripUtopija", "parseMangaList empty")
            return mangas
        }
        val doc = Jsoup.parse(response.body!!.string())
        val sidebar = doc.getElementById("sidebar-left-1")
        val wcs = sidebar.getElementsByAttributeValue("class", "widget PageList")
        for (wc in wcs) {
            for (w in wc.select("a[href]")) {
                val manga = SManga.create()
                manga.title = w.text()
                manga.url = w.attr("href")
                manga.status = SManga.COMPLETED
                mangas.add(manga)
            }
        }
        try {
            mangas.removeAt(0)
        } catch (e: Exception) {
            Log.e("StripUtopija", e.stackTraceToString())
        }
        return mangas.sortedBy { manga -> manga.title }
    }

    private fun parseChapterList(response: Response): List<SChapter> {
        Log.v("StripUtopija", "parseChapterList")
        val chapters = ArrayList<SChapter>()
        if (response.body == null) {
            Log.v("StripUtopija", "parseChapterList empty")
            return chapters
        }
        val doc = Jsoup.parse(response.body!!.string())

        val mainDiv = doc.getElementsByAttributeValue("class", "post-body entry-content")
        for (link in mainDiv.select("a[href]")) {
            try {
                val chapter = SChapter.create()
                chapter.date_upload = 0
                chapter.chapter_number = link.text().substring(0, 4).toFloat()
                chapter.name = link.text()
                chapter.url = link.attr("href")
                chapters.add(chapter)
            } catch (e: Exception) {
                Log.e("StripUtopija", e.stackTraceToString())
            }
        }
        return chapters.reversed()
    }

    private fun parseChapterPages(response: Response): List<Page> {
        Log.v("StripUtopija", "parseChapterPages")
        val pages = ArrayList<Page>()
        if (response.body == null) {
            Log.v("StripUtopija", "parseChapterPages empty")
            return pages
        }
        val doc = Jsoup.parse(response.body!!.string())

        val mainDiv = doc.getElementsByAttributeValue("class", "post-body entry-content")
        for (link in mainDiv.select("img[src]")) {
            val page = Page(
                pages.size,
                "",
                link.attr("src")
            )
            if (page.imageUrl!!.startsWith("//")) {
                page.imageUrl = "https:" + page.imageUrl!!
            }
            pages.add(page)
        }
        return pages
    }

    private fun parseMangaDetails(response: Response): SManga {
        Log.v("StripUtopija", "parseMangaDetails")
        val manga = SManga.create()
        if (response.body == null) {
            Log.v("StripUtopija", "parseMangaDetails empty")
            return manga
        }
        val doc = Jsoup.parse(response.body!!.string())
        val mainDiv = doc.getElementsByAttributeValue("class", "post-body entry-content")
        for (el in mainDiv) {
            try {
                val bd = el.text()
                val descEnd = bd.indexOf("REDOVNA IZDANJA:")
                if (descEnd == -1) {
                    break
                }
                manga.description = bd.substring(0, descEnd)
                break
            } catch (e: Exception) {
                Log.e("StripUtopija", e.stackTraceToString())
            }
        }
        for (link in mainDiv.select("img[src]")) {
            manga.thumbnail_url = link.attr("src")
            break
        }
        return manga
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        Log.v("StripUtopija", "chapterListParse")
        return parseChapterList(response)
    }

    override fun imageUrlParse(response: Response): String {
        Log.v("StripUtopija", "imageUrlParse")
        return ""
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        Log.v("StripUtopija", "latestUpdatesParse")
        return MangasPage(parseMangaList(response), false)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val r = defaultRequest(baseUrl)
        Log.v("StripUtopija", "latestUpdatesRequest $r")
        return r
    }

    override fun mangaDetailsParse(response: Response): SManga {
        Log.v("StripUtopija", "mangaDetailsParse")
        return parseMangaDetails(response)
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        val r = defaultRequest(manga.url)
        Log.v("StripUtopija", "mangaDetailsRequest $r")
        return r
    }

    override fun pageListParse(response: Response): List<Page> {
        Log.v("StripUtopija", "pageListParse")
        return parseChapterPages(response)
    }
    override fun pageListRequest(chapter: SChapter): Request {
        val r = defaultRequest(chapter.url)
        Log.v("StripUtopija", "pageListRequest $r")
        return r
    }

    override fun popularMangaParse(response: Response): MangasPage {
        Log.v("StripUtopija", "popularMangaParse")
        return MangasPage(parseMangaList(response), false)
    }

    override fun popularMangaRequest(page: Int): Request {
        val r = defaultRequest(baseUrl)
        Log.v("StripUtopija", "popularMangaRequest $r")
        return r
    }

    override fun searchMangaParse(response: Response): MangasPage {
        Log.v("StripUtopija", "searchMangaParse")
        val mangas = parseMangaList(response)
        val req = response.request
        val title = req.headers["title-name"]
        return MangasPage(mangas.filter { manga -> manga.title.contains(title!!, true) }, false)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val r = defaultRequestBuilder(baseUrl).header("title-name", query).build()
        Log.v("StripUtopija", "searchMangaRequest $r")
        return r
    }

    override fun chapterListRequest(manga: SManga): Request {
        val r = defaultRequest(manga.url)
        Log.v("StripUtopija", "chapterListRequest $r")
        return r
    }
}
