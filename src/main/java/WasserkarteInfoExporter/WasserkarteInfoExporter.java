package WasserkarteInfoExporter;

import WasserkarteInfoExporter.exporter.AlamosExporter;
import WasserkarteInfoExporter.exporter.Exporter;
import WasserkarteInfoExporter.exporter.IExporter;
import WasserkarteInfoExporter.exporter.OfmExporter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class WasserkarteInfoExporter implements CommandLineRunner
{
    public enum Mode {Alamos, OFM}
    private static String help = "Usage: java -jar WasserkarteInfoExporter.jar <TOKEN> <LAT> <LNG> <MODE(Alamos|OFM)>";

    @Override
    public void run(String... args)
    {
        if (args.length != 4) {
            System.out.println(help);
            System.exit(1);
        }

        String token = args[0];
        if (token == null || token.isEmpty()) {
            System.out.println(help);
            System.exit(1);
        }

        double lat = 0, lng = 0;
        try {
            lat = Double.parseDouble(args[1]);
            lng = Double.parseDouble(args[2]);

            if (lat < lng) {
                throw new IllegalArgumentException("Coordinates seem wrong");
            }
        }
        catch (NumberFormatException e) {
            System.out.println(help);
            System.exit(1);
        }

        Mode mode = null;
        try {
            mode = Mode.valueOf(args[3]);
        }
        catch (IllegalArgumentException e) {
            System.out.println(help);
            System.exit(1);
        }

        String dateTime = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        String csvContent, csvFileName;
        Charset charset;
        IExporter exporter;

        switch (mode) {
            case Alamos -> {
                exporter = new AlamosExporter(token, lat, lng);
                csvFileName = "wasserkarte.info_export_alamos_" + dateTime + ".csv";
                csvContent = exporter.generateCsv();
                charset = StandardCharsets.US_ASCII;
            }
            case OFM -> {
                exporter = new OfmExporter(token, lat, lng);
                csvFileName = "wasserkarte.info_export_ofm_" + dateTime + ".csv";
                csvContent = exporter.generateCsv();
                charset = StandardCharsets.UTF_8;
            }
            default -> throw new IllegalStateException("Unknown mode");
        }

        // Write to file
        try {
            writeFile(csvFileName, charset, csvContent);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        System.out.println("DONE! Check " + csvFileName + " for result");
    }

    private void writeFile(String fileName, Charset charset, String content) throws IOException {
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(fileName), charset))) {
            writer.write(content);
        }
    }

}
