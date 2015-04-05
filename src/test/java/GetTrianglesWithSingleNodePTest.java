import com.graphaware.test.performance.CacheConfiguration;
import com.graphaware.test.performance.CacheParameter;
import com.graphaware.test.performance.Parameter;
import com.graphaware.test.performance.PerformanceTest;
import com.graphaware.test.util.TestUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by Jaroslav on 3/25/15.
 */
public class GetTrianglesWithSingleNodePTest implements PerformanceTest {

    private SortedSet<String> triangleSet;
    private List<Map<String, Object>> optResults;
    private boolean writePermission = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public String shortName() {
        return "triangle count";
    }

    @Override
    public String longName() {
        return "Cypher query for get count of triangles";
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
        return ((CacheConfiguration) params.get("cache")).needsWarmup() ? 50 : 5;
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
        triangleSet = PerformanceTestHelper.getTriangleSetFromDatabase(database, "only-nodes");
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

        time += TestUtils.time(new TestUtils.Timed() {
            @Override
            public void time() {
                // Uncomment to optimize
                //Set<String> usedNodes = new HashSet<String>();
                for (String nodeId : triangleSet) {
                    nodeId = nodeId.split("_")[0];
                    // Uncomment to optimize
                    //if (!usedNodes.contains(nodeId)) {
                    Result result = database.execute(
                            "MATCH (a)--(b)--(c)--(a) " +
                                    "WHERE id(a)=" + nodeId + " " +
                                    "RETURN id(a), id(b), id(c) " +
                                    "UNION " +
                                    "MATCH (a)--(b)--(c)--(a) " +
                                    "WHERE id(b)=" + nodeId + " " +
                                    "RETURN id(a), id(b), id(c) " +
                                    "UNION " +
                                    "MATCH (a)--(b)--(c)--(a) " +
                                    "WHERE id(c)=" + nodeId + " " +
                                    "RETURN id(a), id(b), id(c)");
                    // Uncomment to optimize
                    //usedNodes.add(nodeId);
                    PerformanceTestHelper.prepareResults(writePermission, result, optResults);

                    // Uncomment to optimize
                    //}
                }
            }
        });

        writePermission = PerformanceTestHelper.saveResultToFile(writePermission, "ptt-single-node", optResults);

        return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean rebuildDatabase(Map<String, Object> params) {
        throw new UnsupportedOperationException("never needed, database rebuilt after every param change");
    }
}
