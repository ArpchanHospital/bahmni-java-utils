package org.bahmni.openmrsconnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.bahmni.openmrsconnector.response.AuthenticationResponse;
import org.bahmni.openmrsconnector.response.PersonAttributeType;
import org.bahmni.openmrsconnector.response.PersonAttributeTypes;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenMRSRestService {
    private RestTemplate restTemplate = new RestTemplate();
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final Log log = LogFactory.getLog(OpenMRSRestService.class);
    private String sessionId;
    private static Logger logger = Logger.getLogger(OpenMRSRestService.class);
    private AllPatientAttributeTypes allPatientAttributeTypes;
    private OpenMRSRESTConnection openMRSRESTConnection;

    public OpenMRSRestService(OpenMRSRESTConnection openMRSRESTConnection) throws IOException, URISyntaxException {
        this.openMRSRESTConnection = openMRSRESTConnection;
        authenticate();
        loadReferences();
    }

    public void authenticate() throws URISyntaxException, IOException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Basic " + openMRSRESTConnection.encodedLogin());
        HttpEntity requestEntity = new HttpEntity<MultiValueMap>(new LinkedMultiValueMap<String, String>(), requestHeaders);
        String authURL = openMRSRESTConnection.getRestApiUrl() + "session";
        ResponseEntity<String> exchange = restTemplate.exchange(new URI(authURL), HttpMethod.GET, requestEntity, String.class);
        logger.info(exchange.getBody());
        AuthenticationResponse authenticationResponse = objectMapper.readValue(exchange.getBody(), AuthenticationResponse.class);
        sessionId = authenticationResponse.getSessionId();
    }

    private void loadReferences() throws URISyntaxException, IOException {
        allPatientAttributeTypes = new AllPatientAttributeTypes();
        String jsonResponse = executeHTTPMethod("personattributetype?v=full", HttpMethod.GET);
        PersonAttributeTypes personAttributeTypes = objectMapper.readValue(jsonResponse, PersonAttributeTypes.class);
        for (PersonAttributeType personAttributeType : personAttributeTypes.getResults())
            allPatientAttributeTypes.addPersonAttributeType(personAttributeType.getName(), personAttributeType.getUuid());
    }

    public AllPatientAttributeTypes getAllPatientAttributeTypes() {
        return allPatientAttributeTypes;
    }

    private String executeHTTPMethod(String urlSuffix, HttpMethod method) throws URISyntaxException {
        HttpHeaders requestHeaders = getHttpHeaders();
        String referencesURL = openMRSRESTConnection.getRestApiUrl() + urlSuffix;
        HttpEntity requestEntity = new HttpEntity<MultiValueMap>(new LinkedMultiValueMap<String, String>(), requestHeaders);
        ResponseEntity<String> exchange = restTemplate.exchange(new URI(referencesURL), method, requestEntity, String.class);
        logger.debug("(" + urlSuffix + ") - " + exchange.getBody());
        return exchange.getBody();
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Cookie", "JSESSIONID=" + sessionId);
        return requestHeaders;
    }

    public String getSessionId() {
        return sessionId;
    }
}