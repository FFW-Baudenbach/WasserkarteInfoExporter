package WasserkarteInfoExporter.exporter;

import WasserkarteInfoExporter.helper.Hydrant;
import WasserkarteInfoExporter.helper.HydrantType;

public class OfmExporter extends Exporter implements IExporter
{
    public OfmExporter(String token, double lat, double lng) {
        super(token, lat, lng);
    }

    @Override
    public String generateCsv() {
        var parsedHydrants = getHydrants();

        String OFM_CSV_HEADER = "emergency;latitude;longitude;fire_hydrant:type;fire_hydrant:pressure;fire_hydrant:diameter;ref;name";

        StringBuilder sb = new StringBuilder();
        sb.append(OFM_CSV_HEADER);
        sb.append(System.lineSeparator());

        for (Hydrant hydrant : parsedHydrants) {

            Long id;
            try {
                Long.parseLong(hydrant.getId());
            }
            catch (NumberFormatException e) {
                System.out.println("WARN: Ignoring hydrant due to invalid id: " + hydrant);
            }

            sb.append(String.join(";",
                    "fire_hydrant",
                    String.valueOf(hydrant.getLatitude()),
                    String.valueOf(hydrant.getLongitude()),
                    mapOfmHydrantType(hydrant.getHydrantType()),
                    getOfmPressure(hydrant.getHydrantType()),
                    getOfmDiameter(hydrant),
                    String.valueOf(hydrant.getId()),
                    hydrant.getName().trim()));
            sb.append(System.lineSeparator());
        }

        return sb.toString().trim();
    }

    private String getOfmPressure(final HydrantType type) {
        if (type == HydrantType.PIPE) {
            return "suction";
        }
        return "yes";
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

    private String getOfmDiameter(final Hydrant hydrant) {
        if (hydrant.getDiameter() > 0) {
            return String.valueOf(hydrant.getDiameter());
        }
        System.out.println("WARN: Empty diameter for Hydrant " + hydrant);
        return "";
    }
}
