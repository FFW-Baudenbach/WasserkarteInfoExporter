package WasserkarteInfoExporter.exporter;

import WasserkarteInfoExporter.helper.AsciiConverter;
import WasserkarteInfoExporter.helper.Hydrant;
import WasserkarteInfoExporter.helper.HydrantType;

public class AlamosExporter extends Exporter implements IExporter
{
    public AlamosExporter(String token, double lat, double lng) {
        super(token, lat, lng);
    }

    @Override
    public String generateCsv() {
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

    private boolean isSupportedForAlamos(HydrantType type) {
        return type == HydrantType.PILLAR || type == HydrantType.UNDERGROUND;
    }

    private String mapAlamosHydrantType(HydrantType type) {
        switch (type)
        {
            case PIPE, WALL -> throw new IllegalArgumentException("Unsupported type for Alamos: " + type);
            case PILLAR -> {
                return "O";
            }
            case UNDERGROUND -> {
                return "U";
            }
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        }
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
