package org.georchestra;

import java.util.logging.Logger;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

public class GeorchestraHeaderWebComponent extends WebComponent {

    private String headerUrl;
    private String headerHeight;
    private String legacyHeader;

    private static Logger LOGGER = Logging.getLogger(GeorchestraHeaderWebComponent.class);

    private void init() {
        headerHeight = getGeoServerApplication().getBean("georchestraHeaderHeight").toString();
        headerUrl = getGeoServerApplication().getBean("georchestraHeaderUrl").toString();
        legacyHeader = getGeoServerApplication().getBean("georchestraLegacyHeader").toString();
    }

    protected GeoServerApplication getGeoServerApplication() {
        return (GeoServerApplication) getApplication();
    }

    public GeorchestraHeaderWebComponent(String id) {
        super(id);
        init();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        tag.put("style", "width:100%;height:" + this.headerHeight + "px;border:none;");
        tag.put("active-app", "geoserver");
        tag.put("legacy-url", this.headerUrl);
        tag.put("legacy-header", this.legacyHeader);
        super.onComponentTag(tag);
    }
}
