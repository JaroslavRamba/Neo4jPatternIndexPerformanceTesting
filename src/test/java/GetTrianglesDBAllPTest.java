import com.graphaware.test.performance.CacheConfiguration;
import com.graphaware.test.performance.CacheParameter;
import com.graphaware.test.performance.Parameter;
import com.graphaware.test.performance.PerformanceTest;
import com.graphaware.test.util.TestUtils;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.*;
import java.util.*;

/**
 * Created by Martin on 05.04.15.
 */
public class GetTrianglesDBAllPTest implements PerformanceTest {

    private TemporaryFolder temporaryFolder;
    private GraphDatabaseService temporaryDatabase;
    SortedSet<String> triangleSet;
    private boolean writePermission = true;
    private List<Map<String, Object>> optResults;


    /**
     * {@inheritDoc}
     */
    @Override
    public String shortName() {
        return "GetTrianglesDBAllPTest";
    }

    @Override
    public String longName() {
        return "Optimalization to get all triangles with external database, " +
                "where all triangles are copied and queried at once.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Parameter> parameters() {
        List<Parameter> result = new LinkedList<>();
        result.add(new CacheParameter("cache")); //no cache, low-level cache, high-level cache
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int dryRuns(Map<String, Object> params) {
        return ((CacheConfiguration) params.get("cache")).needsWarmup() ? 5 : 5;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int measuredRuns() {
        return 10;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> databaseParameters(Map<String, Object> params) {
        return ((CacheConfiguration) params.get("cache")).addToConfig(Collections.<String, String>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareDatabase(GraphDatabaseService database, final Map<String, Object> params) {
        //triangleSet = PerformanceTestHelper.getTriangleSetFromDatabase(database, "");
        triangleSet = PerformanceTestHelper.getTriangleSetFromFile("ptt-all-original.txt", "");
    }

    @Override
    public String getExistingDatabasePath() {
        return "testDb/graph1000-5000.db.zip";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RebuildDatabase rebuildDatabase() {
        return RebuildDatabase.AFTER_PARAM_CHANGE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long run(final GraphDatabaseService database, Map<String, Object> params) {
        long time = 0;
        optResults = new LinkedList<Map<String, Object>>();

        /* Create tmp DB - TODO move this to time block */
        createTemporaryFolder();
        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(temporaryFolder.getRoot().getPath());
        Map<String, String> dbConfig = databaseParameters(params);
        if (dbConfig != null) {
            graphDatabaseBuilder = graphDatabaseBuilder.setConfig(dbConfig);
        }
        temporaryDatabase = graphDatabaseBuilder.newGraphDatabase();

        time += TestUtils.time(new TestUtils.Timed() {
            @Override
            public void time() {
                Iterator triangleSetIterator = triangleSet.iterator();
                while (triangleSetIterator.hasNext()) {
                    String triangle = triangleSetIterator.next().toString();
                    String[] rels = triangle.split("_");

                    Relationship[] sourceRelationships = new Relationship[3];
                    Transaction txDatabase = database.beginTx();
                    Transaction txTemporaryDatabase = temporaryDatabase.beginTx();

                    try {
                        Map<Long, Node> copiedNodes = new HashMap<>();

                        for (int i = 3; i < sourceRelationships.length + 3; i++) {
                            Relationship sourceRelationship = database.getRelationshipById(Long.parseLong(rels[i]));
                            Node sourceStartNode = sourceRelationship.getStartNode();
                            Node sourceEndNode = sourceRelationship.getEndNode();

                            Node targetStartNode;
                            if (!copiedNodes.containsKey(sourceStartNode.getId())) {
                                targetStartNode = temporaryDatabase.createNode();
                                copyProperties(sourceStartNode, targetStartNode);
                                copiedNodes.put(sourceStartNode.getId(), targetStartNode);
                            } else {
                                targetStartNode = copiedNodes.get(sourceStartNode.getId());
                            }

                            Node targetEndNode;
                            if (!copiedNodes.containsKey(sourceEndNode.getId())) {
                                targetEndNode = temporaryDatabase.createNode();
                                copyProperties(sourceEndNode, targetEndNode);
                                copiedNodes.put(sourceEndNode.getId(), targetEndNode);
                            } else {
                                targetEndNode = copiedNodes.get(sourceEndNode.getId());
                            }

                            Relationship targetRelationship = targetStartNode.createRelationshipTo(targetEndNode, sourceRelationship.getType());
                            copyProperties(sourceRelationship, targetRelationship);
                        }

                        txTemporaryDatabase.success();
                        txTemporaryDatabase.close();

                        txDatabase.success();
                        txDatabase.close();
                    } catch (Exception e) {
                        txTemporaryDatabase.failure();
                        txDatabase.failure();
                        e.printStackTrace();
                    }
                }

                Result result = temporaryDatabase.execute("MATCH (a)--(b)--(c)--(a) RETURN id(a), id(b), id(c)");
                PerformanceTestHelper.prepareResults(writePermission, result, optResults);

                writePermission = PerformanceTestHelper.saveResultToFile(writePermission, "ptt-db-all", optResults);
                closeDatabase();
            }
        });

        return time;
    }

    private static void copyProperties(PropertyContainer source, PropertyContainer target) {
        for (String key : source.getPropertyKeys())
            target.setProperty(key, source.getProperty(key));
    }

    private void closeDatabase() {
        if (temporaryDatabase != null) {
            temporaryDatabase.shutdown();
            temporaryFolder.delete();
            temporaryDatabase = null;
        }
    }

    private void createTemporaryFolder() {
        temporaryFolder = new TemporaryFolder();
        try {
            temporaryFolder.create();
            temporaryFolder.getRoot().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean rebuildDatabase(Map<String, Object> params) {
        throw new UnsupportedOperationException("never needed, database rebuilt after every param change");
    }
}
