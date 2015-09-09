package com.mickstarify.adbwifi;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

public class About extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        WebView aboutView = (WebView) findViewById(R.id.webView_about);
        String htmlCode = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>Untitled Document.md</title><style></style></head><body id=\"preview\">\n" +
                "<h1><a id=\"Developed_By_Michael_Johnston_0\"></a>Developed By Michael Johnston</h1>\n" +
                "<p><em><a href=\"mailto:michael.johnston29@gmail.com\">michael.johnston29@gmail.com</a></em></p>\n" +
                "<p>Adb Wifi is a simple tool for enabling ADB over TCP connections such as wifi,<br>\n" +
                "It opens the port 5555 for the adb service, so that you can debug your phone over wifi.<br>\n" +
                "To use it, turn on adb wifi and run the command</p>\n" +
                "<blockquote>\n" +
                "<p>$ adb connect X.X.X.X</p>\n" +
                "</blockquote>\n" +
                "</blockquote>\n" +
                "<p>This application is released free under <a href=\"https://www.gnu.org/licenses/gpl-3.0.en.html\">GPLv3</a><br>\n" +
                "Find the source code here <a href=\"http://bitbucket.com/m_j/adbwifi\">bitbucket.com/m_j/adbwifi</a></p>\n" +
                "<h3><a id=\"It_uses_the_following_libraries_10\"></a>It uses the following libraries</h3>\n" +
                "<ul>\n" +
                "<li><a href=\"https://github.com/Chainfire/libsuperuser\">eu.chainfire:libsuperuser</a></li>\n" +
                "</ul>\n" +
                "<p>This application also uses icons from googles material design icon library.</p>\n" +
                "\n" +
                "</body></html>";

        aboutView.loadData(htmlCode,"text/html", null);

        Button closeButton = (Button) findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
