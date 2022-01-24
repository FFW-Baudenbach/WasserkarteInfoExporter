package WasserkarteInfoExporter.exporter;

import WasserkarteInfoExporter.helper.AsciiConverter;
import WasserkarteInfoExporter.helper.Hydrant;
import WasserkarteInfoExporter.helper.HydrantType;
import WasserkarteInfoExporter.helper.UrlHelper;
import WasserkarteInfoExporter.pojo.SourceType;
import WasserkarteInfoExporter.pojo.WasserkarteInfoResponse;
import WasserkarteInfoExporter.pojo.WaterSource;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.*;


public abstract class Exporter
{
    private final String apiToken;
    private final double latitude;
    private final double longitude;
    private final String baseUrl = "https://api.wasserkarte.info/2.0/getSurroundingWaterSources/";

    public Exporter(final String token, double lat, double lng) {
        apiToken = token;
        latitude = lat;
        longitude = lng;
    }


    List<Hydrant> getHydrants()
    {
        var response = callWasserkarteInfoApi();
        Map<Long, HydrantType> mappedSourceTypes = new TreeMap<>();
        for (SourceType sourceType : response.getSourceTypes()) {
            switch (sourceType.getName().getDe()) {
                case "Unterflurhydrant" -> mappedSourceTypes.put(sourceType.getId(), HydrantType.UNDERGROUND);
                case "Überflurhydrant" -> mappedSourceTypes.put(sourceType.getId(), HydrantType.PILLAR);
                case "Löschwasserteich" -> mappedSourceTypes.put(sourceType.getId(), HydrantType.PIPE);
                case "Wandhydrant" -> mappedSourceTypes.put(sourceType.getId(), HydrantType.WALL);
                default -> throw new IllegalArgumentException("Got unknown sourceType: " + sourceType.getName().getDe());
            }
        }

        List<Hydrant> parsedHydrants = new ArrayList<>();
        for(WaterSource source : response.getWaterSources()) {
            Hydrant hydrant = new Hydrant();
            hydrant.setId(source.getIdForUser());
            hydrant.setName(source.getName());
            hydrant.setHydrantType(mappedSourceTypes.get(source.getSourceType()));
            hydrant.setLatitude(source.getLatitude());
            hydrant.setLongitude(source.getLongitude());
            hydrant.setDiameter(source.getNominalDiameter());
            parsedHydrants.add(hydrant);

            //Long aTestToCheckIfValidNumber = Long.parseLong(hydrant.getId());
        }
        return parsedHydrants;
    }

    private WasserkarteInfoResponse callWasserkarteInfoApi()
    {
        String url = baseUrl + "?source=alamosam";

        url += UrlHelper.buildProperParameter("token", apiToken);
        url += UrlHelper.buildProperParameter("lat", String.valueOf(latitude));
        url += UrlHelper.buildProperParameter("lng", String.valueOf(longitude));
        url += UrlHelper.buildProperParameter("range", "1000"); //KM
        url += UrlHelper.buildProperParameter("numItems", "1000");

        WasserkarteInfoResponse response;
        try {
            RestTemplate template = new RestTemplateBuilder().build();
            response = template.getForObject(url, WasserkarteInfoResponse.class);
            if (response == null)
                throw new IllegalStateException("Response is null");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }

        return response;
    }
}
