import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Generates a PlantUML package diagram (classes nested inside their packages)
 * from a Java source tree.
 *
 * Run from the project root:   java GenPuml.java
 * Optional args:               java GenPuml.java <sourceRoot> <outputFile>
 * Defaults:                    src/main/java  ->  diagram.puml
 */
public class GenPuml {
    record Type(String fqn, String name, String kind, String header) {}

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : "src/main/java");
        Path out = Path.of(args.length > 1 ? args[1] : "diagram.puml");

        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }

        Pattern pkgPattern = Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;");
        List<Type> types = new ArrayList<>();
        Map<String, String> fqnBySimpleName = new HashMap<>();

        for (Path f : files) {
            String src = Files.readString(f)
                    .replaceAll("(?s)/\\*.*?\\*/", "")   // block comments
                    .replaceAll("//[^\\n]*", "")          // line comments
                    .replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\""); // string contents
            String name = f.getFileName().toString().replace(".java", "");
            Matcher pm = pkgPattern.matcher(src);
            String pkg = pm.find() ? pm.group(1) : "";

            Matcher dm = Pattern.compile("\\b(class|interface|enum|record)\\s+" + name + "\\b([^{]*)\\{").matcher(src);
            if (!dm.find()) continue;

            String kind = dm.group(1);
            String before = src.substring(Math.max(0, dm.start() - 60), dm.start());
            if (kind.equals("class") && before.matches("(?s).*\\babstract\\b[\\s\\w-]*$")) kind = "abstract class";
            if (kind.equals("record")) kind = "class";

            String fqn = pkg.isEmpty() ? name : pkg + "." + name;
            types.add(new Type(fqn, name, kind, dm.group(2)));
            fqnBySimpleName.put(name, fqn);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam packageStyle folder\n");
        sb.append("hide members\n\n");

        for (Type t : types) {
            sb.append(t.kind()).append(' ').append(t.fqn()).append('\n');
        }
        sb.append('\n');

        Set<String> interfaces = types.stream().filter(t -> t.kind().equals("interface"))
                .map(Type::fqn).collect(Collectors.toSet());

        for (Type t : types) {
            String h = t.header();
            while (h.contains("<")) {
                String next = h.replaceAll("<[^<>]*>", "");
                if (next.equals(h)) break;
                h = next;
            }
            h = h.replaceAll("\\([^)]*\\)", " ")      // record components
                    .replaceAll("\\bpermits\\b.*", "")   // sealed permits list
                    .replaceAll("\\s+", " ");

            Matcher em = Pattern.compile("\\bextends\\s+(.*?)(?=\\bimplements\\b|$)").matcher(h);
            Matcher im = Pattern.compile("\\bimplements\\s+(.*)$").matcher(h);

            if (em.find()) {
                for (String parent : em.group(1).split(",")) {
                    String p = resolve(parent, fqnBySimpleName);
                    if (p == null) continue;
                    // interface extends interface -> solid; class extends class -> solid
                    sb.append(p).append(" <|-- ").append(t.fqn()).append('\n');
                }
            }
            if (im.find()) {
                for (String parent : im.group(1).split(",")) {
                    String p = resolve(parent, fqnBySimpleName);
                    if (p == null) continue;
                    sb.append(p).append(interfaces.contains(p) ? " <|.. " : " <|-- ").append(t.fqn()).append('\n');
                }
            }
        }

        sb.append("@enduml\n");
        Files.writeString(out, sb.toString());
        System.out.println("Wrote " + out.toAbsolutePath() + " (" + types.size() + " types)");
    }

    private static String resolve(String raw, Map<String, String> fqnBySimpleName) {
        String s = raw.trim();
        if (s.isEmpty()) return null;
        String simple = s.substring(s.lastIndexOf('.') + 1).trim();
        return fqnBySimpleName.get(simple); // only types from this project
    }
}