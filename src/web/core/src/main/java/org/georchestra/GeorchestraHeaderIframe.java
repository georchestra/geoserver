package org.georchestra;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.InlineFrame;
import org.apache.wicket.util.tester.DummyHomePage;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

public class GeorchestraHeaderIframe extends InlineFrame {

    private String headerUrl;
    private String headerHeight;

    private static Logger LOGGER = Logging.getLogger(GeorchestraHeaderIframe.class);

    @PostConstruct
    public void init() {
        headerHeight = getGeoServerApplication().getBean("georchestraHeaderHeight").toString();
        headerUrl = getGeoServerApplication().getBean("georchestraHeaderUrl").toString();
    }

    protected GeoServerApplication getGeoServerApplication() {
        return (GeoServerApplication) getApplication();
    }

    public GeorchestraHeaderIframe(String id) {
        super(id, new DummyHomePage());
    }

    @Override
    protected CharSequence getURL() {
        return this.headerUrl;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        tag.put(
                "style",
                "width:100%;height:" + this.headerHeight + "px;border:none;overflow:hidden;");
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
