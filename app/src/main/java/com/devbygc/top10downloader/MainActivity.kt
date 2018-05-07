package com.devbygc.top10downloader

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import kotlin.properties.Delegates

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""

    override fun toString(): String {
        return """
            name = $name
            artist = $artist
            releaseDate = $releaseDate
            imageURL = $imageURL
        """.trimIndent()
    }
}

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private var downloadData: DownloadData? = null

    private var feedURL: String = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    private var feedLimit = 10
    var previousMenuItemId = R.id.menuFree

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadURL(feedURL.format(feedLimit))
        Log.d(TAG, "onCreate: done")
    }

    private fun downloadURL(feedURL: String) {
        Log.d(TAG, "downloadURL starting AsyncTask")
        downloadData = DownloadData(this, xmlListView)
        downloadData?.execute(feedURL)
        Log.d(TAG, "downloadURL AsyncTask done")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)

        if (feedLimit == 10) {
            menu?.findItem(R.id.menu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.menu25)?.isChecked = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == previousMenuItemId) {
            // Do nothing
            Log.d(TAG, "onOptionsItemSelected: ${item.title} == previousMenuItemId. Do not download data")
        } else if (item.itemId == R.id.menuRefresh){
            Log.d(TAG, "onOptionsItemSelected: Refresh data")
            downloadURL(feedURL.format(feedLimit))
        } else {
            Log.d(TAG, "onOptionsItemSelected: ${item.title} Download data")

            when (item.itemId) {
                R.id.menuFree ->
                    feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
                R.id.menuPaid ->
                    feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
                R.id.menuSongs ->
                    feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
                R.id.menu10, R.id.menu25 -> {
                    if (!item.isChecked) {
                        item.isChecked = true
                        feedLimit = 35 - feedLimit
                        Log.d(TAG, "onOptionsItemSelected: ${item.title} setting feedLimit to $feedLimit")
                    } else {
                        Log.d(TAG, "onOptionsItemSelected: ${item.title} feedLimit unchanged")
                    }
                }
                else ->
                    return super.onOptionsItemSelected(item)
            }

            downloadURL(feedURL.format(feedLimit))
            previousMenuItemId = item.itemId
        }

        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)
    }

    companion object {
        //        private class DownloadData : AsyncTask<String, Void, String>() {
        private class DownloadData(context: Context, listView: ListView) : AsyncTask<String, Int, String>() {
            private val TAG = "DownloadData"

            var context: Context by Delegates.notNull()
            var listView: ListView by Delegates.notNull()

            init {
                this.context = context
                this.listView = listView
            }

            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground: starts with ${url[0]}")
                val rssFeed = downloadXML(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackground: Error downloading")
                }
                return rssFeed
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                val parseApplications = ParseApplications()
                parseApplications.parse(result)

                val feedAdapter = FeedAdapter(context, R.layout.list_record, parseApplications.applications)
                listView.adapter = feedAdapter
            }

            private fun downloadXML(urlPath: String?): String {
                return URL(urlPath).readText()
            }

        }
    }


}
