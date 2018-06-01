package org.georchestra;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.InlineFrame;
import org.apache.wicket.util.tester.DummyHomePage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;


public class GeorchestraHeaderIframe extends InlineFrame{

    private static String headerUrl;
    private static String headerHeight;

    public GeorchestraHeaderIframe(String id){
        super(id, new DummyHomePage());

        if(GeorchestraHeaderIframe.headerUrl == null || GeorchestraHeaderIframe.headerHeight == null) {

            // Set default value
            GeorchestraHeaderIframe.headerUrl = "/header/";
            GeorchestraHeaderIframe.headerHeight = "90";

            // Try to load datadir
            String globalDatadirPath = System.getProperty("georchestra.datadir");

            if (globalDatadirPath != null) {
                File defaultConfiguration = new File(String.format("%s%s%s", globalDatadirPath,
                        File.separator, "default.properties"));
                File geoserverConfiguration = new File(String.format("%s%s%s%s%s", globalDatadirPath,
                        File.separator, "geoserver", File.separator, "geoserver.properties"));
                Properties properties = new Properties();
                if (defaultConfiguration.canRead()) {
                    try {
                        this.loadProperties(defaultConfiguration, properties);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (geoserverConfiguration.canRead()) {
                    try {
                        this.loadProperties(geoserverConfiguration, properties);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (properties.containsKey("headerUrl"))
                    GeorchestraHeaderIframe.headerUrl = properties.getProperty("headerUrl");
                if (properties.containsKey("headerHeight"))
                    GeorchestraHeaderIframe.headerHeight = properties.getProperty("headerHeight");
            }
        }
    }

    @Override
    protected CharSequence getURL() {
        return this.headerUrl;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        // <iframe src="<%= headerUrl %>" style="width:100%;height:<%= headerHeight %>px;border:none;overflow:hidden;" scrolling="no" frameborder="0"></iframe>
        tag.put("style", "width:100%;height:"+this.headerHeight+"px;border:none;overflow:hidden;");
        super.onComponentTag(tag);
    }

    private Properties loadProperties(File path, Properties prop) throws IOException {
        FileInputStream fisProp = null;
        try {
            fisProp = new FileInputStream(path);
            InputStreamReader isrProp = new InputStreamReader(fisProp, "UTF8");
            prop.load(isrProp);
        } finally {
            if (fisProp != null) {
                fisProp.close();
            }
        }
        return prop;
    }

}
