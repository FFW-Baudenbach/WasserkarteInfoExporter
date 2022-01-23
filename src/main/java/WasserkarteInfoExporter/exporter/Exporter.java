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

    public Exporter(final String token, double lat, double lng) {
        apiToken = token;
        latitude = lat;
        longitude = lng;
    }

    public String generateAlamosCsv()
    {
        var parsedHydrants = getHydrants();

        String ALAMOS_CSV_HEADER = "Y-Koordinate;X-Koordinaten;Anzeigetext;Typ;Durchfluss;Kategorie";

        StringBuilder sb = new StringBuilder();
        sb.append(ALAMOS_CSV_HEADER);
        sb.append(System.lineSeparator());

        for (Hydrant hydrant : parsedHydrants) {
            if (!isSupportedForAlamos(hydrant.getHydrantType())) {
                System.out.println("WARNING: Ignoring unsupported Hydrant: " + hydrant);
                continue;
            }

            sb.append(String.join(";",
                    String.valueOf(hydrant.getLongitude()),
                    String.valueOf(hydrant.getLatitude()),
                    AsciiConverter.convertToPlainAscii(hydrant.getName() + " (# " + hydrant.getId() + ")"),
                    mapAlamosHydrantType(hydrant.getHydrantType()),
                    String.valueOf(hydrant.getDiameter()), // We don't have 'Durchfluss', use Diameter instead.
                    getAlamosCategory(hydrant)));
            sb.append(System.lineSeparator());
        }

        return sb.toString().trim();
    }

    public String generateOfmCsv()
    {
        var parsedHydrants = getHydrants();

        String OFM_CSV_HEADER = "emergency;latitude;longitude;fire_hydrant:type;fire_hydrant:pressure;fire_hydrant:diameter;ref";

        StringBuilder sb = new StringBuilder();
        sb.append(OFM_CSV_HEADER);
        sb.append(System.lineSeparator());

        for (Hydrant hydrant : parsedHydrants) {
            sb.append(String.join(";",
                    "fire_hydrant",
                    String.valueOf(hydrant.getLatitude()),
                    String.valueOf(hydrant.getLongitude()),
                    mapOfmHydrantType(hydrant.getHydrantType()),
                    getOfmPressure(hydrant.getHydrantType()),
                    getOfmDiameter(hydrant.getDiameter()),
                    hydrant.getId()));
            sb.append(System.lineSeparator());
        }

        return sb.toString().trim();
    }

    private List<Hydrant> getHydrants()
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

    private String getOfmPressure(final HydrantType type) {
        if (type == HydrantType.PIPE) {
            return "suction";
        }
        return "yes";
    }

    private boolean isSupportedForAlamos(HydrantType type) {
        return type == HydrantType.PILLAR || type == HydrantType.UNDERGROUND;
    }

    private String mapAlamosHydrantType(HydrantType type) {
        switch (type)
        {
            case PIPE, WALL -> {
                throw new IllegalArgumentException("Unsupported type for Alamos: " + type);
            }
            case PILLAR -> {
                return "O";
            }
            case UNDERGROUND -> {
                return "U";
            }
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    private String mapOfmHydrantType(final HydrantType type) {
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

    private String getOfmDiameter(Long diameter) {
        if (diameter > 0) {
            return String.valueOf(diameter);
        }
        return "";
    }

    private String getAlamosCategory(Hydrant hydrant) {
        Long diameter = hydrant.getDiameter();
        if (diameter > 100) {
            return "96"; //green
        }
        if (diameter > 80) {
            return "48"; //yellow
        }
        if (diameter > 0) {
            return "24"; //red
        }
        System.out.println("WARNING: Undefined Diameter: " + hydrant);
        return "96";
    }
}
