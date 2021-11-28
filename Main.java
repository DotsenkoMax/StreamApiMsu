
import com.google.common.base.Stopwatch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Answer {
    Double delta;
    Double percentIncrease;
    Double valueSum;

    public Answer(Double firstPrice, Double lastPrice, Double valueSum) {
        this.delta = lastPrice - firstPrice;
        this.percentIncrease = lastPrice / firstPrice * 100. - 100.;
        this.valueSum = valueSum;
    }

    @Override
    public String toString() {
        return "Answer{" +
                "delta=" + delta +
                ", percentIncrease=" + percentIncrease +
                ", valueSum=" + valueSum +
                '}';
    }
}

class Main {
    public static final int TRADETIME = 1;
    public static final int SECBOARD = 2;
    public static final int SECCODE = 3;
    public static final int PRICE = 4;
    public static final int VALUE = 8;

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static void unzip(String fileZip, String path) throws IOException {
        File destDir = new File(path);
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void main(String[] args) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        unzip("trades.zip", "trades/");
        System.out.printf("Unzipping time: %s\n", stopwatch.reset());

        stopwatch.start();
        try (Stream<String> stream = Files.lines(Paths.get("./trades/trades.txt"))) {
            var res = stream
                    .skip(1)
                    .map(row -> row.split("\t"))
                    .filter(row -> ("TQBR".equals(row[SECBOARD]) || "FQBR".equals(row[SECBOARD])))
                    .collect(Collectors.groupingBy(row -> row[SECCODE]))
                    .values()
                    .stream()
                    .filter(list -> !list.isEmpty())
                    .map(strings -> {
                                var maxResult = strings.stream().max(Comparator.comparing(lhs -> lhs[TRADETIME])).get();
                                var minResult = strings.stream().min(Comparator.comparing(lhs -> lhs[TRADETIME])).get();
                                var dealSum = strings
                                        .stream()
                                        .collect(Collectors.summarizingDouble((row) -> Double.parseDouble(row[VALUE])))
                                        .getSum();
                                return new Answer(Double.parseDouble(minResult[PRICE]), Double.parseDouble(maxResult[PRICE]), dealSum);
                            }
                    ).sorted(Comparator.comparing(it -> it.delta));
            int leftLimit = 10, rightLimit = 10;
            int curr = 0;
            List<Answer> result = res.collect(Collectors.toList());
            System.out.println("---------------BAD CASES--------------");
            for (var each : result) {
                if (curr < leftLimit) {
                    System.out.println(each);
                }
                if (curr == leftLimit) System.out.println("---------------TOP CASES--------------");
                if (curr > result.size() - rightLimit - 1) {
                    System.out.println(each);
                }
                ++curr;
            }
        }
        System.out.printf("Program time: %s", stopwatch.stop());
    }
}