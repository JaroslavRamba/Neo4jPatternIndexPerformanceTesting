import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;

import java.io.*;
import java.util.*;

/**
 * Created by Martin on 02.04.15.
 */
public class PerformanceTestHelper {

    /**
     * Method takes 3 nodes, or relationships, sorts them by their id value and returns concatenated unique key.
     *
     * @param itemA
     * @param itemB
     * @param itemC
     * @return unique key from concatenated node or relationship ids.
     */
    private static String getKeyToTriangleSet(Object itemA, Object itemB, Object itemC) {
        Long[] items = new Long[3];
        items[0] = Long.parseLong(itemA.toString());
        items[1] = Long.parseLong(itemB.toString());
        items[2] = Long.parseLong(itemC.toString());
        Arrays.sort(items);
        return items[0] + "_" + items[1] + "_" + items[2];
    }

    private static String getKeyToTriangleSet(Object[] items) {
        if (items.length != 6) {
            return null;
        }
        String nodes = getKeyToTriangleSet(items[0], items[1], items[2]);
        String rels = getKeyToTriangleSet(items[3], items[4], items[5]);
        return nodes + "_" + rels;
    }

    /**
     * Method loads triangleSet from file represented with its absolute path.
     *
     * @param pathToFile absolute path to file.
     * @return unique triangleSet with sorted-concatenated keys, that represent individual triangles.
     */
    // TODO review
    public static SortedSet<String> getTriangleSetFromFile(String pathToFile, String type) {
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(pathToFile))) {
                String line;
                try {
                    SortedSet<String> triangleSet = new TreeSet<String>();

                    br.readLine();
                    br.readLine();

                    while ((line = br.readLine()) != null) {
                        String[] parts = line.substring(1, line.length() - 1).split("\\|");
                        if (type.equals("only-nodes")) {
                            triangleSet.add(getKeyToTriangleSet(parts[0], parts[1], parts[2]));
                        } else if (type.equals("")) {
                            triangleSet.add(getKeyToTriangleSet(parts));
                        } else {
                            return null;
                        }
                    }
                    if (type.equals("only-nodes")) {
                        saveTriangleSetResultToFile("ptt-only-nodes-original-automorphism-reduced.txt", triangleSet);
                    } else if (type.equals("")) {
                        saveTriangleSetResultToFile("ptt-all-original-automorphism-reduced.txt", triangleSet);
                    }
                    return triangleSet;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method loads triangleSet from file represented with its absolute path.
     *
     * @param database current working database.
     * @return unique triangleSet with sorted-concatenated keys, that represent individual triangles.
     */
    // TODO review
    public static SortedSet<String> getTriangleSetFromDatabase(GraphDatabaseService database, String type) {
        try {
            Result result;
            if (type.equals("only-nodes")) {
                result = database.execute("MATCH (a)--(b)--(c)--(a) RETURN id(a), id(b), id(c)");
            } else if (type.equals("")) {
                result = database.execute("MATCH (a)-[r]-(b)-[p]-(c)-[q]-(a) RETURN id(a), id(b), id(c), id(r), id(p), id(q)");
            } else {
                return null;
            }

            List<Map<String, Object>> resultToPrint = new ArrayList<Map<String, Object>>();

            SortedMap<String, Object> row;
            SortedSet<String> triangleSet = new TreeSet<String>();

            while (result.hasNext()) {
                row = new TreeMap<String, Object>(result.next());
                if (type.equals("only-nodes")) {
                    triangleSet.add(getKeyToTriangleSet(row.get("id(a)"), row.get("id(b)"), row.get("id(c)")));
                } else if (type.equals("")) {
                    triangleSet.add(getKeyToTriangleSet(new Object[]{
                            row.get("id(a)"), row.get("id(b)"), row.get("id(c)"),
                            row.get("id(r)"), row.get("id(p)"), row.get("id(q)")
                    }));
                } else {
                    return null;
                }
                resultToPrint.add(row);
            }

            if (type.equals("only-nodes")) {
                saveTriangleResultToFile("ptt-only-nodes-original.txt", resultToPrint);
                saveTriangleSetResultToFile("ptt-only-nodes-original-automorphism-reduced.txt", triangleSet);
            } else if (type.equals("")) {
                saveTriangleResultToFile("ptt-all-original.txt", resultToPrint);
                saveTriangleSetResultToFile("ptt-all-original-automorphism-reduced.txt", triangleSet);
            }

            return triangleSet;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to store data from Result instance into file. Data in this instance are received within Cypher query.
     *
     * @param fileName filename of the file, where data should be saved.
     * @param result   data to be stored.
     * @throws FileNotFoundException
     */
    public static void saveTriangleResultToFile(String fileName, List<Map<String, Object>> result)
            throws FileNotFoundException {
        PrintStream out = new PrintStream(new FileOutputStream(fileName));

        out.println("Total of " + result.size() + " triangles.");
        out.print("|");
        for (Map.Entry<String, Object> columnHead : result.get(0).entrySet()) {
            out.print(columnHead.getKey() + "|");
        }
        out.println();


        for (Map<String, Object> row : result) {
            out.print("|");
            for (Map.Entry<String, Object> column : row.entrySet()) {
                out.print(column.getValue() + "|");
            }
            out.println();
        }
        out.close();
    }

    /**
     * Method to store data from triangleSet variable. Data in this variable are received within Cypher query where DISTINCT
     * filter is applied. It means it only stores unique data.
     *
     * @param fileName    filename of the file, where data should be saved.
     * @param triangleSet data to be stored.
     * @throws FileNotFoundException
     */
    public static void saveTriangleSetResultToFile(String fileName, SortedSet<String> triangleSet)
            throws FileNotFoundException {
        PrintStream out = new PrintStream(new FileOutputStream(fileName));

        out.println("Total of " + triangleSet.size() + " triangles.");
        for (String triangle : triangleSet) {
            String[] triangleArray = triangle.split("_");
            out.print("|");
            for (String node : triangleArray) {
                out.print(node + "|");
            }
            out.println();
        }
        out.close();
    }

    /**
     * Method to store Result instance data into different structure (List<Map<String, Object>>).
     *
     * @param writePermission
     * @param result          result instance to be converted.
     * @param results         container to store converted data.
     */
    public static void prepareResults(boolean writePermission, Result result, List<Map<String, Object>> results) {
        if (writePermission) {
            while (result.hasNext()) {
                results.add(result.next());
            }
        }
    }

    /**
     * Method to apply DISTINCT filter on data from Cypher query.
     *
     * @param result data received within Cypher query.
     * @return unique records from Cypher query.
     */
    public static SortedSet<String> triangleResultToTriangleSet(List<Map<String, Object>> result) {
        SortedSet<String> triangleSet = new TreeSet<>();
        Object[] items = new Object[3];
        int i;

        for (Map<String, Object> row : result) {
            i = 0;
            if (row.size() == 3) {
                for (Map.Entry<String, Object> column : row.entrySet()) {
                    items[i++] = column.getValue();
                }
                triangleSet.add(getKeyToTriangleSet(items[0], items[1], items[2]));
            } else {
                return null;
            }
        }
        return triangleSet;
    }


    /**
     * Method to save cypher result to file
     *
     * @param writePermission true for store data, false to not store data
     * @param optResults      data to be stored.
     */
    public static boolean saveResultToFile(boolean writePermission, String filenameResult, List<Map<String, Object>> optResults) {
        // ptt = Performance test triangle, opt = optimalized
        if (writePermission) {
            System.out.println("Saving results to file...");
            try {
                PerformanceTestHelper.saveTriangleResultToFile(filenameResult + ".txt", optResults);

                PerformanceTestHelper.saveTriangleSetResultToFile(filenameResult + "-automorphism-reduced.txt",
                        PerformanceTestHelper.triangleResultToTriangleSet(optResults));

                return false;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return true;
            }
        }

        return false;
    }

}