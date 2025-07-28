GEOSERVER_EXTENSION_PROFILES=wps-download,app-schema,control-flow,csw,libjpeg-turbo,monitor,pyramid,wps,css,jp2k,authkey,mapstore2,mbstyle,web-resource,sldservice,geopkg-output,wfs-freemarker,ogcapi

docker-pull-jetty:
	echo "docker pull jetty:9-jre11"

docker-build: docker-pull-jetty
	LANG=C mvn clean install -DskipTests -Dfmt.skip=true -P${GEOSERVER_EXTENSION_PROFILES}; \
	cd webapp; \
	mvn clean install docker:build -DdockerImageTags=${BTAG} -Pdocker,${GEOSERVER_EXTENSION_PROFILES} -DskipTests

docker-build-geofence: docker-pull-jetty
	LANG=C mvn clean install -DskipTests -Dfmt.skip=true -Pgeofence-server,${GEOSERVER_EXTENSION_PROFILES} ; \
    cd webapp; \
	mvn clean install docker:build -DdockerImageTags=${BTAG} -Pdocker,geofence,${GEOSERVER_EXTENSION_PROFILES} -DskipTests

war-build:
	LANG=C mvn clean install -DskipTests -Dfmt.skip=true -P${GEOSERVER_EXTENSION_PROFILES}; \
	mvn clean install -pl webapp -P${GEOSERVER_EXTENSION_PROFILES}

war-geofence:
	LANG=C mvn clean install -DskipTests -Dfmt.skip=true -P${GEOSERVER_EXTENSION_PROFILES},geofence; \
	mvn clean install -pl webapp -P${GEOSERVER_EXTENSION_PROFILES}

deb: war-build
	mvn clean package deb:package -pl webapp -PdebianPackage,${GEOSERVER_EXTENSION_PROFILES} ${DEPLOY_OPTS}

deb-geofence: war-geofence
	mvn clean package deb:package -pl webapp -PdebianPackage,geofence,${GEOSERVER_EXTENSION_PROFILES} ${DEPLOY_OPTS}