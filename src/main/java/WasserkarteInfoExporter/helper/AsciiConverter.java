package WasserkarteInfoExporter.helper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class AsciiConverter
{
    public static String convertToPlainAscii(final String input)
    {
        String output = input;
        output = output.replaceAll("ä", "ae");
        output = output.replaceAll("Ä", "Ae");
        output = output.replaceAll("ö", "oe");
        output = output.replaceAll("Ö", "Oe");
        output = output.replaceAll("ü", "ue");
        output = output.replaceAll("Ü", "Ue");
        output = output.replaceAll("ß", "ss");

        if (Charset.forName(StandardCharsets.US_ASCII.name()).newEncoder().canEncode(output))
            return output;

        throw new RuntimeException("ERROR: Found non-ascii in: " + input);
    }

}
