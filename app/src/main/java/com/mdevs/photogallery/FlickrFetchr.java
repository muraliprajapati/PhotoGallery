package com.mdevs.photogallery;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Murali on 22-05-2015.
 */
public class FlickrFetchr {
    public static final String TAG = "FlickrFetchr";
    public static final String PREF_SEARCH_QUERY = "searchQuery";
    public static final String PREF_LAST_RESULT_ID = "lastResultId";
    private static final String ENDPOINT = "http://api.flickr.com/services/rest/";
    private static final String API_KEY = "67de34a941bcce7d1132d9c250b5843b";//22102b8ce9c11f4592c321e643d2a0bb
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String PARAM_EXTRAS = "extras";
    private static final String EXTRA_SMALL_URL = "url_s";
    private static final String XML_PHOTO = "photo";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String PARAM_TEXT = "text";

    byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public ArrayList<GalleryItem> downloadGalleryItems(String url) throws XmlPullParserException {
        ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
        try {
            /*String url = Uri.parse(ENDPOINT).buildUpon()
                    .appendQueryParameter("method", METHOD_GET_RECENT)
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                    .build().toString();*/
            //Complete URL? http://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=67de34a941bcce7d1132d9c250b5843b&extras=url_s

            String xmlString = getUrl(url);
            Log.i("Complete URL", url);
            Log.i(TAG, "Received xml: " + xmlString);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(xmlString));
            parseItems(items, parser);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (XmlPullParserException xmlE) {
            Log.e(TAG, "Failed to parse XML", xmlE);
        }
        return items;
    }

    public ArrayList<GalleryItem> fetchItems() throws XmlPullParserException {
        String url = "https://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=22102b8ce9c11f4592c321e643d2a0bb&extras=url_s";
        return downloadGalleryItems(url);

    }

    public ArrayList<GalleryItem> search(String query) throws XmlPullParserException {
        String url = "https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=22102b8ce9c11f4592c321e643d2a0bb&extras=url_s&text=" + query;
        /*String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method", METHOD_SEARCH)
                .appendQueryParameter("api_key", API_KEY)
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                .appendQueryParameter(PARAM_TEXT, query)
                .build().toString();*/
        return downloadGalleryItems(url);

    }

    private void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && XML_PHOTO.equals(parser.getName())) {
                String id = parser.getAttributeValue(null, "id");
                String caption = parser.getAttributeValue(null, "title");
                String smallURL = parser.getAttributeValue(null, EXTRA_SMALL_URL);
                GalleryItem item = new GalleryItem();
                item.setmId(id);
                item.setmCaption(caption);
                item.setmUrl(smallURL);
                items.add(item);
            }
            eventType = parser.next();
        }
    }
}
