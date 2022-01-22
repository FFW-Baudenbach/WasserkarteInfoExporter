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


public class Exporter
{
    private final String apiToken;
    private final double latitude;
    private final double longitude;
    private final String baseUrl = "https://api.wasserkarte.info/2.0/getSurroundingWaterSources/";

    public Exporter(final String token) {
        apiToken = token;
        latitude = 49.62348703601339;
        longitude = 10.536265950423282;
    }

    public String generateAlamosCsv() {
        return "blub";
    }

    public String generateOfmCsv() {
        var result = getHydrants();

        Map<Long, HydrantType> mappedSourceTypes = new TreeMap<>();
        for (SourceType sourceType : result.getSourceTypes()) {
            switch (sourceType.getName().getDe()) {
                case "Unterflurhydrant":
                    mappedSourceTypes.put(sourceType.getId(), HydrantType.UNDERGROUND);
                    break;
                case "Überflurhydrant":
                    mappedSourceTypes.put(sourceType.getId(), HydrantType.PILLAR);
                    break;
                case "Löschwasserteich":
                    mappedSourceTypes.put(sourceType.getId(), HydrantType.PIPE);
                    break;
                case "Wandhydrant":
                    mappedSourceTypes.put(sourceType.getId(), HydrantType.WALL);
                    break;
                default:
                    throw new IllegalArgumentException("Got unknown sourceType: " + sourceType.getName().getDe());
            }
        }

        List<Hydrant> parsedHydrants = new ArrayList<>();
        for(WaterSource source : result.getWaterSources()) {
            Hydrant hydrant = new Hydrant();
            hydrant.setId(source.getIdForUser());
            hydrant.setName(source.getName());
            hydrant.setHydrantType(mappedSourceTypes.get(source.getSourceType()));
            hydrant.setLatitude(source.getLatitude());
            hydrant.setLongitude(source.getLongitude());
            hydrant.setDiameter(source.getNominalDiameter());
            parsedHydrants.add(hydrant);
        }

        //flag, id, name, type, lat, lng, dia
        String OFM_CSV_HEADER = "emergency;longitude;latitude;fire_hydrant:type;fire_hydrant:pressure;fire_hydrant:diameter;ref;name";

        String csv = OFM_CSV_HEADER + System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append(OFM_CSV_HEADER);
        sb.append(System.lineSeparator());

        for (Hydrant hydrant : parsedHydrants) {
            sb.append(String.join(";",
                    "fire_hydrant",
                    String.valueOf(hydrant.getLongitude()),
                    String.valueOf(hydrant.getLatitude()),
                    mapOfmHydrantType(hydrant.getHydrantType()),
                    getPressure(hydrant.getHydrantType()),
                    String.valueOf(hydrant.getDiameter()),
                    hydrant.getId(),
                    hydrant.getName()));
            sb.append(System.lineSeparator());
        }

        return sb.toString().trim();
    }

    private String getPressure(HydrantType type) {
        if (type == HydrantType.PIPE) {
            return "suction";
        }
        return "yes";
    }

    private String mapOfmHydrantType(HydrantType type) {
        switch (type)
        {
            case PIPE -> {
                return "pipe";
            }
            case PILLAR -> {
                return "pillar";
            }
            case WALL -> {
                return "wall";
            }
            case UNDERGROUND -> {
                return "underground";
            }
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    private WasserkarteInfoResponse getHydrants()
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
